package com.escola.disciplinaservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.escola.disciplinaservice.model.Disciplina;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {
}