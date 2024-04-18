import {GuPlayApp} from '@guardian/cdk';
import {AccessScope} from '@guardian/cdk/lib/constants/access';
import type {GuStackProps} from '@guardian/cdk/lib/constructs/core';
import {GuDistributionBucketParameter, GuStack, GuStringParameter} from '@guardian/cdk/lib/constructs/core';
import {GuCname} from '@guardian/cdk/lib/constructs/dns';
import {GuPolicy, ReadParametersByName} from '@guardian/cdk/lib/constructs/iam';
import type {App} from 'aws-cdk-lib';
import {Duration} from 'aws-cdk-lib';
import {InstanceClass, InstanceSize, InstanceType, SecurityGroup} from 'aws-cdk-lib/aws-ec2';
import {Effect, PolicyStatement} from 'aws-cdk-lib/aws-iam';
import {ParameterDataType, ParameterTier, StringParameter} from 'aws-cdk-lib/aws-ssm';

export interface GatehouseStackProps extends GuStackProps {
    domainName: string;
}

export class Gatehouse extends GuStack {
    constructor(
        scope: App,
        id: string,
        props: GatehouseStackProps,
    ) {
        super(scope, id, props);

        const ec2App = 'gatehouse';

        const distBucket = GuDistributionBucketParameter.getInstance(this).valueAsString;

        const artifactPath = [distBucket, this.stack, this.stage, ec2App, `${ec2App}.deb`].join('/');

        const readAppSsmParamsPolicy = new GuPolicy(this, 'ReadAppSsmParamsPolicy', {
            statements: [new ReadParametersByName(this, {app: ec2App})]
        })

        // See https://aws-otel.github.io/docs/setup/permissions
        const xrayTelemetryPolicy = new GuPolicy(this, 'XrayTelemetryPolicy', {
            statements: [new PolicyStatement({
                effect: Effect.ALLOW,
                actions: [
                    'logs:PutLogEvents',
                    'logs:CreateLogGroup',
                    'logs:CreateLogStream',
                    'logs:DescribeLogStreams',
                    'logs:DescribeLogGroups',
                    'logs:PutRetentionPolicy',
                    'xray:PutTraceSegments',
                    'xray:PutTelemetryRecords',
                    'xray:GetSamplingRules',
                    'xray:GetSamplingTargets',
                    'xray:GetSamplingStatisticSummaries',
                ],
                resources: ['*']
            })]
        })

        const rdsSecurityGroupId = new GuStringParameter(this, 'rdsSecurityGroupId', {
            fromSSM: true,
            default: `/${this.stage}/${this.stack}/${ec2App}/rdsSecurityGroup/id`,
            description: 'ID of database security group.',
        });

        const app = new GuPlayApp(this, {
            app: ec2App,
            instanceType: InstanceType.of(InstanceClass.T4G, InstanceSize.MICRO),
            access: {scope: AccessScope.PUBLIC},
            userData: [
                '#!/bin/bash -ev',

                // See https://github.com/aws-observability/aws-otel-collector/blob/main/docs/developers/linux-rpm-demo.md
                '# Install X-Ray Collector',
                'wget -P /tmp https://aws-otel-collector.s3.amazonaws.com/ubuntu/arm64/latest/aws-otel-collector.deb',
                'dpkg -i /tmp/aws-otel-collector.deb',
                'cat << EOF > /opt/aws/aws-otel-collector/etc/config.yaml',
                '# Prepares collector to receive OTLP traces',
                '# See https://github.com/open-telemetry/opentelemetry-collector/tree/main/receiver/otlpreceiver#otlp-receiver',
                'receivers:',
                '  otlp:',
                '    protocols:',
                '      grpc:',
                'processors:',
                '  # Collects EC2 metadata.  In particular, we need the Stage tag to distinguish between prod and non-prod environments',
                '  # See https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/processor/resourcedetectionprocessor#resource-detection-processor',
                '  resourcedetection/ec2:',
                '    detectors:',
                '      - ec2',
                '    ec2:',
                '      tags:',
                '        - Stage',
                '    timeout: 2s',
                '    override: false',
                '  # Keeps the collector from using more than 20 MiB of memory',
                '  # See https://github.com/open-telemetry/opentelemetry-collector/tree/main/processor/memorylimiterprocessor#memory-limiter-processor',
                '  memory_limiter:',
                '    check_interval: 1s',
                '    limit_mib: 20',
                '  # Sends batches of up to 50 traces every second',
                '  # https://github.com/open-telemetry/opentelemetry-collector/tree/main/processor/batchprocessor#batch-processor',
                '  batch/traces:',
                '    timeout: 1s',
                '    send_batch_size: 50',
                '# Exports traces to AWS X-Ray, allowing Stage to be indexed for filtering',
                '# https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/exporter/awsxrayexporter',
                'exporters:',
                '  awsxray:',
                '    indexed_attributes:',
                '      - otel.resource.ec2.tag.Stage',
                '# Allows access to AWS APIs',
                '# https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/extension/awsproxy',
                'extensions:',
                '  awsproxy:',
                '# Wires all the resources defined above together',
                'service:',
                '  extensions:',
                '    - awsproxy',
                '  pipelines:',
                '    traces:',
                '      receivers:',
                '        - otlp',
                '      processors:',
                '        - resourcedetection/ec2',
                '        - memory_limiter',
                '        - batch/traces',
                '      exporters:',
                '        - awsxray',
                'EOF',
                // 'echo "loggingLevel=DEBUG" | sudo tee -a /opt/aws/aws-otel-collector/etc/extracfg.txt',
                'sudo /opt/aws/aws-otel-collector/bin/aws-otel-collector-ctl -a start',

                // See https://aws-otel.github.io/docs/getting-started/java-sdk/auto-instr
                '# Install X-Ray Agent',
                'sudo mkdir /opt/aws-opentelemetry-agent',
                'chmod +rx /opt/aws-opentelemetry-agent',
                'wget -P /opt/aws-opentelemetry-agent https://github.com/aws-observability/aws-otel-java-instrumentation/releases/latest/download/aws-opentelemetry-agent.jar',

                '# Install app',
                `aws --region ${props.env?.region} s3 cp s3://${artifactPath} /tmp/${ec2App}.deb`,
                `dpkg -i /tmp/${ec2App}.deb`,
            ].join('\n'),
            certificateProps: {
                domainName: props.domainName,
            },
            monitoringConfiguration: {noMonitoring: true},
            scaling: {
                minimumInstances: 1,
                maximumInstances: 2,
            },
            applicationLogging: {
                enabled: true,
                systemdUnitName: 'gatehouse'
            },
            imageRecipe: 'arm-identity-base-jammy-java21-cdk-base',
            roleConfiguration: {
                additionalPolicies: [
                    readAppSsmParamsPolicy,
                    xrayTelemetryPolicy,
                ],
            },
        });

        app.autoScalingGroup.connections.addSecurityGroup(
            SecurityGroup.fromSecurityGroupId(this, 'rdsSecurityGroup', rdsSecurityGroupId.valueAsString, {
                mutable: false
            }));

        // This parameter is used by https://github.com/guardian/waf
        new StringParameter(this, 'AlbSsmParam', {
            parameterName: `/infosec/waf/services/${this.stage}/gatehouse-alb-arn`,
            description: `The ARN of the ALB for identity-${this.stage}-gatehouse. N.B. This parameter is created via CDK.`,
            simpleName: false,
            stringValue: app.loadBalancer.loadBalancerArn,
            tier: ParameterTier.STANDARD,
            dataType: ParameterDataType.TEXT,
        });

        new GuCname(this, 'EC2AppDNS', {
            app: ec2App,
            ttl: Duration.hours(1),
            domainName: props.domainName,
            resourceRecord: app.loadBalancer.loadBalancerDnsName,
        });
    }
}
