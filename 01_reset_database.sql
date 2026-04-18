-- Script para RESETAR o banco de dados
-- Execute este script antes de iniciar a aplicação

-- Deletar TODAS as tabelas (singulares e plurais)
-- Cada microsserviço gerencia suas próprias tabelas
DROP TABLE IF EXISTS public.matricula CASCADE;
DROP TABLE IF EXISTS public.matriculas CASCADE;
DROP TABLE IF EXISTS public.professor CASCADE;
DROP TABLE IF EXISTS public.professores CASCADE;
DROP TABLE IF EXISTS public.aluno CASCADE;
DROP TABLE IF EXISTS public.alunos CASCADE;
DROP TABLE IF EXISTS public.disciplina_prerequisito CASCADE;
DROP TABLE IF EXISTS public.disciplina CASCADE;
DROP TABLE IF EXISTS public.disciplinas CASCADE;
DROP TABLE IF EXISTS public.periodo CASCADE;

COMMIT;
