package com.skillmatch.domain.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public PageVO(long total,Integer size, Integer page, List<T> postVOList) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.list = postVOList;
    }

    public static <T, E> PageVO<T> of(Page<E> page, List<T> list) {

        PageVO<T> vo = new PageVO<>();

        vo.setTotal(page.getTotal());

        vo.setPage((int) page.getCurrent());

        vo.setSize((int) page.getSize());

        vo.setList(list);

        return vo;
    }

}
