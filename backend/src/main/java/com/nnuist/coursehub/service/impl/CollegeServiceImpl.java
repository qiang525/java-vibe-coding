package com.nnuist.coursehub.service.impl;

import com.nnuist.coursehub.entity.College;
import com.nnuist.coursehub.mapper.CollegeMapper;
import com.nnuist.coursehub.service.CollegeService;
import com.nnuist.coursehub.vo.CollegeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 学院业务实现
 *
 * @author CourseHub Team
 */
@Service
@RequiredArgsConstructor
public class CollegeServiceImpl implements CollegeService {

    private final CollegeMapper collegeMapper;

    @Override
    public List<CollegeVO> listAll() {
        List<College> colleges = collegeMapper.selectAll();
        // Entity -> VO：只把 id/name/code 三个字段给前端
        return colleges.stream().map(c -> {
            CollegeVO vo = new CollegeVO();
            BeanUtils.copyProperties(c, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
