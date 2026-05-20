package com.skillmatch.domain.vo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class GalleryVO {
    private String id;
    private String imageUrl;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
