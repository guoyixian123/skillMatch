package com.skillmatch.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDTO {
    //昵称，1-16字符，不传不更新
    @NotBlank(message = "昵称不能为空")
    private String name;
    //个性签名，≤64字符
    @Size(max = 64, message = "个性签名长度不能超过64个字符")
    private String signature;
    //个人简介，≤500字符
    @Size(max = 500, message = "个人简介长度不能超过500个字符")
    private String bio;
    //联系方式，如微信号，QQ号，手机号，邮箱，其他联系方式，≤64字符
    @Size(max = 32, message = "联系方式长度不能超过64个字符")
    private String contactInfo;
}
