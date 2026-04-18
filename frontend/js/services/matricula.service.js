const MatriculaService = {
    listar: () => fetch(`${API.MATRICULA}/matriculas`).then(r => r.json()),
    buscar: (id) => fetch(`${API.MATRICULA}/matriculas/${id}`).then(r => r.json()),
    listarPorAluno: (alunoId) => fetch(`${API.MATRICULA}/matriculas/aluno/${alunoId}`).then(r => r.json()),
    listarPorDisciplina: (disciplinaId) => fetch(`${API.MATRICULA}/matriculas/disciplina/${disciplinaId}`).then(r => r.json()),
    matricular: (matricula) => fetch(`${API.MATRICULA}/matriculas`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(matricula)
    }).then(r => r.json()),
    cancelar: (id) => fetch(`${API.MATRICULA}/matriculas/${id}`, { method: 'DELETE' }),
    atualizarStatus: (id, status) => fetch(`${API.MATRICULA}/matriculas/${id}/status?status=${status}`, { method: 'PATCH' }),
    finalizarSemestre: (alunoId, disciplinaIds) => fetch(`${API.MATRICULA}/matriculas/finalizar-semestre?alunoId=${alunoId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(disciplinaIds)
    })
};
