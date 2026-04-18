package com.escola.matriculaservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class CadastroClient {

    private final RestTemplate restTemplate;
    private final String cadastroServiceUrl;

    public CadastroClient(RestTemplate restTemplate,
                          @Value("${cadastro-service.url}") String cadastroServiceUrl) {
        this.restTemplate = restTemplate;
        this.cadastroServiceUrl = cadastroServiceUrl;
    }

    public boolean alunoExiste(Long alunoId) {
        try {
            restTemplate.getForObject(cadastroServiceUrl + "/alunos/" + alunoId, Object.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }
}
