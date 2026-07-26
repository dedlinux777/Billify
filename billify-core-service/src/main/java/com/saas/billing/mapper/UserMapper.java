package com.saas.billing.mapper;

import com.saas.billing.dto.response.UserResponse;
import com.saas.billing.entity.User;

public class UserMapper {

    public static UserResponse toDTO(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}