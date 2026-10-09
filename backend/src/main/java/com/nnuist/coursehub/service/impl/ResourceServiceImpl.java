package com.nnuist.coursehub.service.impl;

import com.nnuist.coursehub.common.exception.BizException;
import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.common.result.ResultCode;
import com.nnuist.coursehub.common.util.FileUtil;
import com.nnuist.coursehub.common.util.UserContext;
import com.nnuist.coursehub.dto.ResourceQueryDTO;
import com.nnuist.coursehub.dto.ResourceSaveDTO;
import com.nnuist.coursehub.entity.Course;
import com.nnuist.coursehub.entity.FileInfo;
import com.nnuist.coursehub.entity.Resource;
import com.nnuist.coursehub.entity.ResourceVersion;
import com.nnuist.coursehub.mapper.CourseMapper;
import com.nnuist.coursehub.mapper.FileMapper;
import com.nnuist.coursehub.mapper.ResourceMapper;
import com.nnuist.coursehub.mapper.ResourceVersionMapper;
import com.nnuist.coursehub.service.FileService;
import com.nnuist.coursehub.service.ResourceService;
import com.nnuist.coursehub.vo.FileDownloadResult;
import com.nnuist.coursehub.vo.ResourceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 资料业务实现
 *
 * <p><b>本类最值得看的两处</b>：</p>
 * <ol>
 *   <li>{@link #checkVisible} —— 三级可见性校验（数据权限）</li>
 *   <li>{@link #pageResources} —— 用批量查询规避 N+1 问题</li>
 * </ol>
 *
 * @author CourseHub Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceMapper resourceMapper;
    private final ResourceVersionMapper versionMapper;
    private final CourseMapper courseMapper;
    private final FileMapper fileMapper;
    private final FileService fileService;

    private static final String DEFAULT_TYPE = "OTHER";
    private static final String DEFAULT_VISIBILITY = "PUBLIC";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String BRANCH_MAIN = "main";

    // =========================================================================
    //  查询
    // =========================================================================

    @Override
    public PageResult<ResourceVO> pageResources(ResourceQueryDTO query) {
        // 关键：当前用户身份由服务端注入。前端就算传了 currentUserId / isAdmin 也会被这里覆盖
        query.setCurrentUserId(UserContext.getUserId());
        query.setIsAdmin(UserContext.isAdmin());

        long total = resourceMapper.countPage(query);
        if (total == 0) {
            return PageResult.empty(query.getPage(), query.getPageSize());
        }
        List<ResourceVO> list = resourceMapper.selectPage(query);

        // ---- 批量查课程负责人，避免 N+1 查询 ----
        // 反面写法：list.forEach(vo -> canModify(vo))，其中 canModify 会逐条查课程
        //          → 一页 10 条就是 10 次数据库往返
        Set<Long> courseIds = list.stream()
                .map(ResourceVO::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Long> ownerMap = courseIds.isEmpty()
                ? Collections.emptyMap()
                : courseMapper.selectByIds(new ArrayList<>(courseIds)).stream()
                        .collect(Collectors.toMap(Course::getId, Course::getOwnerId, (a, b) -> a));

        list.forEach(vo -> fillExtra(vo, ownerMap.get(vo.getCourseId())));

        return new PageResult<>(list, total, query.getPage(), query.getPageSize());
    }

    @Override
    public ResourceVO getDetail(Long id) {
        ResourceVO vo = resourceMapper.selectVOById(id);
        if (vo == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
        }
        // ---- 核心：三级可见性校验 ----
        checkVisible(vo);

        // 能走到这里说明有权限看，浏览数 +1
        resourceMapper.increaseViewCount(id);
        vo.setViewCount(vo.getViewCount() == null ? 1 : vo.getViewCount() + 1);

        // 详情只有一条，直接查课程即可（不需要批量）
        Course course = courseMapper.selectById(vo.getCourseId());
        fillExtra(vo, course == null ? null : course.getOwnerId());
        return vo;
    }

    // =========================================================================
    //  写操作
    // =========================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(ResourceSaveDTO dto) {
        Long uid = UserContext.getUserId();

        // ① 课程必须存在
        Course course = courseMapper.selectById(dto.getCourseId());
        if (course == null) {
            throw new BizException(ResultCode.COURSE_NOT_FOUND);
        }

        // ② 文件必须存在（前端先调上传接口拿到的 fileId）
        FileInfo file = fileMapper.selectById(dto.getFileId());
        if (file == null) {
            throw new BizException(ResultCode.FILE_NOT_FOUND, "文件不存在，请重新上传");
        }

        // ③ 插入资料主记录
        Resource resource = new Resource();
        resource.setCourseId(dto.getCourseId());
        resource.setChapterId(dto.getChapterId());
        resource.setUploaderId(uid);
        resource.setTitle(dto.getTitle());
        resource.setDescription(dto.getDescription() == null ? "" : dto.getDescription());
        resource.setType(isBlank(dto.getType()) ? DEFAULT_TYPE : dto.getType());
        resource.setVisibility(isBlank(dto.getVisibility()) ? DEFAULT_VISIBILITY : dto.getVisibility());
        resource.setStatus(STATUS_PUBLISHED);
        resource.setVersionCount(1);
        resourceMapper.insert(resource);

        // ④ 生成 v1 版本，指向刚上传的文件
        ResourceVersion version = new ResourceVersion();
        version.setResourceId(resource.getId());
        version.setFileId(file.getId());
        version.setParentVersionId(null);   // null = 根版本，版本树的起点
        version.setVersionNo(1);
        version.setBranch(BRANCH_MAIN);
        version.setChangeLog("初始版本");
        version.setEditorId(uid);
        version.setIsRollback(0);
        versionMapper.insert(version);

        // ⑤ 把 current_version_id 回填为 v1
        //    为什么要分三步而不是一步插入？
        //    因为 version 的 id 要等插入后才知道，所以只能先插 resource、
        //    再插 version、最后回填指针。这三步在同一事务里，不会出现中间状态。
        Resource pointer = new Resource();
        pointer.setId(resource.getId());
        pointer.setCurrentVersionId(version.getId());
        resourceMapper.update(pointer);

        // ⑥ 课程的资料数 +1（冗余字段，避免列表页对每门课 COUNT）
        courseMapper.increaseResourceCount(course.getId(), 1);

        log.info("发布资料 | resourceId={} | courseId={} | fileId={} | title={}",
                resource.getId(), dto.getCourseId(), file.getId(), dto.getTitle());
        return resource.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ResourceSaveDTO dto) {
        Resource exist = getExistingOrThrow(id);
        checkModifyPermission(exist);

        Resource update = new Resource();
        update.setId(id);
        update.setTitle(dto.getTitle());
        update.setDescription(dto.getDescription());
        update.setType(dto.getType());
        update.setVisibility(dto.getVisibility());
        update.setChapterId(dto.getChapterId());
        // 注意：这里刻意不处理 fileId。
        // 「换个文件」属于"产生新版本"，要同时更新 version 表和 version_count，
        // 这部分逻辑留到 M3（版本树）实现。
        resourceMapper.update(update);

        log.info("修改资料 | resourceId={} | operatorId={}", id, UserContext.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Resource exist = getExistingOrThrow(id);
        checkModifyPermission(exist);

        resourceMapper.softDelete(id);
        // 课程资料数 -1
        courseMapper.increaseResourceCount(exist.getCourseId(), -1);

        // 释放文件引用（引用计数 -1）
        // 注意：这里只减计数，不删物理文件。物理文件什么时候删？
        // 交给后续的定时任务统一清理（refCount 归零的孤儿文件），M4 实现。
        if (exist.getCurrentVersionId() != null) {
            ResourceVersion version = versionMapper.selectById(exist.getCurrentVersionId());
            if (version != null && version.getFileId() != null) {
                fileMapper.decreaseRefCount(version.getFileId());
            }
        }

        log.info("删除资料 | resourceId={} | operatorId={}", id, UserContext.getUserId());
    }

    @Override
    public FileDownloadResult download(Long id) {
        ResourceVO vo = resourceMapper.selectVOById(id);
        if (vo == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
        }
        // 下载前同样要校验可见性！
        // 否则私密资料只要猜到 ID 就能绕开接口直接下载，前面的校验全白做
        checkVisible(vo);

        if (vo.getFileId() == null) {
            throw new BizException(ResultCode.FILE_NOT_FOUND, "该资料没有关联文件");
        }
        resourceMapper.increaseDownloadCount(id);
        return fileService.download(vo.getFileId());
    }

    // =========================================================================
    //  私有方法 —— 权限
    // =========================================================================

    /**
     * 三级可见性校验（数据权限的核心）
     *
     * <p><b>规则表</b>：</p>
     * <table border="1">
     *   <tr><th>可见性</th><th>谁能看</th></tr>
     *   <tr><td>PUBLIC</td><td>所有人（含未登录）</td></tr>
     *   <tr><td>COURSE_ONLY</td><td>课程成员 + 课程负责人</td></tr>
     *   <tr><td>PRIVATE</td><td>仅上传者本人</td></tr>
     * </table>
     *
     * <p>另外：上传者本人和管理员对所有可见性都有访问权（放在最前面短路返回）。</p>
     */
    private void checkVisible(ResourceVO resource) {
        Long uid = UserContext.getUserId();

        // 上传者本人 → 放行
        if (uid != null && uid.equals(resource.getUploaderId())) {
            return;
        }
        // 管理员 → 放行
        if (UserContext.isAdmin()) {
            return;
        }

        String visibility = resource.getVisibility();

        // PUBLIC 谁都能看，包括未登录游客
        if ("PUBLIC".equals(visibility)) {
            return;
        }

        // 剩下的两种都必须登录
        if (uid == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }

        if ("COURSE_ONLY".equals(visibility)) {
            // 是本课程成员？
            if (courseMapper.countMember(resource.getCourseId(), uid) > 0) {
                return;
            }
            // 是本课程负责人？
            Course course = courseMapper.selectById(resource.getCourseId());
            if (course != null && uid.equals(course.getOwnerId())) {
                return;
            }
            throw new BizException(ResultCode.NOT_COURSE_MEMBER);
        }

        if ("PRIVATE".equals(visibility)) {
            // 上传者本人在最前面已经放行过了，走到这里说明不是本人
            throw new BizException(ResultCode.RESOURCE_NOT_ACCESSIBLE);
        }
    }

    /**
     * 能否修改：上传者 / 课程负责人 / 管理员
     */
    private boolean canModify(Long operatorId, Long uploaderId, boolean isAdmin, Long courseOwnerId) {
        if (operatorId == null) {
            return false;
        }
        return operatorId.equals(uploaderId)
                || isAdmin
                || operatorId.equals(courseOwnerId);
    }

    /**
     * 修改权限校验（写操作用，不通过直接抛 403）
     */
    private void checkModifyPermission(Resource resource) {
        Long uid = UserContext.getUserId();
        if (uid == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        if (uid.equals(resource.getUploaderId()) || UserContext.isAdmin()) {
            return;
        }
        Course course = courseMapper.selectById(resource.getCourseId());
        if (course != null && uid.equals(course.getOwnerId())) {
            return;
        }
        log.warn("越权操作资料被拒绝 | resourceId={} | uploaderId={} | operatorId={}",
                resource.getId(), resource.getUploaderId(), uid);
        throw new BizException(ResultCode.FORBIDDEN);
    }

    // =========================================================================
    //  私有方法 —— 其他
    // =========================================================================

    private Resource getExistingOrThrow(Long id) {
        Resource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return resource;
    }

    /**
     * 补齐 VO 里那些"需要额外信息"的字段
     *
     * @param courseOwnerId 所属课程的负责人 ID（由调用方批量查好传进来，避免 N+1）
     */
    private void fillExtra(ResourceVO vo, Long courseOwnerId) {
        if (vo.getFileSize() != null) {
            vo.setReadableSize(FileUtil.formatSize(vo.getFileSize()));
        }
        if (vo.getFileId() != null) {
            vo.setDownloadUrl("/api/resources/" + vo.getId() + "/download");
        }
        vo.setCanEdit(canModify(UserContext.getUserId(),
                vo.getUploaderId(), UserContext.isAdmin(), courseOwnerId));
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
