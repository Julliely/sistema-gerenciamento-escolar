#!/bin/bash
##############################################
# Script de deploy do Sistema Escolar no Kubernetes
# Uso: ./deploy.sh [apply|delete]
##############################################

set -e

NAMESPACE="escola-system"
ACTION="${1:-apply}"

echo "============================================"
echo " Sistema de Gerenciamento Escolar - K8s"
echo " Acao: $ACTION"
echo "============================================"

if [ "$ACTION" = "apply" ]; then
    echo ""
    echo "[1/7] Criando namespace..."
    kubectl apply -f 00-namespace.yaml

    echo "[2/7] Criando ConfigMaps e Secrets..."
    kubectl apply -f 01-configmap.yaml
    kubectl apply -f 01-secrets.yaml

    echo "[3/7] Deploy do PostgreSQL..."
    kubectl apply -f 02-postgresql.yaml
    echo "  Aguardando PostgreSQL ficar pronto..."
    kubectl wait --namespace=$NAMESPACE --for=condition=ready pod -l app=postgresql --timeout=120s

    echo "[4/7] Deploy dos microsservicos..."
    kubectl apply -f 03-cadastro-service.yaml
    kubectl apply -f 04-disciplina-service.yaml
    kubectl apply -f 05-matricula-service.yaml
    kubectl apply -f 06-gateway-service.yaml

    echo "[5/7] Deploy do frontend..."
    kubectl apply -f 07-frontend.yaml

    echo "[6/7] Configurando Ingress..."
    kubectl apply -f 08-ingress.yaml

    echo "[7/7] Configurando HPA, PDB e NetworkPolicies..."
    kubectl apply -f 09-hpa.yaml
    kubectl apply -f 10-pdb.yaml
    kubectl apply -f 11-network-policies.yaml

    echo ""
    echo "============================================"
    echo " Deploy concluido!"
    echo "============================================"
    echo ""
    echo "Verificar status:"
    echo "  kubectl get all -n $NAMESPACE"
    echo ""
    echo "Verificar health dos pods:"
    echo "  kubectl get pods -n $NAMESPACE -o wide"
    echo ""
    echo "Verificar logs de um servico:"
    echo "  kubectl logs -n $NAMESPACE -l app=cadastro-service -f"
    echo ""
    echo "Acessar o sistema:"
    echo "  Adicione '127.0.0.1 escola.local' ao /etc/hosts"
    echo "  Acesse: http://escola.local"
    echo ""

elif [ "$ACTION" = "delete" ]; then
    echo ""
    echo "Removendo todos os recursos..."
    kubectl delete -f 11-network-policies.yaml --ignore-not-found
    kubectl delete -f 10-pdb.yaml --ignore-not-found
    kubectl delete -f 09-hpa.yaml --ignore-not-found
    kubectl delete -f 08-ingress.yaml --ignore-not-found
    kubectl delete -f 07-frontend.yaml --ignore-not-found
    kubectl delete -f 06-gateway-service.yaml --ignore-not-found
    kubectl delete -f 05-matricula-service.yaml --ignore-not-found
    kubectl delete -f 04-disciplina-service.yaml --ignore-not-found
    kubectl delete -f 03-cadastro-service.yaml --ignore-not-found
    kubectl delete -f 02-postgresql.yaml --ignore-not-found
    kubectl delete -f 01-secrets.yaml --ignore-not-found
    kubectl delete -f 01-configmap.yaml --ignore-not-found
    kubectl delete -f 00-namespace.yaml --ignore-not-found

    echo ""
    echo "Todos os recursos removidos!"
    echo ""

else
    echo "Uso: $0 [apply|delete]"
    exit 1
fi
