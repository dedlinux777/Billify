package com.saas.billing.dto;

import com.saas.billing.model.Role;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private Role role;

    // password not here
}