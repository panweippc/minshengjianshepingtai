package com.msjs.project.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页统一返回对象
 */
@Data
@Schema(description = "分页返回对象")
public class PageVO<T> implements Serializable {

    @Schema(description = "数据列表")
    private List<T> records;

    @Schema(description = "总条数")
    private Long total;

    @Schema(description = "当前页码")
    private Long pageNum;

    @Schema(description = "每页条数")
    private Long pageSize;

    public static <T> PageVO<T> of(IPage<T> page) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(page.getRecords() == null ? Collections.emptyList() : page.getRecords());
        vo.setTotal(page.getTotal());
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        return vo;
    }
}
