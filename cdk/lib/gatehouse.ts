import { GuPlayApp } from '@guardian/cdk';
import { AccessScope } from '@guardian/cdk/lib/constants/access';
import type { GuStackProps } from '@guardian/cdk/lib/constructs/core';
import {
	GuDistributionBucketParameter,
	GuStack,
	GuStringParameter,
} from '@guardian/cdk/lib/constructs/core';
import { GuCname } from '@guardian/cdk/lib/constructs/dns';
import { GuVpc, SubnetType } from '@guardian/cdk/lib/constructs/ec2';
import {
	GuPolicy,
	ReadParametersByName,
} from '@guardian/cdk/lib/constructs/iam';
import type { App } from 'aws-cdk-lib';
import { Duration, SecretValue, Tags } from 'aws-cdk-lib';
import {
	InstanceClass,
	InstanceSize,
	InstanceType,
	Port,
	SecurityGroup,
	UserData,
} from 'aws-cdk-lib/aws-ec2';
import { Effect, PolicyStatement } from 'aws-cdk-lib/aws-iam';
import type { CfnDBCluster } from 'aws-cdk-lib/aws-rds';
import {
	AuroraPostgresEngineVersion,
	ClusterInstance,
	Credentials,
	DatabaseCluster,
	DatabaseClusterEngine,
	PerformanceInsightRetention,
} from 'aws-cdk-lib/aws-rds';
import {
	ParameterDataType,
	ParameterTier,
	StringParameter,
} from 'aws-cdk-lib/aws-ssm';

export interface GatehouseStackProps extends GuStackProps {
	domainName: string;
	database: {
		minCapacity: number;
		maxCapacity: number;
	};
}

