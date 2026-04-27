package com.escola.disciplinaservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class CadastroClient {

    private static final Logger logger = LoggerFactory.getLogger(CadastroClient.class);

    private final RestTemplate restTemplate;
    private final String cadastroServiceUrl;

    public CadastroClient(RestTemplate restTemplate,
                          @Value("${cadastro-service.url}") String cadastroServiceUrl) {
        this.restTemplate = restTemplate;
        this.cadastroServiceUrl = cadastroServiceUrl;
    }

    /**
     * Verifica se um professor existe no cadastro-service.
     *
     * @CircuitBreaker: Apos 50% de falhas em 10 chamadas, o circuito ABRE
     *   e para de chamar o cadastro-service por 30s (protege contra cascata).
     *
     * @Retry: Tenta ate 3 vezes com backoff exponencial (1s, 2s, 4s)
     *   antes de considerar como falha.
     *
     * Fallback: Se tudo falhar, retorna false e loga o erro.
     */
    @CircuitBreaker(name = "cadastroService", fallbackMethod = "professorExisteFallback")
    @Retry(name = "cadastroService")
    public boolean professorExiste(Long professorId) {
        logger.info("[Resilience4j] Verificando professor {} no cadastro-service", professorId);
        try {
            restTemplate.getForObject(cadastroServiceUrl + "/professores/" + professorId, Object.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    /**
     * Fallback: chamado quando o circuit breaker esta aberto ou apos esgotar retries.
     */
    private boolean professorExisteFallback(Long professorId, Throwable t) {
        logger.warn("[Resilience4j] FALLBACK - cadastro-service indisponivel. " +
                "Nao foi possivel verificar professor {}. Erro: {}", professorId, t.getMessage());
        return false;
    }
}
