package br.com.pulseapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.pulseapi.entities.PlanoEntity;

public interface PlanoRepository extends JpaRepository<PlanoEntity, Long> {
    PlanoEntity findByName(String name);
}
