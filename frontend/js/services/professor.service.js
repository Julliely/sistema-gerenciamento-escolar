const ProfessorService = {
    listar: () => fetch(`${API.CADASTRO}/professores`).then(r => r.json()),
    buscar: (id) => fetch(`${API.CADASTRO}/professores/${id}`).then(r => r.json()),
    cadastrar: (professor) => fetch(`${API.CADASTRO}/professores`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(professor)
    }).then(r => r.json()),
    atualizar: (id, professor) => fetch(`${API.CADASTRO}/professores/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(professor)
    }).then(r => r.json()),
    deletar: (id) => fetch(`${API.CADASTRO}/professores/${id}`, { method: 'DELETE' })
};
