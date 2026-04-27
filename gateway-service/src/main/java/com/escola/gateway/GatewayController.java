package com.escola.gateway;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.regex.Pattern;

@RestController
@CrossOrigin(origins = "*")
public class GatewayController {
    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    private final RestTemplate restTemplate;
    private final String cadastroServiceUrl;
    private final String matriculaServiceUrl;
    private final String disciplinaServiceUrl;

    public GatewayController(@Value("${CADASTRO_SERVICE_URL:http://cadastro-service:8081}") String cadastroServiceUrl,
                             @Value("${MATRICULA_SERVICE_URL:http://matricula-service:8082}") String matriculaServiceUrl,
                             @Value("${DISCIPLINA_SERVICE_URL:http://disciplina-service:8083}") String disciplinaServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.cadastroServiceUrl = cadastroServiceUrl;
        this.matriculaServiceUrl = matriculaServiceUrl;
        this.disciplinaServiceUrl = disciplinaServiceUrl;
    }

    private URI buildServiceUri(String serviceUrl, String prefix, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String path = requestUri.replaceFirst("^" + Pattern.quote(prefix), "");
        if (path.isEmpty()) {
            path = "/";
        }
        logger.debug("[Gateway] requestUri='{}' serviceUrl='{}' path='{}' query='{}'",
                requestUri, serviceUrl, path, request.getQueryString());

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serviceUrl).path(path);
        if (request.getQueryString() != null) {
            builder.query(request.getQueryString());
        }
        URI uri = builder.build(true).toUri();
        logger.debug("[Gateway] uri='{}'", uri);
        return uri;
    }

    private ResponseEntity<String> proxyRequest(String serviceUrl, String prefix,
                                                 HttpServletRequest request, String body) {
        URI uri = buildServiceUri(serviceUrl, prefix, request);
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            return restTemplate.exchange(uri, method, entity, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());
        }
    }

    // ==========================================
    // Rotas com Circuit Breaker + Retry
    // ==========================================

    /**
     * Rota /cadastro/** com protecao Resilience4j.
     * Se o cadastro-service estiver fora, o circuit breaker abre e retorna fallback.
     */
    @CircuitBreaker(name = "cadastroRoute", fallbackMethod = "cadastroFallback")
    @Retry(name = "cadastroRoute")
    @RequestMapping(value = "/cadastro/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public ResponseEntity<String> proxyCadastro(HttpServletRequest request,
                                                 @RequestBody(required = false) String body) {
        logger.info("[Gateway] Roteando para cadastro-service: {} {}", request.getMethod(), request.getRequestURI());
        return proxyRequest(cadastroServiceUrl, "/cadastro", request, body);
    }

    /**
     * Rota /matricula/** com protecao Resilience4j.
     */
    @CircuitBreaker(name = "matriculaRoute", fallbackMethod = "matriculaFallback")
    @Retry(name = "matriculaRoute")
    @RequestMapping(value = "/matricula/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public ResponseEntity<String> proxyMatricula(HttpServletRequest request,
                                                  @RequestBody(required = false) String body) {
        logger.info("[Gateway] Roteando para matricula-service: {} {}", request.getMethod(), request.getRequestURI());
        return proxyRequest(matriculaServiceUrl, "/matricula", request, body);
    }

    /**
     * Rota /disciplina/** com protecao Resilience4j.
     */
    @CircuitBreaker(name = "disciplinaRoute", fallbackMethod = "disciplinaFallback")
    @Retry(name = "disciplinaRoute")
    @RequestMapping(value = "/disciplina/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public ResponseEntity<String> proxyDisciplina(HttpServletRequest request,
                                                   @RequestBody(required = false) String body) {
        logger.info("[Gateway] Roteando para disciplina-service: {} {}", request.getMethod(), request.getRequestURI());
        return proxyRequest(disciplinaServiceUrl, "/disciplina", request, body);
    }

    // ==========================================
    // Fallbacks - Respostas quando servicos estao fora
    // ==========================================

    /**
     * Fallback do cadastro-service: retorna 503 com mensagem amigavel.
     * Chamado quando o circuit breaker esta ABERTO ou apos esgotar retries.
     */
    private ResponseEntity<String> cadastroFallback(HttpServletRequest request, String body, Throwable t) {
        logger.error("[Gateway] CIRCUIT BREAKER ABERTO - cadastro-service indisponivel. Erro: {}", t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"erro\": \"Servico de cadastro temporariamente indisponivel. Tente novamente em alguns instantes.\", \"servico\": \"cadastro-service\", \"status\": \"CIRCUIT_BREAKER_OPEN\"}");
    }

    /**
     * Fallback do matricula-service.
     */
    private ResponseEntity<String> matriculaFallback(HttpServletRequest request, String body, Throwable t) {
        logger.error("[Gateway] CIRCUIT BREAKER ABERTO - matricula-service indisponivel. Erro: {}", t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"erro\": \"Servico de matricula temporariamente indisponivel. Tente novamente em alguns instantes.\", \"servico\": \"matricula-service\", \"status\": \"CIRCUIT_BREAKER_OPEN\"}");
    }

    /**
     * Fallback do disciplina-service.
     */
    private ResponseEntity<String> disciplinaFallback(HttpServletRequest request, String body, Throwable t) {
        logger.error("[Gateway] CIRCUIT BREAKER ABERTO - disciplina-service indisponivel. Erro: {}", t.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"erro\": \"Servico de disciplina temporariamente indisponivel. Tente novamente em alguns instantes.\", \"servico\": \"disciplina-service\", \"status\": \"CIRCUIT_BREAKER_OPEN\"}");
    }
}
