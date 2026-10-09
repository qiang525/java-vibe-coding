package com.nnuist.coursehub.service;

import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.dto.ResourceQueryDTO;
import com.nnuist.coursehub.dto.ResourceSaveDTO;
import com.nnuist.coursehub.vo.FileDownloadResult;
import com.nnuist.coursehub.vo.ResourceVO;

/**
 * 资料业务接口
 *
 * <p>本模块最关键的是<b>三级可见性</b>：一份资料可以被设置为
 * 全平台可见 / 仅本课程可见 / 仅自己可见，服务端必须在每次读取和写入时校验。</p>
 *
 * @author CourseHub Team
 */
public interface ResourceService {

    /**
     * 分页查询资料列表
     *
     * <p>结果<b>已按当前用户的可见性权限过滤</b>，调用方不需再判权限。</p>
     */
    PageResult<ResourceVO> pageResources(ResourceQueryDTO query);

    /**
     * 查询资料详情
     *
     * <p>会做三级可见性校验：无权访问时抛 403。</p>
     */
    ResourceVO getDetail(Long id);

    /**
     * 发布资料
     *
     * <p>把"已上传的文件"挂到课程下，同时生成 v1 版本记录。</p>
     *
     * @return 新资料 ID
     */
    Long publish(ResourceSaveDTO dto);

    /**
     * 修改资料
     *
     * <p>只有上传者、课程负责人、管理员可以改。</p>
     */
    void update(Long id, ResourceSaveDTO dto);

    /**
     * 删除资料（逻辑删除）
     *
     * <p>同时会：课程资料数 -1、释放文件引用。</p>
     */
    void delete(Long id);

    /**
     * 下载资料对应的文件
     *
     * <p>同样要做可见性校验 —— 否则私密资料只要知道 ID 就能下载。</p>
     */
    FileDownloadResult download(Long id);
}
