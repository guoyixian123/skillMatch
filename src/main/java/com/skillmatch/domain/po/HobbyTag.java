package com.skillmatch.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("hobby_tag")
public class HobbyTag {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    private String icon;
    private String category;
    private Integer sortOrder;
}
