-- Script para POPULAR o banco com dados de teste
-- Execute este script DEPOIS que TODOS os serviços estiverem rodando
-- (cadastro-service, disciplina-service, matricula-service)

-- Inserir Períodos (disciplina-service)
INSERT INTO periodo (numero) VALUES (1);
INSERT INTO periodo (numero) VALUES (2);

-- Inserir Alunos com CPF
INSERT INTO aluno (nome, email, cpf) VALUES 
('João Silva', 'joao.silva@escola.com', '123.456.789-10'),
('Maria Santos', 'maria.santos@escola.com', '987.654.321-00'),
('Pedro Oliveira', 'pedro.oliveira@escola.com', '456.789.123-45'),
('Ana Costa', 'ana.costa@escola.com', '789.123.456-78'),
('Lucas Ferreira', 'lucas.ferreira@escola.com', '234.567.890-12');

-- Inserir Professores com CPF
INSERT INTO professor (nome, email, especialidade, cpf) VALUES 
('Dr. Carlos Mendes', 'carlos.mendes@escola.com', 'Matemática', '111.222.333-44'),
('Profa. Juliana Rocha', 'juliana.rocha@escola.com', 'Português', '222.333.444-55'),
('Prof. Roberto Alves', 'roberto.alves@escola.com', 'Física', '333.444.555-66'),
('Profa. Fernanda Torres', 'fernanda.torres@escola.com', 'Química', '444.555.666-77'),
('Prof. Marcelo Costa', 'marcelo.costa@escola.com', 'História', '555.666.777-88');

-- Inserir Disciplinas
INSERT INTO disciplina (nome, descricao, periodo_id, professor_id) VALUES 
('Algoritmos e Programação', 'Estudo de algoritmos fundamentais', 1, 1),
('Cálculo I', 'Fundamentos de cálculo diferencial', 1, 1),
('Estrutura de Dados', 'Estruturas de dados avançadas', 2, 1),
('Literatura Brasileira', 'Literatura brasileira clássica', 1, 2),
('Gramática Avançada', 'Gramática portuguesa avançada', 2, 2),
('Mecânica Clássica', 'Leis de Newton e movimento', 1, 3),
('Termodinâmica', 'Primeira e segunda lei da termodinâmica', 2, 3),
('Reações Químicas', 'Tipos de reações químicas', 1, 4),
('Química Orgânica', 'Estrutura e propriedades de compostos orgânicos', 2, 4),
('História do Brasil', 'História política e social do Brasil', 1, 5),
('História Moderna', 'História da modernidade europeia', 2, 5);

-- Inserir Matrículas (alunos em disciplinas)
INSERT INTO matricula (aluno_id, disciplina_id, data_matricula, status) VALUES 
(1, 1, NOW(), 'ATIVA'),
(1, 2, NOW(), 'ATIVA'),
(1, 4, NOW(), 'ATIVA'),
(2, 1, NOW(), 'ATIVA'),
(2, 6, NOW(), 'ATIVA'),
(3, 1, NOW(), 'ATIVA'),
(3, 8, NOW(), 'ATIVA'),
(4, 4, NOW(), 'ATIVA'),
(4, 6, NOW(), 'ATIVA'),
(5, 2, NOW(), 'ATIVA'),
(5, 10, NOW(), 'ATIVA');


