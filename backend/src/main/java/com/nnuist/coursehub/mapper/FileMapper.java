package com.nnuist.coursehub.mapper;

import com.nnuist.coursehub.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文件数据访问接口
 *
 * @author CourseHub Team
 */
@Mapper
public interface FileMapper {

    /**
     * 按 MD5 查询文件 —— <b>秒传的核心查询</b>
     *
     * <p>走唯一索引 {@code uk_file_hash}，是等值查询，速度极快。
     * 命中就说明"这份文件已经有人传过了"。</p>
     */
    FileInfo selectByHash(@Param("fileHash") String fileHash);

    /**
     * 按主键查询
     */
    FileInfo selectById(@Param("id") Long id);

    /**
     * 新增文件记录
     */
    int insert(FileInfo fileInfo);

    /**
     * 引用计数 +1（被新资料引用时调用）
     *
     * <p>用 {@code ref_count = ref_count + 1} 而不是"先查出来加一再写回"，
     * 是为了避免并发覆盖：两个人同时引用时，先查后写会只加一次。</p>
     */
    int increaseRefCount(@Param("id") Long id);

    /**
     * 引用计数 -1
     *
     * <p>末尾的 {@code ref_count > 0} 防止减成负数。</p>
     */
    int decreaseRefCount(@Param("id") Long id);

    /**
     * 物理删除记录
     *
     * <p>只在"引用计数已归零、物理文件也删掉了"之后调用。
     * 否则会留下一条指向不存在文件的脏记录。</p>
     */
    int deleteById(@Param("id") Long id);
}
