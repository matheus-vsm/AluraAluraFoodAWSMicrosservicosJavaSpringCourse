package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

/**
 * Stack template padrão criado pelo "cdk init".
 * <p>
 * Este stack NÃO é utilizado no projeto atual - a aplicação usa os stacks:
 * AluraVpcStack, AluraClusterStack, AluraRdsStack e AluraServiceStack.
 * <p>
 * Pode ser mantido como exemplo/referência ou removido.
 * Um Stack no CDK é uma unidade de deploy - um conjunto de recursos AWS
 * que são criados/atualizados/deletados juntos.
 */
public class AluraAwsInfraStack extends Stack {
    public AluraAwsInfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AluraAwsInfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // The code that defines your stack goes here

        // example resource
        // final Queue queue = Queue.Builder.create(this, "AluraAwsInfraQueue")
        //         .visibilityTimeout(Duration.seconds(300))
        //         .build();
    }
}
