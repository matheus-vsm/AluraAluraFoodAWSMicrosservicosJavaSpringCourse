package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;

/**
 * Stack que define a VPC (Virtual Private Cloud) - a rede isolada na AWS.
 * <p>
 * O que é uma VPC?
 * - É como uma rede privada dedicada à sua conta AWS
 * - Isola seus recursos dos outros clientes da AWS
 * - Similar a uma rede física em um datacenter, mas virtualizada
 * <p>
 * Por padrão, o CDK cria automaticamente:
 * - Subnets públicas e privadas em cada AZ
 * - Internet Gateway (para subnets públicas acessarem a internet)
 * - NAT Gateways (para subnets privadas acessarem internet de forma segura)
 * - Tabelas de roteamento
 * <p>
 * AZ = Availability Zone - datacenter físico separado para alta disponibilidade
 */
public class AluraVpcStack extends Stack {

    private Vpc vpc;

    public AluraVpcStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AluraVpcStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Cria a VPC com configuração padrão do CDK
        vpc = Vpc.Builder.create(this, "AluraVpc")
                // maxAzs: número de Availability Zones a usar (máx 3 para custo controlado)
                // Cada AZ terá subnets públicas e privadas criadas automaticamente
                .maxAzs(3)
                .build();

    }

    public Vpc getVpc() {
        return vpc;
    }

}
