package com.nnuist.coursehub.service;

import com.nnuist.coursehub.common.page.PageResult;
import com.nnuist.coursehub.vo.CollectionVO;

/**
 * 收藏业务接口
 *
 * @author CourseHub Team
 */
public interface CollectionService {

    /** 收藏资料 */
    void collect(Long resourceId, String folder);

    /** 取消收藏 */
    void uncollect(Long resourceId);

    /** 我的收藏列表 */
    PageResult<CollectionVO> myCollections(Integer page, Integer pageSize);

    /** 是否已收藏 */
    boolean isCollected(Long resourceId);
}
