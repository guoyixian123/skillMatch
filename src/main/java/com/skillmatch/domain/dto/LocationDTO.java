package com.skillmatch.domain.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocationDTO {
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180", message = "经度范围-180~180")
    @DecimalMax(value = "180", message = "经度范围-180~180")
    private Double longitude;
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90", message = "纬度范围-90~90")
    @DecimalMax(value = "90", message = "纬度范围-90~90")
    private Double latitude;
}
