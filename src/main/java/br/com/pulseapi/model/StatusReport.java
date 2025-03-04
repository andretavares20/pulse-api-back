package br.com.pulseapi.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
public class StatusReport {
    private String apiUrl;  // URL base da API
    private int status;     // Código de status HTTP
    private String endpoint;// Endpoint específico onde ocorreu o status
}
