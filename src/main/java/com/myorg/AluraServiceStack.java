package com.myorg;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.CpuUtilizationScalingProps;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.MemoryUtilizationScalingProps;
import software.amazon.awscdk.services.ecs.ScalableTaskCount;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

/**
 * Stack que faz o deploy da aplicação como container no ECS Fargate.
 * O que esse stack cria (em um único comando!):
 * - Serviço ECS Fargate: roda seus containers sem gerenciar servidores
 * - Application Load Balancer (ALB): distribui tráfego entre as instâncias
 * - ECR: referência à imagem Docker (deve existir previamente)
 * - CloudWatch Logs: armazena logs da aplicação
 * - Auto Scaling: escala automaticamente baseado em CPU e memória
 * Fluxo: Internet -> ALB -> Container(s) Fargate -> RDS (banco)
 * Fn.importValue: Importa valores exportados por outros stacks (RDS) - cross-stack reference
 */
public class AluraServiceStack extends Stack {

    public AluraServiceStack(final Construct scope, final String id, final Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public AluraServiceStack(final Construct scope, final String id, final StackProps props, final Cluster cluster) {
        super(scope, id, props);

        // Variáveis de ambiente para a aplicação Spring Boot conectar ao MySQL
        // Fn.importValue busca valores exportados pelo AluraRdsStack (CfnOutput com exportName)
        Map<String, String> autenticacao = new HashMap<>();
        autenticacao.put("SPRING_DATASOURCE_URL", "jdbc:mysql://" +
                Fn.importValue("pedidos-db-endpoint") +
                ":3306/alurafood-pedidos?createDatabaseIfNotExist=true");
        autenticacao.put("SPRING_DATASOURCE_USERNAME", "admin");
        autenticacao.put("SPRING_DATASOURCE_PASSWORD", Fn.importValue("pedidos-db-senha"));

        // ECR = Elastic Container Registry - repositório de imagens Docker (como Docker Hub da AWS)
        // O repositório "img-pedidos-ms" deve existir na sua conta AWS
        IRepository iRepository = Repository.fromRepositoryName(
                this, "repositorio", "img-pedidos-ms");

        // ApplicationLoadBalancedFargateService é um "pattern" do CDK que cria várias coisas de uma vez:
        // - Task Definition (definição do container)
        // - Fargate Service (roda as tasks)
        // - Application Load Balancer (ALB)
        // - Target Group (grupo de destinos para o ALB)
        ApplicationLoadBalancedFargateService aluraService = ApplicationLoadBalancedFargateService.Builder.create(this, "AluraService")
                .serviceName("alura-service-ola")
                .cluster(cluster)           // Cluster ECS onde o serviço rodará
                .cpu(512)                   // 0.5 vCPU por container
                .desiredCount(1)            // Número inicial de containers rodando
                .listenerPort(8080)         // Porta que o ALB escuta (entrada)
                .assignPublicIp(true)       // Containers precisam de IP público para pull da imagem ECR
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                // Usa imagem do ECR (Docker Hub da AWS)
                                .image(ContainerImage.fromEcrRepository(iRepository))
                                .containerPort(8080)     // Porta interna do container (Spring Boot)
                                .containerName("app_ola")
                                .environment(autenticacao) // Env vars para o Spring conectar ao MySQL
                                // CloudWatch Logs: logs da aplicação vão para a AWS
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                        .logGroup(LogGroup.Builder.create(this, "PedidosMsLogGroup")
                                                .logGroupName("PedidosMsLog")
                                                .removalPolicy(RemovalPolicy.DESTROY) // Apaga logs ao deletar stack
                                                .build())
                                        .streamPrefix("PedidosMS")  // Prefixo nos nomes dos log streams
                                        .build()))
                                .build())
                .memoryLimitMiB(1024)       // 1 GB de RAM por container
                .publicLoadBalancer(true)   // ALB com IP público (acessível da internet)
                .build();

        // AUTO SCALING: ajusta o número de containers automaticamente
        // Application Auto Scaling - escala baseado em métricas (CPU, memória, etc.)
        ScalableTaskCount scalableTarget = aluraService.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(1)   // Mínimo 1 container sempre rodando
                .maxCapacity(3)   // Máximo 3 containers sob carga
                .build());
        // Escala quando CPU passa de 70%
        scalableTarget.scaleOnCpuUtilization("CpuScaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(70)
                .scaleInCooldown(Duration.minutes(3))   // Espera antes de reduzir (evita oscilação)
                .scaleOutCooldown(Duration.minutes(2))  // Espera antes de aumentar
                .build());
        // Escala quando memória passa de 65%
        scalableTarget.scaleOnMemoryUtilization("MemoryScaling", MemoryUtilizationScalingProps.builder()
                .targetUtilizationPercent(65)
                .scaleInCooldown(Duration.minutes(3))
                .scaleOutCooldown(Duration.minutes(2))
                .build());

    }
    
}
