const AlunoService = {
    listar: () => fetch(`${API.CADASTRO}/alunos`).then(r => r.json()),
    buscar: (id) => fetch(`${API.CADASTRO}/alunos/${id}`).then(r => r.json()),
    cadastrar: (aluno) => fetch(`${API.CADASTRO}/alunos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(aluno)
    }).then(r => r.json()),
    atualizar: (id, aluno) => fetch(`${API.CADASTRO}/alunos/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(aluno)
    }).then(r => r.json()),
    deletar: (id) => fetch(`${API.CADASTRO}/alunos/${id}`, { method: 'DELETE' })
};
