package br.com.pulseapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa a configuração de uma API monitorada pelo sistema.
 * Mapeia os dados da tabela 'api_config' no banco de dados.
 */
@Entity
@Table(name = "api_config")
@Data
@NoArgsConstructor
public class ApiConfig {

    /**
     * Identificador único da configuração, gerado automaticamente.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * URL única da API a ser monitorada.
     */
    @Column(unique = true, nullable = false)
    @NotBlank(message = "A URL não pode estar em branco")
    private String apiUrl;

    /**
     * Nome descritivo da API.
     */
    @Column(nullable = false)
    @NotBlank(message = "O nome não pode estar em branco")
    private String apiName;

    /**
     * Canal de notificação (ex.: chatId do Telegram) pra envio de alertas.
     */
    @Column(nullable = false)
    @NotBlank(message = "O canal de notificação não pode estar em branco")
    private String notificationChannel;

    /**
     * Último status HTTP registrado da API (ex.: 200, 404).
     */
    private Integer lastHttpStatus;

    /**
     * Identificador do usuário responsável pela API.
     */
    @Column(nullable = false)
    @NotBlank(message = "O ID do usuário não pode estar em branco")
    private String ownerUserId;

    /**
     * Token de acesso único gerado para autenticação.
     */
    private String accessToken;

    /**
     * Intervalo de agendamento em milissegundos para monitoramento (ex.: 60000 para 1 minuto).
     */
    private Long scheduleInterval;

    /**
     * Construtor com campos obrigatórios pra criação de uma nova configuração.
     *
     * @param apiUrl URL única da API
     * @param apiName Nome da API
     * @param notificationChannel Canal de notificação
     * @param ownerUserId ID do usuário responsável
     */
    public ApiConfig(String apiUrl, String apiName, String notificationChannel, String ownerUserId) {
        this.apiUrl = apiUrl;
        this.apiName = apiName;
        this.notificationChannel = notificationChannel;
        this.ownerUserId = ownerUserId;
    }
}
