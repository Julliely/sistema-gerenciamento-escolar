package com.escola.disciplinaservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.escola.disciplinaservice.client.CadastroClient;
import com.escola.disciplinaservice.model.Disciplina;
import com.escola.disciplinaservice.model.Periodo;
import com.escola.disciplinaservice.repository.DisciplinaRepository;
import com.escola.disciplinaservice.repository.PeriodoRepository;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/disciplinas")
public class DisciplinaController {
    private final DisciplinaRepository repository;
    private final PeriodoRepository periodoRepository;
    private final CadastroClient cadastroClient;

    public DisciplinaController(DisciplinaRepository repository, PeriodoRepository periodoRepository, CadastroClient cadastroClient) {
        this.repository = repository;
        this.periodoRepository = periodoRepository;
        this.cadastroClient = cadastroClient;
    }

    @PostMapping
    public ResponseEntity<?> cadastrar(@Valid @RequestBody Disciplina disciplina) {
        if (disciplina.getProfessorId() != null && !cadastroClient.professorExiste(disciplina.getProfessorId())) {
            return ResponseEntity.badRequest().body("Professor não encontrado.");
        }
        if (disciplina.getPeriodo() != null && disciplina.getPeriodo().getId() != null) {
            Periodo periodo = periodoRepository.getReferenceById(disciplina.getPeriodo().getId());
            disciplina.setPeriodo(periodo);
        }
        return ResponseEntity.ok(repository.save(disciplina));
    }

    @GetMapping
    public List<Disciplina> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Disciplina> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @Valid @RequestBody Disciplina disciplina) {
        return repository.findById(id).map(existente -> {
            if (disciplina.getProfessorId() != null && !cadastroClient.professorExiste(disciplina.getProfessorId())) {
                return ResponseEntity.badRequest().body("Professor não encontrado.");
            }
            if (disciplina.getPeriodo() != null && disciplina.getPeriodo().getId() != null) {
                disciplina.setPeriodo(periodoRepository.getReferenceById(disciplina.getPeriodo().getId()));
            }
            disciplina.setId(id);
            return ResponseEntity.ok(repository.save(disciplina));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}