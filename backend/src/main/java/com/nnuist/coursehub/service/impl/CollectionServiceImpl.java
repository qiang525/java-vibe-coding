package com.nnuist.coursehub.service.impl;

import com.nnuist.coursehub.common.exception.BizException;
import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.common.result.ResultCode;
import com.nnuist.coursehub.common.util.FileUtil;
import com.nnuist.coursehub.common.util.UserContext;
import com.nnuist.coursehub.entity.Resource;
import com.nnuist.coursehub.mapper.CollectionMapper;
import com.nnuist.coursehub.mapper.ResourceMapper;
import com.nnuist.coursehub.service.CollectionService;
import com.nnuist.coursehub.vo.CollectionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收藏业务实现
 *
 * <p>防重复收藏用了两道防线：先查一次，同时数据库上还有唯一索引兜底。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionMapper collectionMapper;
    private final ResourceMapper resourceMapper;

    private static final String DEFAULT_FOLDER = "默认收藏夹";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collect(Long resourceId, String folder) {
        Long uid = UserContext.getUserId();

        Resource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (collectionMapper.countByUserAndResource(uid, resourceId) > 0) {
            throw new BizException(ResultCode.ALREADY_COLLECTED);
        }

        collectionMapper.insert(uid, resourceId,
                (folder == null || folder.trim().isEmpty()) ? DEFAULT_FOLDER : folder);
        resourceMapper.increaseFavoriteCount(resourceId);

        log.info("收藏资料 | resourceId={} | userId={}", resourceId, uid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollect(Long resourceId) {
        Long uid = UserContext.getUserId();

        // 幂等：没收藏过也当成成功，避免前端重复点击报错
        if (collectionMapper.countByUserAndResource(uid, resourceId) == 0) {
            return;
        }
        collectionMapper.delete(uid, resourceId);
        resourceMapper.decreaseFavoriteCount(resourceId);

        log.info("取消收藏 | resourceId={} | userId={}", resourceId, uid);
    }

    @Override
    public PageResult<CollectionVO> myCollections(Integer page, Integer pageSize) {
        Long uid = UserContext.getUserId();
        int p = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100);

        long total = collectionMapper.countPage(uid);
        if (total == 0) {
            return PageResult.empty(p, size);
        }

        List<CollectionVO> list = collectionMapper.selectPage(uid, (p - 1) * size, size);
        list.forEach(vo -> {
            if (vo.getFileSize() != null) {
                vo.setReadableSize(FileUtil.formatSize(vo.getFileSize()));
            }
        });
        return new PageResult<>(list, total, p, size);
    }

    @Override
    public boolean isCollected(Long resourceId) {
        Long uid = UserContext.getUserId();
        if (uid == null) {
            return false;
        }
        return collectionMapper.countByUserAndResource(uid, resourceId) > 0;
    }
}
