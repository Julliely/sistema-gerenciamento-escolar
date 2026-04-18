const PeriodoService = {
    listar: () => fetch(`${API.DISCIPLINA}/periodos`).then(r => r.json())
};
