package com.escola.gateway;

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

    @RequestMapping(value = "/cadastro/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public ResponseEntity<String> proxyCadastro(HttpServletRequest request,
                                                 @RequestBody(required = false) String body) {
        return proxyRequest(cadastroServiceUrl, "/cadastro", request, body);
    }

    @RequestMapping(value = "/matricula/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public ResponseEntity<String> proxyMatricula(HttpServletRequest request,
                                                  @RequestBody(required = false) String body) {
        return proxyRequest(matriculaServiceUrl, "/matricula", request, body);
    }

    @RequestMapping(value = "/disciplina/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public ResponseEntity<String> proxyDisciplina(HttpServletRequest request,
                                                   @RequestBody(required = false) String body) {
        return proxyRequest(disciplinaServiceUrl, "/disciplina", request, body);
    }
}
