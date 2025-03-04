package br.com.pulseapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.pulseapi.domain.ApiConfig;


public interface ApiConfigRepository extends JpaRepository<ApiConfig, Long> {
    boolean existsByUrl(String url);
    boolean existsByAccessToken(String accessToken);
    Optional<ApiConfig> findByUrl(String url);
    Optional<ApiConfig> findByAccessToken(String accessToken);
}
