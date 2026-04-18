package com.escola.cadastroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.escola.cadastroservice.model.Professor;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
}
