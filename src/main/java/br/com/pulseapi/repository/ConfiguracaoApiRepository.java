package br.com.pulseapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.pulseapi.entities.ConfiguracaoApiEntity;
import br.com.pulseapi.entities.UserEntity;


/**
 * Repositório para gerenciamento de configurações de APIs monitoradas.
 * Fornece métodos para consultar e verificar a existência de configurações no banco de dados.
 */
public interface ConfiguracaoApiRepository extends JpaRepository<ConfiguracaoApiEntity, Long> {

    /**
     * Verifica se existe uma configuração com a URL da API especificada.
     *
     * @param apiUrl URL da API a ser verificada
     * @return true se a URL já estiver registrada, false caso contrário
     */
    boolean existsByApiUrl(String apiUrl);

    /**
     * Verifica se existe uma configuração com o token de acesso especificado.
     *
     * @param accessToken Token de acesso a ser verificado
     * @return true se o token já estiver registrado, false caso contrário
     */
    boolean existsByAccessToken(String accessToken);

    /**
     * Busca uma configuração de API pela URL.
     *
     * @param apiUrl URL da API a ser buscada
     * @return Optional contendo a configuração, ou vazio se não encontrada
     */
    Optional<ConfiguracaoApiEntity> findByApiUrl(String apiUrl);

    /**
     * Busca uma configuração de API pelo token de acesso.
     *
     * @param accessToken Token de acesso a ser buscado
     * @return Optional contendo a configuração, ou vazio se não encontrada
     */
    Optional<ConfiguracaoApiEntity> findByAccessToken(String accessToken);

    List<ConfiguracaoApiEntity> findByUser(UserEntity user);
}
