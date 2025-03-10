package br.com.pulseapi.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.com.pulseapi.entities.ConfiguracaoApiEntity;
import br.com.pulseapi.entities.UserEntity;
import br.com.pulseapi.model.dtos.EndpointDTO;
import br.com.pulseapi.repository.ConfiguracaoApiRepository;

@Service
public class ConfiguracaoApiService {

    private final ConfiguracaoApiRepository configuracaoApiRepository;

    public ConfiguracaoApiService(ConfiguracaoApiRepository configuracaoApiRepository) {
        this.configuracaoApiRepository = configuracaoApiRepository;
    }

    public List<EndpointDTO> findByUser(UserEntity user) {
        List<ConfiguracaoApiEntity> configs = configuracaoApiRepository.findByUser(user);
        return configs.stream().map(this::mapToEndpointDTO).collect(Collectors.toList());
    }

    public EndpointDTO save(ConfiguracaoApiEntity configuracaoApi) {
        ConfiguracaoApiEntity savedConfig = configuracaoApiRepository.save(configuracaoApi);
        return mapToEndpointDTO(savedConfig);
    }

    public void deleteById(Long id) {
        configuracaoApiRepository.deleteById(id);
    }

    private EndpointDTO mapToEndpointDTO(ConfiguracaoApiEntity config) {
        EndpointDTO dto = new EndpointDTO();
        dto.setId(config.getId());
        dto.setUserId(config.getUser().getId());
        dto.setName(config.getApiName());
        dto.setUrl(config.getApiUrl());
        if (config.getLastHttpStatus() == null) {
            dto.setStatus("Unknown");
        } else if (config.getLastHttpStatus() >= 200 && config.getLastHttpStatus() < 300) {
            dto.setStatus("Online");
        } else {
            dto.setStatus("Offline");
        }
        return dto;
    }
}
