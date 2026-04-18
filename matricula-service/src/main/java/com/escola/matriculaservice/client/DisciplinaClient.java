package com.escola.matriculaservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class DisciplinaClient {

    private final RestTemplate restTemplate;
    private final String disciplinaServiceUrl;

    public DisciplinaClient(RestTemplate restTemplate,
                            @Value("${disciplina-service.url}") String disciplinaServiceUrl) {
        this.restTemplate = restTemplate;
        this.disciplinaServiceUrl = disciplinaServiceUrl;
    }

    public boolean disciplinaExiste(Long disciplinaId) {
        try {
            restTemplate.getForObject(disciplinaServiceUrl + "/disciplinas/" + disciplinaId, Object.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }
}