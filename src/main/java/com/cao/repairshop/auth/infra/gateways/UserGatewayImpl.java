package com.cao.repairshop.auth.infra.gateways;

import com.cao.repairshop.auth.application.gateways.UserGateway;
import com.cao.repairshop.auth.domain.entities.User;
import com.cao.repairshop.auth.infra.persistence.mapper.UserMapper;
import com.cao.repairshop.auth.infra.persistence.models.UserEntity;
import com.cao.repairshop.auth.infra.persistence.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class UserGatewayImpl implements UserGateway {

    @Inject
    UserRepository userRepository;

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = UserMapper.toEntity(user);
        userRepository.persist(entity);
        return UserMapper.toDomain(entity);
    }
}
