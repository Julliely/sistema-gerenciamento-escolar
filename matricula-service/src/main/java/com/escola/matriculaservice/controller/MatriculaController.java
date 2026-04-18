package com.escola.matriculaservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.escola.matriculaservice.client.CadastroClient;
import com.escola.matriculaservice.client.DisciplinaClient;
import com.escola.matriculaservice.model.Matricula;
import com.escola.matriculaservice.model.Matricula.StatusMatricula;
import com.escola.matriculaservice.repository.MatriculaRepository;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/matriculas")
public class MatriculaController {

    @Autowired
    private MatriculaRepository repository;

    @Autowired
    private CadastroClient cadastroClient;

    @Autowired
    private DisciplinaClient disciplinaClient;

    @PostMapping
    public ResponseEntity<?> matricular(@Valid @RequestBody Matricula matricula) {
        if (!cadastroClient.alunoExiste(matricula.getAlunoId()))
            return ResponseEntity.badRequest().body("Aluno não encontrado.");

        if (!disciplinaClient.disciplinaExiste(matricula.getDisciplinaId()))
            return ResponseEntity.badRequest().body("Disciplina não encontrada.");

        matricula.setDataMatricula(LocalDate.now());
        matricula.setStatus(StatusMatricula.ATIVA);
        return ResponseEntity.ok(repository.save(matricula));
    }

    @GetMapping
    public List<Matricula> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Matricula> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/aluno/{alunoId}")
    public List<Matricula> listarPorAluno(@PathVariable Long alunoId) {
        return repository.findByAlunoId(alunoId);
    }

    @GetMapping("/disciplina/{disciplinaId}")
    public List<Matricula> listarPorDisciplina(@PathVariable Long disciplinaId) {
        return repository.findByDisciplinaId(disciplinaId);
    }

    @GetMapping("/disciplina/{disciplinaId}/ativas")
    public boolean temMatriculasAtivas(@PathVariable Long disciplinaId) {
        return repository.findByDisciplinaId(disciplinaId).stream()
                .anyMatch(m -> m.getStatus() == Matricula.StatusMatricula.ATIVA);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Matricula> atualizarStatus(@PathVariable Long id, @RequestParam String status) {
        return repository.findById(id).map(matricula -> {
            matricula.setStatus(StatusMatricula.valueOf(status));
            return ResponseEntity.ok(repository.save(matricula));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        return repository.findById(id).map(matricula -> {
            matricula.setStatus(StatusMatricula.CANCELADA);
            repository.save(matricula);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/finalizar-semestre")
    public ResponseEntity<?> finalizarSemestre(@RequestParam Long alunoId, @RequestBody List<Long> disciplinaIds) {
        List<Matricula> matriculas = repository.findByAlunoId(alunoId).stream()
                .filter(m -> m.getStatus() == StatusMatricula.ATIVA && disciplinaIds.contains(m.getDisciplinaId()))
                .toList();

        if (matriculas.isEmpty()) {
            return ResponseEntity.badRequest().body("Nenhuma matrícula ativa encontrada para finalizar.");
        }

        matriculas.forEach(m -> m.setStatus(StatusMatricula.APROVADO));
        repository.saveAll(matriculas);

        return ResponseEntity.ok("Semestre finalizado com sucesso.");
    }
}
