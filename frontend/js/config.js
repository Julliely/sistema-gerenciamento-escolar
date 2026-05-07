// Paths relativos: o Ingress do K8s ja roteia /cadastro, /matricula e /disciplina
// para o gateway. Tambem funciona com docker-compose se o gateway estiver na
// mesma origem (caso contrario, usar URLs absolutas como http://localhost:8080/cadastro).
const API = {
    CADASTRO: '/cadastro',
    MATRICULA: '/matricula',
    DISCIPLINA: '/disciplina'
};
