package com.myorg;

import software.amazon.awscdk.App;

/**
 * Ponto de entrada principal da aplicação AWS CDK.
 * <p>
 * Este projeto usa AWS CDK (Cloud Development Kit) para definir a infraestrutura como código (IaC).
 * O CDK converte esse código Java em templates CloudFormation, que a AWS usa para criar os recursos.
 * <p>
 * Arquitetura geral (ordem de criação):
 * 1. VPC - Rede virtual onde tudo roda
 * 2. Cluster ECS - Orquestrador de containers
 * 3. RDS - Banco de dados MySQL
 * 4. Service - Aplicação em containers (Fargate)
 * <p>
 * Para fazer deploy: cdk deploy (ou cdk deploy --all para todos os stacks)
 * Para ver o que será criado: cdk diff
 */
public class AluraAwsInfraApp {
    public static void main(final String[] args) {
        // App é o objeto raiz do CDK - contém todos os stacks
        App app = new App();

        // 1. VPC - Cria a rede virtual (sub-redes públicas e privadas, NAT Gateways, etc.)
        AluraVpcStack vpcStack = new AluraVpcStack(app, "Vpc");

        // 2. Cluster ECS - Cria o cluster para rodar containers Docker
        AluraClusterStack clusterStack = new AluraClusterStack(app, "Cluster", vpcStack.getVpc());
        clusterStack.addDependency(vpcStack); // Garante que o cluster seja criado APÓS a VPC existir

        // 3. RDS - Cria o banco MySQL para a aplicação
        AluraRdsStack rdsStack = new AluraRdsStack(app, "Rds", vpcStack.getVpc());
        rdsStack.addDependency(vpcStack); // Garante que o RDS seja criado APÓS a VPC existir

        // 4. Service - Deploy da aplicação como container no Fargate com Load Balancer
        AluraServiceStack serviceStack = new AluraServiceStack(app, "Service", clusterStack.getCluster());
        serviceStack.addDependency(clusterStack); // Garante que o serviço seja criado APÓS o Cluster existir
        serviceStack.addDependency(rdsStack);     // Service usa Fn.importValue do RDS - precisa existir antes

        // synth() gera os arquivos CloudFormation na pasta cdk.out/
        app.synth();
    }
}