export class Gatehouse extends GuStack {
	constructor(scope: App, id: string, props: GatehouseStackProps) {
		super(scope, id, props);

		const {
			stack,
			stage,
			database: {
				minCapacity: serverlessV2MinCapacity,
				maxCapacity: serverlessV2MaxCapacity,
			},
		} = props;

		const ec2App = 'gatehouse';
		const databasePort = 5432;

		const distBucket =
			GuDistributionBucketParameter.getInstance(this).valueAsString;

		const artifactPath = [
			distBucket,
			stack,
			stage,
			ec2App,
			`${ec2App}.deb`,
		].join('/');

		const readAppSsmParamsPolicy = new GuPolicy(
			this,
			'ReadAppSsmParamsPolicy',
			{
				statements: [new ReadParametersByName(this, { app: ec2App })],
			},
		);

		// See https://aws-otel.github.io/docs/setup/permissions
		const xrayTelemetryPolicy = new GuPolicy(this, 'XrayTelemetryPolicy', {
			statements: [
				new PolicyStatement({
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
					resources: ['*'],
				}),
			],
		});

		const rdsSecurityGroupId = new GuStringParameter(
			this,
			'rdsSecurityGroupId',
			{
				fromSSM: true,
				default: `/${stage}/${stack}/${ec2App}/rdsSecurityGroup/id`,
				description: 'ID of database security group.',
			},
		);

		const app = new GuPlayApp(this, {
			app: ec2App,
			instanceType: InstanceType.of(InstanceClass.T4G, InstanceSize.MICRO),
			access: { scope: AccessScope.PUBLIC },
			userData: UserData.custom(
				[
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
			),
			certificateProps: {
				domainName: props.domainName,
			},
			monitoringConfiguration: { noMonitoring: true },
			scaling: {
				minimumInstances: 0,
				maximumInstances: 2,
			},
			applicationLogging: {
				enabled: true,
				systemdUnitName: 'gatehouse',
			},
			imageRecipe: 'arm-identity-base-jammy-java21-cdk-base',
			roleConfiguration: {
				additionalPolicies: [readAppSsmParamsPolicy, xrayTelemetryPolicy],
			},
		});

		app.autoScalingGroup.connections.addSecurityGroup(
			SecurityGroup.fromSecurityGroupId(
				this,
				'rdsSecurityGroup',
				rdsSecurityGroupId.valueAsString,
				{
					mutable: false,
				},
			),
		);

		// This parameter is used by https://github.com/guardian/waf
		new StringParameter(this, 'AlbSsmParam', {
			parameterName: `/infosec/waf/services/${stage}/gatehouse-alb-arn`,
			description: `The ARN of the ALB for identity-${stage}-gatehouse. N.B. This parameter is created via CDK.`,
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

		const vpc = GuVpc.fromIdParameter(this, 'IdentityVPC');
		const rdsSecurityGroupRules = new SecurityGroup(
			this,
			'RDSSecurityGroupRules',
			{
				vpc: vpc,
				allowAllOutbound: false,
			},
		);

		const rdsSecurityGroupClients = new SecurityGroup(
			this,
			'RDSSecurityGroupClients',
			{
				vpc: vpc,
				allowAllOutbound: false,
				description: 'Allow access to Gatehouse DB from Clients',
			},
		);

		rdsSecurityGroupRules.addIngressRule(
			rdsSecurityGroupClients,
			Port.tcp(databasePort),
		);

		const cluster = new DatabaseCluster(this, 'GatehouseDb', {
			engine: DatabaseClusterEngine.auroraPostgres({
				version: AuroraPostgresEngineVersion.VER_16_6,
			}),
			writer: ClusterInstance.serverlessV2('writer'),
			readers: [
				// Scale reader instance with writer so that it can deal with immediate traffic spike during failover
				ClusterInstance.serverlessV2('reader', { scaleWithWriter: true }),
			],
			credentials: Credentials.fromPassword(
				'postgres',
				// This value will be replaced by the escape hatch to use ManagerMasterUserPassword below
				SecretValue.secretsManager('uselessWorkaroundSecret'),
			),
			storageEncrypted: true,
			deletionProtection: true,
			iamAuthentication: true,
			enableDataApi: true,
			enableClusterLevelEnhancedMonitoring: true,
			enablePerformanceInsights: true,
			// Under some scenarios AWS can upgrade the database version without downtime
			// However all upgrades, including minor ones, can result in downtime in certain scenarios.
			// https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/USER_UpgradeDBInstance.PostgreSQL.MinorUpgrade.html
			autoMinorVersionUpgrade: false,
			performanceInsightRetention: PerformanceInsightRetention.DEFAULT,
			monitoringInterval: Duration.minutes(1),
			defaultDatabaseName: 'gatehouse',
			port: databasePort,
			serverlessV2MinCapacity,
			serverlessV2MaxCapacity,
			securityGroups: [rdsSecurityGroupRules],
			vpcSubnets: {
				subnets: GuVpc.subnetsFromParameterFixedNumber(
					this,
					{
						type: SubnetType.PRIVATE,
					},
					3,
				),
			},
			vpc,
			parameters: {
				// Require verifying SSL before connecting to DB
				'rds.force_ssl': '1',
			},
		});

		// Resources tagged with devx-backup-enabled=true will be backed up by the DevX backup service
		// https://github.com/guardian/aws-account-setup/blob/42885f5d22dbee137950d4e7500bbb1d7cc1bf77/packages/cdk/lib/aws-backup.ts#L72-L76
		Tags.of(cluster).add('devx-backup-enabled', 'true');

		// CDK currently does not support ManagerMasterUserPassword
		// See https://github.com/aws/aws-cdk/issues/29239
		const defaultChild = cluster.node.defaultChild as CfnDBCluster;
		defaultChild.addOverride('Properties.ManageMasterUserPassword', true);
		defaultChild.addOverride('Properties.MasterUserPassword', undefined);

		// Output RDS Client security group as SSM parameter to be used in other stacks.
		new StringParameter(this, 'ClientSecurityGroupOutputParameter', {
			parameterName: `/${stage}/${stack}/${ec2App}/db-clients-security-group`,
			stringValue: rdsSecurityGroupClients.securityGroupId,
		});
	}
}
