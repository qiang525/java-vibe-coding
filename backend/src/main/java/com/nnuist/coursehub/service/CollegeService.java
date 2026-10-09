package com.nnuist.coursehub.service;

import com.nnuist.coursehub.vo.CollegeVO;

import java.util.List;

/**
 * 学院业务接口
 *
 * @author CourseHub Team
 */
public interface CollegeService {

    /**
     * 查询全部学院（供下拉框与筛选使用）
     */
    List<CollegeVO> listAll();
}
