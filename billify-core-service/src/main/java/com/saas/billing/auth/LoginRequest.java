package com.saas.billing.auth;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class LoginRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}