package com.saas.billing.mapper;

import com.saas.billing.dto.UserDTO;
import com.saas.billing.model.User;

public class UserMapper {

    public static UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}