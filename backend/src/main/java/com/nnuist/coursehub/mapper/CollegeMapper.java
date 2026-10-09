package com.nnuist.coursehub.mapper;

import com.nnuist.coursehub.entity.College;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学院数据访问接口
 *
 * <p>学院是基础字典数据，操作很简单：查全部、按 ID 查、新增。</p>
 *
 * <p>真正的 SQL 写在 {@code resources/mapper/CollegeMapper.xml}。</p>
 *
 * @author CourseHub Team
 */
@Mapper
public interface CollegeMapper {

    /**
     * 查询全部学院，按 sort_order 升序
     *
     * <p>数据量极小（一个学校十几个学院），一次查完缓存到前端即可，
     * 不需要分页。</p>
     */
    List<College> selectAll();

    /**
     * 按主键查询学院
     */
    College selectById(@Param("id") Long id);

    /**
     * 新增学院
     */
    int insert(College college);
}
