package com.saas.billing.dto.response;

import com.saas.billing.entity.Role;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
}
