package br.com.pulseapi.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import br.com.pulseapi.domain.ApiConfig;
import br.com.pulseapi.repository.ApiConfigRepository;
import br.com.pulseapi.service.ApiMonitorService;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SchedulerConfig {
    private final ApiMonitorService monitorService;
    private final ApiConfigRepository repository;

    @Scheduled(fixedRate = 300000) // 5 minutos
    public void monitorApis() {
        List<ApiConfig> listApiConfig = repository.findAll();

        listApiConfig.forEach(monitorService::checkApiStatus);
    }
}
