# Alura AWS Infra - Projeto CDK

Projeto de infraestrutura como código (IaC) usando AWS CDK para deploy de microsserviços na AWS.

## Arquitetura

O projeto cria 4 stacks (em ordem de dependência):

1. **Vpc** - Rede virtual (VPC) com subnets públicas/privadas em 3 Availability Zones
2. **Cluster** - Cluster ECS Fargate para orquestração de containers
3. **Rds** - Banco de dados MySQL (RDS) em subnets privadas
4. **Service** - Aplicação Spring Boot em container com Load Balancer e Auto Scaling

## Configuração do `cdk.json`

O arquivo `cdk.json` configura como o CDK executa o projeto:

- **app**: Comando para rodar a aplicação (`mvn -e -q compile exec:java`)
- **watch**: Arquivos monitorados para hot-reload durante desenvolvimento
- **context**: Flags de feature do CDK - alteram comportamento padrão das libs (geralmente não é necessário mudar)

## Comandos úteis

 * `mvn package`     - Compila e roda testes
 * `cdk ls`          - Lista todos os stacks
 * `cdk synth`       - Gera os templates CloudFormation (pasta cdk.out/)
 * `cdk deploy`      - Faz deploy dos stacks na AWS (pede confirmação)
 * `cdk deploy --all` - Deploy de todos os stacks
 * `cdk diff`        - Mostra diferenças entre código e o que está na AWS
 * `cdk destroy`     - Remove os recursos da AWS (cuidado!)
 * `cdk docs`        - Abre documentação do CDK

## Pré-requisitos

- AWS CLI configurado (`aws configure`)
- Repositório ECR `img-pedidos-ms` com a imagem Docker da aplicação
- Banco RDS deve ser implantado antes do Service (o deploy pede a senha do RDS)
