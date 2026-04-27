package com.escola.matriculaservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class DisciplinaClient {

    private static final Logger logger = LoggerFactory.getLogger(DisciplinaClient.class);

    private final RestTemplate restTemplate;
    private final String disciplinaServiceUrl;

    public DisciplinaClient(RestTemplate restTemplate,
                            @Value("${disciplina-service.url}") String disciplinaServiceUrl) {
        this.restTemplate = restTemplate;
        this.disciplinaServiceUrl = disciplinaServiceUrl;
    }

    /**
     * Verifica se uma disciplina existe no disciplina-service.
     *
     * @CircuitBreaker: Apos 50% de falhas em 10 chamadas, o circuito ABRE
     *   e para de chamar o disciplina-service por 30s.
     *
     * @Retry: Tenta ate 3 vezes com backoff exponencial (1s, 2s, 4s).
     *
     * Fallback: Se o servico estiver fora, retorna false.
     */
    @CircuitBreaker(name = "disciplinaService", fallbackMethod = "disciplinaExisteFallback")
    @Retry(name = "disciplinaService")
    public boolean disciplinaExiste(Long disciplinaId) {
        logger.info("[Resilience4j] Verificando disciplina {} no disciplina-service", disciplinaId);
        try {
            restTemplate.getForObject(disciplinaServiceUrl + "/disciplinas/" + disciplinaId, Object.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    /**
     * Fallback: chamado quando o circuit breaker esta aberto ou apos esgotar retries.
     */
    private boolean disciplinaExisteFallback(Long disciplinaId, Throwable t) {
        logger.warn("[Resilience4j] FALLBACK - disciplina-service indisponivel. " +
                "Nao foi possivel verificar disciplina {}. Erro: {}", disciplinaId, t.getMessage());
        return false;
    }
}
