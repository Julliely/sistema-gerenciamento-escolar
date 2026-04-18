package com.escola.cadastroservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.escola.cadastroservice.model.Professor;
import com.escola.cadastroservice.repository.ProfessorRepository;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/professores")
public class ProfessorController {
    private final ProfessorRepository repository;

    // Injeção de dependência por construtor
    public ProfessorController(ProfessorRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public Professor cadastrar(@Valid @RequestBody Professor professor) {
        return repository.save(professor);
    }

    @GetMapping
    public List<Professor> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Professor> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Professor> atualizar(@PathVariable Long id, @Valid @RequestBody Professor professor) {
        return repository.findById(id).map(existente -> {
            professor.setId(id);
            return ResponseEntity.ok(repository.save(professor));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
