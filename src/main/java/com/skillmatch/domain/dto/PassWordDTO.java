package com.skillmatch.domain.dto;

import lombok.Data;

@Data
public class PassWordDTO {
    private String oldPassword;
    private String newPassword;
}
