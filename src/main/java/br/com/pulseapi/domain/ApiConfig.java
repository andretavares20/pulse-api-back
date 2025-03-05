package br.com.pulseapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "api_config")
public class ApiConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // Garante que a URL seja única
    private String url;

    private String name;
    private String notificationChannel;
    private Integer lastStatus;
    private String userId;
    private String accessToken;
    private Long scheduleInterval;
}
