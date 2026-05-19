package com.skillmatch.domain.dto;

import lombok.Data;

@Data
public class UserDTO {
    //昵称，1-16字符，不传不更新
    private String name;
    //个性签名，≤64字符
    private String signature;
    //个人简介，≤500字符
    private String bio;
    //联系方式，如微信号，QQ号，手机号，邮箱，其他联系方式，≤64字符
    private String contactInfo;
}
