package com.cao.repairshop.auth.infra.persistence.repositories;

import com.cao.repairshop.auth.infra.persistence.models.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class UserRepository implements PanacheRepository<UserEntity> {

    public Optional<UserEntity> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
}
