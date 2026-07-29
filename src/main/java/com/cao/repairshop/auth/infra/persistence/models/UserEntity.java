package com.cao.repairshop.auth.infra.persistence.models;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tb_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id_tb_user")
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "function", nullable = false, length = 50)
    private String function;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 255)
    private String password;
}
