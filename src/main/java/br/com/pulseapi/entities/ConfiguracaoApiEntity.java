package br.com.pulseapi.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "configuracao_api")
@Data
@NoArgsConstructor
public class ConfiguracaoApiEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank(message = "A URL não pode estar em branco")
    private String apiUrl;

    @Column(nullable = false)
    @NotBlank(message = "O nome não pode estar em branco")
    private String apiName;

    @Column(nullable = false)
    @NotBlank(message = "O canal de notificação não pode estar em branco")
    private String notificationChannel;

    private Integer lastHttpStatus;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    private String accessToken;

    private Long scheduleInterval;

    public ConfiguracaoApiEntity(String apiUrl, String apiName, String notificationChannel, UserEntity user) {
        this.apiUrl = apiUrl;
        this.apiName = apiName;
        this.notificationChannel = notificationChannel;
        this.user = user;
    }
}
