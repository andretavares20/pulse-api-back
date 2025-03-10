package br.com.pulseapi.model.dtos;

import lombok.Data;

@Data
public class EndpointDTO {
    private Long id;
    private Long userId;
    private String name;
    private String url;
    private String status;
}
