package com.nnuist.coursehub.common.page;

import lombok.Data;

/**
 * 分页请求参数基类
 *
 * <p>所有需要分页的查询 DTO 继承这个类即可获得 page / pageSize 字段，
 * 并自动处理边界值（页码不能小于 1，每页条数不能超过 100）。</p>
 *
 * @author CourseHub Team
 */
@Data
public class PageQuery {

    /** 最大每页条数，防止前端传 pageSize=999999 拖垮数据库 */
    public static final int MAX_PAGE_SIZE = 100;

    private Integer page = 1;

    private Integer pageSize = 10;

    /**
     * 排序字段（可选），需在 Service 层做白名单校验，防止 SQL 注入
     */
    private String sortField;

    /**
     * 排序方向：asc / desc
     */
    private String sortOrder = "desc";

    /**
     * 规范化页码 —— 在 Service 层调用
     */
    public Integer getPage() {
        return (page == null || page < 1) ? 1 : page;
    }

    /**
     * 规范化每页条数 —— 防止恶意传入超大值
     */
    public Integer getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * MyBatis 用的偏移量
     */
    public Integer getOffset() {
        return (getPage() - 1) * getPageSize();
    }
}
