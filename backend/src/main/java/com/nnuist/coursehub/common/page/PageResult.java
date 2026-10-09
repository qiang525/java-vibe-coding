package com.nnuist.coursehub.common.page;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果
 *
 * <p>所有列表接口都返回这个结构，前端拿到就能直接渲染分页组件。</p>
 *
 * <pre>
 * {
 *   "list": [ ... ],
 *   "total": 128,
 *   "page": 1,
 *   "pageSize": 10,
 *   "pages": 13
 * }
 * </pre>
 *
 * @param <T> 列表元素类型
 * @author CourseHub Team
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> list;

    /** 总记录数 */
    private Long total;

    /** 当前页码，从 1 开始 */
    private Integer page;

    /** 每页条数 */
    private Integer pageSize;

    /** 总页数，由 total 与 pageSize 计算得出 */
    private Integer pages;

    public PageResult() {
    }

    public PageResult(List<T> list, Long total, Integer page, Integer pageSize) {
        this.list = list == null ? Collections.emptyList() : list;
        this.total = total == null ? 0L : total;
        this.page = page == null ? 1 : page;
        this.pageSize = pageSize == null ? 10 : pageSize;
        this.pages = calculatePages(this.total, this.pageSize);
    }

    /**
     * 构造空结果，用于查询无数据的场景
     */
    public static <T> PageResult<T> empty(Integer page, Integer pageSize) {
        return new PageResult<>(Collections.emptyList(), 0L, page, pageSize);
    }

    /**
     * 由完整列表 + 页码分页（用于数据量小的场景，直接在内存里切）
     */
    public static <T> PageResult<T> of(List<T> all, Integer page, Integer pageSize) {
        if (all == null || all.isEmpty()) {
            return empty(page, pageSize);
        }
        int p = (page == null || page < 1) ? 1 : page;
        int size = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        int from = Math.min((p - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PageResult<>(all.subList(from, to), (long) all.size(), p, size);
    }

    private static int calculatePages(Long total, Integer pageSize) {
        if (total == null || total <= 0 || pageSize == null || pageSize <= 0) {
            return 0;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }
}
