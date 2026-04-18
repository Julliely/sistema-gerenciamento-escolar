package com.escola.disciplinaservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Entity
public class Periodo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Número é obrigatório")
    @Min(value = 1, message = "Número deve ser maior que 0")
    private Integer numero;
}