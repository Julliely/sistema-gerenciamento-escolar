package com.escola.disciplinaservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.escola.disciplinaservice.model.Periodo;
import com.escola.disciplinaservice.repository.PeriodoRepository;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/periodos")
public class PeriodoController {

    @Autowired
    private PeriodoRepository repository;

    @PostMapping
    public Periodo cadastrar(@Valid @RequestBody Periodo periodo) {
        return repository.save(periodo);
    }

    @GetMapping
    public List<Periodo> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Periodo> buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Periodo> atualizar(@PathVariable Long id, @Valid @RequestBody Periodo periodo) {
        return repository.findById(id).map(existente -> {
            periodo.setId(id);
            return ResponseEntity.ok(repository.save(periodo));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}