package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.Collections;

/**
 * Stack que define o banco de dados RDS (Relational Database Service).
 * <p>
 * O que é RDS?
 * - Serviço gerenciado de banco de dados da AWS
 * - AWS cuida de backups, patches, replicação
 * - Suporta MySQL, PostgreSQL, Oracle, etc.
 * <p>
 * Segurança:
 * - O banco fica em subnets PRIVADAS (não acessível diretamente da internet)
 * - Apenas recursos dentro da VPC (como o ECS) podem conectar
 * - Security Group controla quem pode acessar a porta 3306
 * <p>
 * CfnParameter: Permite passar a senha no momento do deploy (cdk deploy -c ou via prompt)
 * CfnOutput + exportName: Exporta valores para outros stacks usarem (Fn.importValue)
 */
public class AluraRdsStack extends Stack {

    public AluraRdsStack(final Construct scope, final String id, final Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public AluraRdsStack(final Construct scope, final String id, final StackProps props, final Vpc vpc) {
        super(scope, id, props);

        // Parâmetro pedido no deploy - evita senha no código
        CfnParameter senha = CfnParameter.Builder.create(this, "senha")
                .type("String")
                .description("Senha do database pedidos-ms")
                .build();

        // Security Group: "firewall" que controla tráfego de rede
        // Usa o default da VPC e adiciona regra para permitir conexões MySQL (porta 3306)
        ISecurityGroup iSecurityGroup = SecurityGroup.fromSecurityGroupId(
                this, id, vpc.getVpcDefaultSecurityGroup());
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306)); // Permite de qualquer IP na VPC

        // Cria a instância do banco MySQL
        DatabaseInstance database = DatabaseInstance.Builder
                .create(this, "Rds-pedidos")
                .instanceIdentifier("alura-aws-pedido-db")  // Nome no console AWS
                .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder()
                        .version(MysqlEngineVersion.VER_8_0)
                        .build()))
                .vpc(vpc)
                .credentials(Credentials.fromUsername("admin",
                        CredentialsFromUsernameOptions.builder()
                                .password(SecretValue.unsafePlainText(senha.getValueAsString()))
                                .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))  // t3.micro - menor custo
                .multiAz(false)       // false = uma só AZ (mais barato). true = réplica em outra AZ (alta disponibilidade)
                .allocatedStorage(10) // 10 GB de armazenamento
                .securityGroups(Collections.singletonList(iSecurityGroup))
                // IMPORTANTE: subnets privadas - banco não fica exposto à internet
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(vpc.getPrivateSubnets())
                        .build())
                .build();

        // Exporta o endpoint para o AluraServiceStack poder conectar (Fn.importValue)
        CfnOutput.Builder.create(this, "pedidos-db-endpoint")
                .exportName("pedidos-db-endpoint")
                .value(database.getDbInstanceEndpointAddress())
                .build();

        // Exporta a senha - usado pelo serviço para conectar ao banco
        CfnOutput.Builder.create(this, "pedidos-db-senha")
                .exportName("pedidos-db-senha")
                .value(senha.getValueAsString())
                .build();

    }

}
