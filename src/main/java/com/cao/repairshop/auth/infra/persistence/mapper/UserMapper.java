package com.cao.repairshop.auth.infra.persistence.mapper;

import com.cao.repairshop.auth.domain.entities.User;
import com.cao.repairshop.auth.infra.persistence.models.UserEntity;

public class UserMapper {

    public static User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return User.builder()
                .id(entity.getId())
                .name(entity.getName())
                .function(entity.getFunction())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .password(entity.getPassword())
                .build();
    }

    public static UserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }
        UserEntity entity = new UserEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }
        entity.setName(domain.getName());
        entity.setFunction(domain.getFunction());
        entity.setEmail(domain.getEmail());
        entity.setPhone(domain.getPhone());
        entity.setPassword(domain.getPassword());
        return entity;
    }
}
