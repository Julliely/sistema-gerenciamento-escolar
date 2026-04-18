package com.escola.cadastroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.escola.cadastroservice.model.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {
}
