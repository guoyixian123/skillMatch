package com.skillmatch.domain.dto;

import lombok.Data;

@Data
public class PasswordDTO {
    private String oldPassword;
    private String newPassword;
}
