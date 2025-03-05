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
    private final Map<Long, Long> currentIntervals = new HashMap<>(); // Armazena os intervalos atuais

    /**
     * Inicializa o monitoramento das APIs ao iniciar a aplicação.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationStart() {
        logger.info("Inicializando agendamento de monitoramento das APIs...");
        scheduleMonitoringTasks();
    }

    /**
     * Verifica mudanças nos intervalos de agendamento a cada 1 minuto e reagenda apenas se necessário.
     */
    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    public void refreshScheduledTasks() {
        logger.debug("Verificando mudanças nos intervalos de agendamento...");
        scheduleMonitoringTasks();
    }

    private void scheduleMonitoringTasks() {
        List<ApiConfig> apiConfigs = fetchAllApiConfigs();
        apiConfigs.forEach(this::scheduleApiMonitoringIfChanged);
    }

    private List<ApiConfig> fetchAllApiConfigs() {
        List<ApiConfig> apiConfigs = repository.findAll();
        logger.debug("Encontradas {} APIs diretamente do banco via JpaRepository: {}", apiConfigs.size(), apiConfigs);
        return apiConfigs;
    }

    /**
     * Agenda o monitoramento de uma API apenas se o intervalo mudou, sem executar imediatamente o checkApiStatus.
     */
    private void scheduleApiMonitoringIfChanged(ApiConfig apiConfig) {
        Long apiId = apiConfig.getId();
        Long newInterval = apiConfig.getScheduleInterval() != null ? apiConfig.getScheduleInterval() : DEFAULT_INTERVAL_MS;
        Long currentInterval = currentIntervals.get(apiId);

        // Só reagenda se o intervalo mudou ou é a primeira vez
        if (currentInterval == null || !currentInterval.equals(newInterval)) {
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

            logger.info("Agendando monitoramento para API ID {} (URL: {}) com intervalo de {}ms", apiId, apiConfig.getUrl(), newInterval);
            ScheduledFuture<?> newTask = scheduler.scheduleAtFixedRate(
                monitoringTask,
                newInterval, // Atraso inicial igual ao intervalo
                newInterval,
                TimeUnit.MILLISECONDS
            );
            scheduledTasks.put(apiId, newTask);
            currentIntervals.put(apiId, newInterval); // Atualiza o intervalo atual
        } else {
            logger.debug("Nenhuma mudança no intervalo para API ID {} (URL: {}), mantendo agendamento atual de {}ms", apiId, apiConfig.getUrl(), currentInterval);
        }
    }
}
