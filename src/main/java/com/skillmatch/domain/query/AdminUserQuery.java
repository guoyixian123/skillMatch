package com.skillmatch.domain.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 管理端用户查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUserQuery extends PageQuery {

    private String userId;
    private String name;
    private Integer status;
    private Boolean robot;
    private String city;
    private String skillTag;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAtStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAtEnd;

    /** 地理范围：中心经度 */
    private Double centerLng;

    /** 地理范围：中心纬度 */
    private Double centerLat;

    /** 地理范围：半径（km） */
    private Double radiusKm;

    /** 快捷筛选：all_real / all_robot / all_frozen / pending_unfreeze / no_coords */
    private String quickFilter;
}
