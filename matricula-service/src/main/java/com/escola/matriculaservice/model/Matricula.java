package com.escola.matriculaservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "ID do aluno é obrigatório")
    private Long alunoId;
    
    @NotNull(message = "ID da disciplina é obrigatório")
    private Long disciplinaId;

    private LocalDate dataMatricula;

    @Enumerated(EnumType.STRING)
    private StatusMatricula status;

    public enum StatusMatricula {
        ATIVA, CANCELADA, APROVADO, REPROVADO
    }
}
