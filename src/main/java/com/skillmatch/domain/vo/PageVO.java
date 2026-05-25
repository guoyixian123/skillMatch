package com.skillmatch.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {
    private Long total;
    private Integer page=1;
    private Integer size=10;
    private List<T> list;

    public PageVO(long total, long pages, List<T> postVOList) {
        this.total = total;
        this.page = (int) pages;
        this.list = postVOList;
    }
    public PageVO(long total,long size, long page, List<T> postVOList) {
        this.total = total;
        this.page = (int) page;
        this.size = (int) size;
        this.list = postVOList;
    }

}
