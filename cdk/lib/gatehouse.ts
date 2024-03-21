import {GuPlayApp} from '@guardian/cdk';
import {AccessScope} from '@guardian/cdk/lib/constants/access';
import type {GuStackProps} from '@guardian/cdk/lib/constructs/core';
import {GuStack, GuStringParameter} from '@guardian/cdk/lib/constructs/core';
import {GuCname} from '@guardian/cdk/lib/constructs/dns';
import {GuPolicy, ReadParametersByName} from '@guardian/cdk/lib/constructs/iam';
import type {App} from 'aws-cdk-lib';
import {Duration} from 'aws-cdk-lib';
import {InstanceClass, InstanceSize, InstanceType, SecurityGroup} from 'aws-cdk-lib/aws-ec2';
import {Effect, PolicyStatement} from "aws-cdk-lib/aws-iam";
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

        const readAppSsmParamsPolicy = new GuPolicy(this, 'ReadAppSsmParamsPolicy', {
            statements: [new ReadParametersByName(this, {app: ec2App})]
        })

        const xrayTelemetryPolicy = new GuPolicy(this, 'XrayTelemetryPolicy', {
            statements: [new PolicyStatement({
                effect: Effect.ALLOW,
                actions: [
                    "xray:PutTraceSegments",
                    "xray:PutTelemetryRecords",
                    "xray:GetSamplingRules",
                    "xray:GetSamplingTargets",
                    "xray:GetSamplingStatisticSummaries",
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
            userData: {
                distributable: {
                    fileName: `${ec2App}.deb`,
                    executionStatement: `dpkg -i /${ec2App}/${ec2App}.deb`,
                },
            },
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
