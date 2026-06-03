package com.skillmatch.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 省份用户分布 VO
 * 用于地图展示各省用户数量
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceDistributionVO {
    /** 省份名称，如 "浙江"、"北京" */
    private String province;

    /** 该省份用户数 */
    private Integer count;
}