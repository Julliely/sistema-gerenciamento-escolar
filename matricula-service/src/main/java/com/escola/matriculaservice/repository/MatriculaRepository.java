package com.escola.matriculaservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.escola.matriculaservice.model.Matricula;
import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    List<Matricula> findByAlunoId(Long alunoId);
    List<Matricula> findByDisciplinaId(Long disciplinaId);
}
