package com.skillmatch.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端新增/编辑用户
 */
@Data
public class AdminUserSaveDTO {

    /** 新建时留空自动生成，编辑时必传 */
    private String userId;

    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 16, message = "昵称长度1-16个字符")
    private String name;

    /** 密码，新建时留空默认123456 */
    private String password;

    private String contactInfo;
    private String avatarUrl;
    private String signature;
    private String bio;
    private Double latitude;
    private Double longitude;
    private String city;

    /** 是否机器人 */
    private Boolean robot;

    /** 状态：1=正常 2=冻结 */
    private Integer status;
}
