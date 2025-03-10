package br.com.pulseapi.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;

import br.com.pulseapi.entities.ConfiguracaoApiEntity;
import br.com.pulseapi.repository.ConfiguracaoApiRepository;
import br.com.pulseapi.service.ApiMonitorService;
import lombok.RequiredArgsConstructor;

/**
 * Configuração do agendador para monitoramento dinâmico de APIs registradas com intervalos ajustáveis.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulerConfig implements SchedulingConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerConfig.class);
    private static final long DEFAULT_INTERVAL_MS = 300_000L; // 5 minutos como padrão
    private static final long CHECK_INTERVAL_MS = 60_000L;   // Verifica mudanças a cada 1 minuto

    private final ApiMonitorService monitorService;
    private final ConfiguracaoApiRepository repository;

    private final Map<Long, Long> currentIntervals = new HashMap<>();
    private final Map<Long, ScheduledTaskRegistrar> taskRegistrars = new HashMap<>();

    @Override
    public void configureTasks(@NonNull ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedRateTask(
                new FixedRateTask(
                        this::refreshScheduledTasks,
                        Duration.ofMillis(CHECK_INTERVAL_MS),
                        Duration.ofMillis(CHECK_INTERVAL_MS) // initialDelay igual ao período
                )
        );
        logger.info("Inicializando agendamento de monitoramento das APIs...");
        refreshScheduledTasks();
    }

    private void refreshScheduledTasks() {
        logger.info("Verificando mudanças nos intervalos de agendamento...");
        List<ConfiguracaoApiEntity> apiConfigs = fetchAllApiConfigs();
        apiConfigs.forEach(this::scheduleApiMonitoringIfChanged);
    }

    private List<ConfiguracaoApiEntity> fetchAllApiConfigs() {
        List<ConfiguracaoApiEntity> apiConfigs = repository.findAll();
        logger.info("Encontradas {} APIs diretamente do banco via JpaRepository: {}", apiConfigs.size(), apiConfigs);
        return apiConfigs;
    }

    private void scheduleApiMonitoringIfChanged(ConfiguracaoApiEntity apiConfig) {
        Long apiId = apiConfig.getId();
        Long newInterval = apiConfig.getScheduleInterval() != null ? apiConfig.getScheduleInterval() : DEFAULT_INTERVAL_MS;
        Long currentInterval = currentIntervals.get(apiId);

        // Loga tarefas ativas antes de processar
        logger.info("Schedules ativos antes de processar API ID {} (URL: {}):", apiId, apiConfig.getApiUrl());
        for (Long scheduledApiId : taskRegistrars.keySet()) {
            Long scheduledInterval = currentIntervals.get(scheduledApiId);
            ConfiguracaoApiEntity config = repository.findById(scheduledApiId).orElse(null);
            String url = config != null ? config.getApiUrl() : "Desconhecido";
            logger.info(" - API ID: {}, URL: {}, Intervalo: {}ms", scheduledApiId, url, scheduledInterval);
        }

        if (currentInterval == null || !currentInterval.equals(newInterval)) {
            // Remove o registrador antigo, se existir
            ScheduledTaskRegistrar existingRegistrar = taskRegistrars.remove(apiId);
            if (existingRegistrar != null) {
                existingRegistrar.destroy();
                logger.info("Removida tarefa anterior para API ID {}", apiId);
            }

            // Cria um novo registrador para a tarefa
            ScheduledTaskRegistrar newRegistrar = new ScheduledTaskRegistrar();
            newRegistrar.addTriggerTask(
                    () -> monitorApi(apiId),
                    new PeriodicTrigger(Duration.ofMillis(newInterval))
            );
            newRegistrar.afterPropertiesSet(); // Inicializa a tarefa

            taskRegistrars.put(apiId, newRegistrar);
            currentIntervals.put(apiId, newInterval);
            logger.info("Agendando monitoramento para API ID {} (URL: {}) com intervalo de {}ms", apiId, apiConfig.getApiUrl(), newInterval);
            logger.info("Tarefa agendada com sucesso para API ID {}. Total de tarefas ativas: {}", apiId, taskRegistrars.size());
        } else {
            logger.info("Nenhuma mudança no intervalo para API ID {} (URL: {}), mantendo agendamento atual de {}ms",
                    apiId, apiConfig.getApiUrl(), currentInterval);
        }
    }

    private void monitorApi(Long apiId) {
        try {
            ConfiguracaoApiEntity freshConfig = repository.findById(apiId)
                    .orElseThrow(() -> new IllegalStateException("Configuração da API ID " + apiId + " não encontrada"));
            logger.info("Monitoramento disparado para API ID {} (URL: {}) com intervalo configurado de {}ms",
                    apiId, freshConfig.getApiUrl(), freshConfig.getScheduleInterval());
            monitorService.checkApiStatus(freshConfig);
        } catch (Exception e) {
            logger.error("Erro ao monitorar API ID {}: {}", apiId, e.getMessage());
        }
    }
}
