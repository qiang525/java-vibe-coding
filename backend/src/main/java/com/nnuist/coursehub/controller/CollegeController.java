package com.nnuist.coursehub.controller;

import com.nnuist.coursehub.common.result.Result;
import com.nnuist.coursehub.service.CollegeService;
import com.nnuist.coursehub.vo.CollegeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学院接口
 *
 * <p>学院是基础字典数据（一个学校十几个），一次全查出来给前端缓存即可。
 * 本接口<b>不需要登录</b>——注册页面还没登录就要用学院下拉框。</p>
 *
 * @author CourseHub Team
 */
@Slf4j
@Api(tags = "02-学院")
@RestController
@RequestMapping("/api/colleges")
@RequiredArgsConstructor
public class CollegeController {

    private final CollegeService collegeService;

    @ApiOperation("查询全部学院（供下拉框与筛选使用）")
    @GetMapping
    public Result<List<CollegeVO>> listAll() {
        return Result.ok(collegeService.listAll());
    }
}
