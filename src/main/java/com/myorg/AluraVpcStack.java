package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.constructs.Construct;
//arn:aws:cloudformation:sa-east-1:128955061068:stack/Vpc/7ae95d70-0305-11f1-bcb9-0234156f3a1f
public class AluraVpcStack extends Stack {

    private Vpc vpc;

    public AluraVpcStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AluraVpcStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        vpc = Vpc.Builder.create(this, "AluraVpc")
                .maxAzs(3)  // Default is all AZs in region
                .build();

    }

    public Vpc getVpc() {
        return vpc;
    }

}
