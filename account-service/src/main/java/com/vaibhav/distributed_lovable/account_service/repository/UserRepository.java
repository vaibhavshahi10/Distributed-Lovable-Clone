package com.vaibhav.distributed_lovable.account_service.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.vaibhav.distributed_lovable.account_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String email);
}
