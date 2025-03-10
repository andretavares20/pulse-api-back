package br.com.pulseapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.pulseapi.entities.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByEmail(String email);
}
