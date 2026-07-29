package com.cao.repairshop.auth.application.gateways;

import com.cao.repairshop.auth.domain.entities.User;
import java.util.Optional;

public interface UserGateway {
    Optional<User> findByEmail(String email);
    User save(User user);
}
