package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.Cluster;
import software.constructs.Construct;

/**
 * Stack que define o Cluster ECS (Elastic Container Service).
 * <p>
 * O que é ECS?
 * - Serviço de orquestração de containers da AWS (similar ao Kubernetes)
 * - Gerencia o ciclo de vida dos containers Docker
 * - Pode rodar em EC2 (você gerencia as máquinas) ou Fargate (serverless - AWS gerencia)
 * <p>
 * O que é um Cluster?
 * - É um agrupamento lógico onde suas tarefas (tasks) e serviços rodam
 * - Pode ser pensado como o "ambiente" onde os containers serão executados
 * - Neste projeto usamos Fargate, então não há instâncias EC2 - a AWS provisiona automaticamente
 * <p>
 * Fargate = serverless para containers - você só paga pelo que usar, sem gerenciar servidores
 */
public class AluraClusterStack extends Stack {

    private Cluster cluster;

    public AluraClusterStack(final Construct scope, final String id, final Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public AluraClusterStack(final Construct scope, final String id, final StackProps props, final Vpc vpc) {
        super(scope, id, props);

        // Cria o cluster ECS dentro da VPC
        cluster = Cluster.Builder.create(this, "AluraCluster")
                .clusterName("cluster-alura")  // Nome visível no console AWS
                .vpc(vpc)                      // O cluster usa a rede da VPC criada no AluraVpcStack
                .build();

    }

    public Cluster getCluster() {
        return cluster;
    }

}
