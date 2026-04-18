const DisciplinaService = {
    listar: () => fetch(`${API.DISCIPLINA}/disciplinas`).then(r => r.json()),
    buscar: (id) => fetch(`${API.DISCIPLINA}/disciplinas/${id}`).then(r => r.json()),
    cadastrar: (disciplina) => fetch(`${API.DISCIPLINA}/disciplinas`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(disciplina)
    }).then(r => r.json()),
    atualizar: (id, disciplina) => fetch(`${API.DISCIPLINA}/disciplinas/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(disciplina)
    }).then(r => r.json()),
    deletar: (id) => fetch(`${API.DISCIPLINA}/disciplinas/${id}`, { method: 'DELETE' })
};
