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
// import {CfnGroup} from 'aws-cdk-lib/aws-xray';
// import {CfnSamplingRule} from 'aws-cdk-lib/aws-xray';

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
                'receivers:',
                '  otlp:',
                '    protocols:',
                '      grpc:',
                'processors:',
                '  resourcedetection/ec2:',
                '    detectors:',
                '      - ec2',
                '    ec2:',
                '      tags:',
                '        - Stage',
                '    timeout: 2s',
                '    override: false',
                '  memory_limiter:',
                '    check_interval: 1s',
                '    limit_mib: 20',
                '  batch/traces:',
                '    timeout: 1s',
                '    send_batch_size: 50',
                'exporters:',
                '  awsxray:',
                '    indexed_attributes:',
                '      - otel.resource.ec2.tag.Stage',
                '      - http.route',
                'extensions:',
                '  awsproxy:',
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
                'echo "loggingLevel=DEBUG" | sudo tee -a /opt/aws/aws-otel-collector/etc/extracfg.txt',
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

        // TODO: Janus to allow xray:CreateGroup
        // new CfnGroup(this, 'CodeXrayGroup', {
        //     groupName: 'CODE',
        //     filterExpression: 'annotation.otel_resource_ec2_tag_Stage = "CODE"',
        //     insightsConfiguration: {insightsEnabled: true},
        // });
        // new CfnGroup(this, 'ProdXrayGroup', {
        //     groupName: 'PROD',
        //     filterExpression: 'annotation.otel_resource_ec2_tag_Stage = "PROD"',
        //     insightsConfiguration: {insightsEnabled: true},
        // });

        // TODO: Janus to allow xray:CreateSamplingRule
        // TODO: how to filter on range of values?
        // Sample rule 1: Include all bad requests
        // new CfnSamplingRule(this, 'BadRequestsSamplingRule', {
        //     samplingRule: {
        //         ruleName: 'bad-requests',
        //         priority: 1,
        //         reservoirSize: 1,
        //         fixedRate: 1,
        //         serviceName: '*',
        //         serviceType: '*',
        //         httpMethod: '*',
        //         urlPath: '*',
        //         resourceArn: '*',
        //         host: '*',
        //         attributes: {
        //             'http.status_code': '*',
        //         },
        //     }
        // });

        // Sample rule 2: Exclude healthchecks
        // new CfnSamplingRule(this, 'HealthCheckSamplingRule', {
        //     samplingRule: {
        //         version: 1,
        //         ruleName: 'health-checks',
        //         priority: 2,
        //         reservoirSize: 0,
        //         fixedRate: 0,
        //         serviceName: '*',
        //         serviceType: '*',
        //         httpMethod: '*',
        //         urlPath: '*',
        //         resourceArn: '*',
        //         host: '*',
        //         attributes: {
        //             'http.route': '/healthcheck',
        //         },
        //     }
        // });

        // Sample rule 3: Limit remaining requests to 1 a second
    //     new CfnSamplingRule(this, 'DefaultSamplingRule', {
    //         samplingRule: {
    //             version: 1,
    //             ruleName: 'default',
    //             priority: 999,
    //             reservoirSize: 1,
    //             fixedRate: 0,
    //             serviceName: '*',
    //             serviceType: '*',
    //             httpMethod: '*',
    //             urlPath: '*',
    //             resourceArn: '*',
    //             host: '*',
    //         }
    //     });
    }
}
