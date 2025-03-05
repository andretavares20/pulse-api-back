package br.com.pulseapi.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import br.com.pulseapi.domain.ApiConfig;
import br.com.pulseapi.repository.ApiConfigRepository;
import br.com.pulseapi.service.ApiMonitorService;
import lombok.RequiredArgsConstructor;

/**
 * Configuração do agendador para monitoramento dinâmico de APIs registradas com intervalos ajustáveis.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerConfig.class);
    private static final long DEFAULT_INTERVAL_MS = 300_000L; // 5 minutos como padrão
    private static final long CHECK_INTERVAL_MS = 60_000L;   // Verifica mudanças a cada 1 minuto

    private final ApiMonitorService monitorService;
    private final ApiConfigRepository repository;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    /**
     * Inicializa o monitoramento das APIs ao iniciar a aplicação.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationStart() {
        logger.info("Inicializando agendamento de monitoramento das APIs...");
        scheduleMonitoringTasks();
    }

    /**
     * Verifica mudanças no banco e reagenda tarefas a cada 1 minuto.
     */
    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    public void refreshScheduledTasks() {
        logger.debug("Verificando mudanças nos intervalos de agendamento...");
        scheduleMonitoringTasks();
    }

    private void scheduleMonitoringTasks() {
        List<ApiConfig> apiConfigs = fetchAllApiConfigs();
        apiConfigs.forEach(this::scheduleApiMonitoring);
    }

    private List<ApiConfig> fetchAllApiConfigs() {
        List<ApiConfig> apiConfigs = repository.findAll();
        logger.debug("Encontradas {} APIs diretamente do banco via JpaRepository: {}", apiConfigs.size(), apiConfigs);
        return apiConfigs;
    }

    /**
     * Agenda o monitoramento de uma API sem executar imediatamente o checkApiStatus.
     */
    private void scheduleApiMonitoring(ApiConfig apiConfig) {
        Long apiId = apiConfig.getId();
        Long interval = apiConfig.getScheduleInterval() != null ? apiConfig.getScheduleInterval() : DEFAULT_INTERVAL_MS;

        ScheduledFuture<?> existingTask = scheduledTasks.get(apiId);
        if (existingTask != null) {
            existingTask.cancel(false);
            logger.debug("Cancelada tarefa anterior para API ID {}", apiId);
        }

        Runnable monitoringTask = () -> {
            try {
                logger.debug("Executando monitoramento da API: {}", apiConfig.getUrl());
                monitorService.checkApiStatus(apiConfig);
            } catch (Exception e) {
                logger.error("Erro ao monitorar API {}: {}", apiConfig.getUrl(), e.getMessage());
            }
        };

        logger.info("Agendando monitoramento para API ID {} (URL: {}) com intervalo de {}ms", apiId, apiConfig.getUrl(), interval);
        ScheduledFuture<?> newTask = scheduler.scheduleAtFixedRate(
            monitoringTask,
            interval, // Atraso inicial igual ao intervalo pra evitar execução imediata
            interval,
            TimeUnit.MILLISECONDS
        );
        scheduledTasks.put(apiId, newTask);
    }
}
