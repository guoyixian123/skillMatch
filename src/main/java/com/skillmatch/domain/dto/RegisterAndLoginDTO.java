package com.skillmatch.domain.dto;

import lombok.Data;

@Data
public class RegisterAndLoginDTO {
    private String userId;
    private String name;
    private String password;
}
