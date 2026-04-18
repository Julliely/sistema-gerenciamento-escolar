package com.escola.disciplinaservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.escola.disciplinaservice.model.Periodo;

public interface PeriodoRepository extends JpaRepository<Periodo, Long> {
}