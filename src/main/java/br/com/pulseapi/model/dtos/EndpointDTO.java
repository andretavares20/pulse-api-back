package br.com.pulseapi.model.dtos;

import lombok.Data;

@Data
public class EndpointDTO {
    private Long id;
    private Long userId;
    private String name;
    private String url;
    private Integer status;
    private String scheduleInterval; // Alterado de Long para String, conforme o backend atual
    private String notificationChannel;
}
