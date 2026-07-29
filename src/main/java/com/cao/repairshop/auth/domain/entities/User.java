package com.cao.repairshop.auth.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID id;
    private String name;
    private String function;
    private String email;
    private String phone;
    private String password;
}
