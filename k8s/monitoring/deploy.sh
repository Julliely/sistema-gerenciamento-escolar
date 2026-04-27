#!/bin/bash
##############################################
# Script de deploy do Stack de Observabilidade
# Uso: ./deploy.sh [apply|delete]
##############################################

set -e

ACTION="${1:-apply}"

echo "============================================"
echo " Observabilidade - Prometheus + Grafana"
echo " Acao: $ACTION"
echo "============================================"

if [ "$ACTION" = "apply" ]; then
    echo ""
    echo "[1/5] Criando namespace monitoring..."
    kubectl apply -f 00-namespace.yaml

    echo "[2/5] Criando configuracao do Prometheus..."
    kubectl apply -f 01-prometheus-config.yaml

    echo "[3/5] Deploy do Prometheus..."
    kubectl apply -f 02-prometheus.yaml
    echo "  Aguardando Prometheus ficar pronto..."
    kubectl wait --namespace=monitoring --for=condition=ready pod -l app=prometheus --timeout=120s

    echo "[4/5] Deploy do Grafana..."
    kubectl apply -f 03-grafana-config.yaml
    kubectl apply -f 04-grafana.yaml
    echo "  Aguardando Grafana ficar pronto..."
    kubectl wait --namespace=monitoring --for=condition=ready pod -l app=grafana --timeout=120s

    echo "[5/5] Configurando Ingress..."
    kubectl apply -f 05-ingress.yaml

    echo ""
    echo "============================================"
    echo " Deploy concluido!"
    echo "============================================"
    echo ""
    echo "Acesso (adicione ao /etc/hosts):"
    echo "  127.0.0.1 grafana.escola.local"
    echo "  127.0.0.1 prometheus.escola.local"
    echo ""
    echo "Grafana:    http://grafana.escola.local"
    echo "  Usuario:  admin"
    echo "  Senha:    admin123"
    echo ""
    echo "Prometheus: http://prometheus.escola.local"
    echo ""
    echo "Verificar status:"
    echo "  kubectl get all -n monitoring"
    echo ""

elif [ "$ACTION" = "delete" ]; then
    echo ""
    echo "Removendo stack de observabilidade..."
    kubectl delete -f 05-ingress.yaml --ignore-not-found
    kubectl delete -f 04-grafana.yaml --ignore-not-found
    kubectl delete -f 03-grafana-config.yaml --ignore-not-found
    kubectl delete -f 02-prometheus.yaml --ignore-not-found
    kubectl delete -f 01-prometheus-config.yaml --ignore-not-found
    kubectl delete -f 00-namespace.yaml --ignore-not-found
    echo "Stack removido!"

else
    echo "Uso: $0 [apply|delete]"
    exit 1
fi
