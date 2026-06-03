package com.skillmatch.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量创建机器人
 */
@Data
public class BatchRobotDTO {

    @NotBlank(message = "名称前缀不能为空")
    private String namePrefix;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "至少创建1个")
    private Integer count;

    private List<String> skillTags;
    private List<String> hobbyTags;

    /** 中心纬度 */
    private Double centerLat;

    /** 中心经度 */
    private Double centerLng;

    /** 散布半径（km） */
    private Double spreadKm;
}
