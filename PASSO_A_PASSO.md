# Passo a Passo - Deploy Completo na AWS

Este guia explica tudo que você precisa fazer para colocar a aplicação no ar.

---

## Pré-requisitos

Antes de começar, certifique-se de ter:

| Requisito | Como verificar |
|-----------|----------------|
| **Conta AWS** | Acesse [aws.amazon.com](https://aws.amazon.com) |
| **Java 17** | `java -version` |
| **Maven** | `mvn -version` |
| **Node.js 18+** (para CDK CLI) | `node -v` |
| **Docker** (para build da imagem) | `docker -version` |
| **Git** | `git --version` |

---

## Etapa 1: Configurar a AWS CLI

1. Instale a AWS CLI: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html

2. Configure suas credenciais:
   ```bash
   aws configure
   ```
   - **AWS Access Key ID**: Sua chave de acesso (console AWS → IAM → Usuários → Sua conta → Segurança → Chaves de acesso)
   - **AWS Secret Access Key**: Sua chave secreta
   - **Region**: `sa-east-1` (São Paulo) ou a região que preferir

3. Teste a conexão:
   ```bash
   aws sts get-caller-identity
   ```

---

## Etapa 2: Instalar o CDK

```bash
npm install -g aws-cdk
```

Verifique a instalação:
```bash
cdk --version
```

---

## Etapa 3: Configurar o ambiente CDK (primeira vez na conta/região)

Execute **uma única vez** por conta AWS e região:

```bash
cdk bootstrap
```

Isso cria um bucket S3 e roles IAM que o CDK usa para fazer deploy.

---

## Etapa 4: Preparar a imagem Docker

O serviço usa uma imagem do repositório ECR chamado `img-pedidos-ms`. Você precisa:

### 4.1 Criar o repositório ECR

```bash
aws ecr create-repository --repository-name img-pedidos-ms
```

Anote o **URI** retornado (algo como `123456789.dkr.ecr.sa-east-1.amazonaws.com/img-pedidos-ms`).

### 4.2 Fazer login no ECR

```bash
aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin SEU_ACCOUNT_ID.dkr.ecr.sa-east-1.amazonaws.com
```

Substitua `SEU_ACCOUNT_ID` pelo ID da sua conta AWS (o número que aparece no `aws sts get-caller-identity`).

### 4.3 Build e push da imagem

Se você tem o projeto da aplicação `pedidos-ms` (Spring Boot):

```bash
# Na pasta do projeto pedidos-ms
docker build -t img-pedidos-ms .
docker tag img-pedidos-ms:latest SEU_ACCOUNT_ID.dkr.ecr.sa-east-1.amazonaws.com/img-pedidos-ms:latest
docker push SEU_ACCOUNT_ID.dkr.ecr.sa-east-1.amazonaws.com/img-pedidos-ms:latest
```

> **Nota:** A aplicação deve ser um Spring Boot que roda na porta 8080 e usa as variáveis `SPRING_DATASOURCE_*` para conectar ao MySQL.

---

## Etapa 5: Deploy da infraestrutura

### 5.1 Navegue até a pasta do projeto

```bash
cd c:\Users\scomp\OneDrive\Documents\ProjetosTI\matheus-vsm\matheus-vsm\alura-aws-infra
```

### 5.2 Compile o projeto

```bash
mvn clean compile
```

### 5.3 Verifique os stacks

```bash
cdk ls
```

Deve listar: `Vpc`, `Cluster`, `Rds`, `Service`

### 5.4 Faça o deploy (ordem recomendada)

**Opção A - Deploy de todos de uma vez:**

```bash
cdk deploy --all --require-approval never
```

Quando o RDS for implantado, o CDK vai pedir a **senha do banco**. Digite uma senha forte e guarde-a! Você precisará dela se precisar reconectar ao banco.

**Opção B - Deploy um por um (mais controle):**

```bash
# 1. VPC (rede)
cdk deploy Vpc --require-approval never

# 2. Cluster e RDS (podem ser em paralelo)
cdk deploy Cluster --require-approval never
cdk deploy Rds --require-approval never
# No RDS, será solicitada a senha do banco

# 3. Service (aplicação) - por último, pois depende do RDS
cdk deploy Service --require-approval never
```

> **Dica:** Use `cdk diff` antes de cada deploy para ver o que será alterado.

---

## Etapa 6: Acessar a aplicação

Após o deploy do **Service**, o CDK exibe as saídas (Outputs). Procure por:

- **LoadBalancerDNS** ou **ALB DNS**
- Ou acesse o Console AWS → EC2 → Load Balancers → encontre o ALB do `Service` → copie o DNS

A URL será algo como:
```
http://Service-LoadB-XXXXXXXX.sa-east-1.elb.amazonaws.com
```

A aplicação responde na porta 8080, mas o Load Balancer já encaminha para ela. Teste:

```
http://SEU-ALB-DNS/
```

---

## Etapa 7: Verificar se está funcionando

### Logs da aplicação
```bash
# No Console AWS: CloudWatch → Log groups → PedidosMsLog
```

### Status dos containers
```bash
# Console AWS: ECS → Clusters → cluster-alura → Services → alura-service-ola
```

### Banco de dados
```bash
# Console AWS: RDS → Databases → alura-aws-pedido-db
```

---

## Comandos úteis

| Comando | O que faz |
|---------|-----------|
| `cdk diff` | Mostra diferenças entre código e o que está na AWS |
| `cdk deploy StackName` | Faz deploy de um stack específico |
| `cdk destroy StackName` | **Remove** um stack (cuidado: apaga os recursos!) |
| `cdk synth` | Gera os arquivos CloudFormation sem fazer deploy |

---

## Troubleshooting

### "Export X does not exist"
O stack **Service** importa valores do **RDS**. Faça o deploy do RDS antes do Service.

### Erro ao fazer pull da imagem no ECS
- Verifique se a imagem existe no ECR: `aws ecr describe-images --repository-name img-pedidos-ms`
- Confirme que o ECS tem permissão para acessar o ECR (o CDK já configura isso)

### Aplicação não conecta no banco
- O RDS está em subnets privadas; a aplicação (Fargate) precisa estar na mesma VPC
- Verifique as variáveis de ambiente no ECS (SPRING_DATASOURCE_*)
- O Security Group do RDS permite conexão na porta 3306?

### Região diferente
Se usar outra região (ex: `us-east-1`), troque em todos os comandos e no `aws configure`.

---

## Resumo do fluxo

```
1. aws configure          → Configura credenciais
2. cdk bootstrap          → Prepara conta AWS (1x)
3. Criar ECR + push img   → Imagem Docker disponível
4. cdk deploy --all       → Cria VPC, Cluster, RDS, Service
5. Acessar URL do ALB     → Aplicação no ar!
```
