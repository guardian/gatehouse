import { GuAlarm } from '@guardian/cdk/lib/constructs/cloudwatch';
import type { GuStackProps } from '@guardian/cdk/lib/constructs/core';
import { GuStack, GuStringParameter } from '@guardian/cdk/lib/constructs/core';
import { GuCname } from '@guardian/cdk/lib/constructs/dns';
import { GuVpc, SubnetType } from '@guardian/cdk/lib/constructs/ec2';
import { GuRole } from '@guardian/cdk/lib/constructs/iam';
import type { App } from 'aws-cdk-lib';
import { Duration, SecretValue, Tags } from 'aws-cdk-lib';
import { ComparisonOperator, Metric } from 'aws-cdk-lib/aws-cloudwatch';
import {
	CfnEndpoint,
	CfnReplicationConfig,
	CfnReplicationSubnetGroup,
} from 'aws-cdk-lib/aws-dms';
import { Port, SecurityGroup } from 'aws-cdk-lib/aws-ec2';
import {
	CompositePrincipal,
	Effect,
	PolicyDocument,
	PolicyStatement,
	ServicePrincipal,
} from 'aws-cdk-lib/aws-iam';
import type { CfnDBCluster } from 'aws-cdk-lib/aws-rds';
import {
	AuroraPostgresEngineVersion,
	ClusterInstance,
	Credentials,
	DatabaseCluster,
	DatabaseClusterEngine,
	PerformanceInsightRetention,
} from 'aws-cdk-lib/aws-rds';
import { Secret } from 'aws-cdk-lib/aws-secretsmanager';
import { Topic } from 'aws-cdk-lib/aws-sns';
import { EmailSubscription } from 'aws-cdk-lib/aws-sns-subscriptions';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';

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

		const rdsSecurityGroupId = new GuStringParameter(
			this,
			'rdsSecurityGroupId',
			{
				fromSSM: true,
				default: `/${stage}/${stack}/${ec2App}/rdsSecurityGroup/id`,
				description: 'ID of database security group.',
			},
		);

		// This repository previously contained a Scala Play App which was intended to be a replacement for
		// Identity API. The app was never completed and the repository is now used for the Gatehouse database.
		// We may revive this app at some point in the future, so hold on to the CNAME record in case we need it.
		new GuCname(this, 'EC2AppDNS', {
			app: ec2App,
			ttl: Duration.hours(1),
			domainName: props.domainName,
			resourceRecord: '255.255.255.255',
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

		const subnets = GuVpc.subnetsFromParameterFixedNumber(
			this,
			{
				type: SubnetType.PRIVATE,
			},
			3,
		);

		const cluster = new DatabaseCluster(this, 'GatehouseDb', {
			engine: DatabaseClusterEngine.auroraPostgres({
				version: AuroraPostgresEngineVersion.VER_16_6,
			}),
			writer: ClusterInstance.serverlessV2('writer'),
			readers: [
				ClusterInstance.serverlessV2('reader', { scaleWithWriter: false }),
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
			// Our maintenance window during which we can apply upgrades is set to a time when traffic is low.
			// https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/USER_UpgradeDBInstance.PostgreSQL.MinorUpgrade.html
			autoMinorVersionUpgrade: true,
			preferredMaintenanceWindow: 'Wed:04:30-Wed:05:00',
			backup: {
				retention: Duration.days(14),
			},
			performanceInsightRetention: PerformanceInsightRetention.DEFAULT,
			monitoringInterval: Duration.minutes(1),
			defaultDatabaseName: 'gatehouse',
			port: databasePort,
			serverlessV2MinCapacity,
			serverlessV2MaxCapacity,
			securityGroups: [rdsSecurityGroupRules],
			vpcSubnets: {
				subnets,
			},
			vpc,
			parameters: {
				// Require verifying SSL before connecting to DB
				'rds.force_ssl': '1',
			},
		});

		const alarmNotificationEmail = new GuStringParameter(
			this,
			'alarmNotificationEmail',
			{
				fromSSM: true,
				default: `/${stage}/${stack}/${ec2App}/alarmNotificationEmail`,
				description: 'Notification email for gatehouse DB Alarms.',
			},
		);

		const notificationTopic = new Topic(this, 'NotificationTopic', {
			displayName: 'Gatehouse notifications',
		});

		notificationTopic.addSubscription(
			new EmailSubscription(alarmNotificationEmail.valueAsString),
		);
		new GuAlarm(this, 'HighUsageAlarm', {
			app: 'gatehouseDB',
			alarmName: `High usage in ${this.stage} Gatehouse database`,
			alarmDescription: 'Gatehouse usage is above 80%',
			snsTopicName: notificationTopic.topicName,
			actionsEnabled: this.stage === 'PROD',
			threshold: 80,
			evaluationPeriods: 2,
			comparisonOperator: ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
			metric: new Metric({
				metricName: 'ACUUtilization',
				namespace: 'AWS/RDS',
				dimensionsMap: {
					DBClusterIdentifier: cluster.clusterIdentifier,
				},
				statistic: 'Average',
				period: Duration.seconds(60),
			}),
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

		// Output DB Identifier as SSM parameter to be used in other stacks.
		new StringParameter(this, 'DatabaseClusterIdentifierOutputParameter', {
			parameterName: `/${stage}/${stack}/${ec2App}/db-identifier`,
			stringValue: cluster.clusterIdentifier,
		});

		new StringParameter(
			this,
			'DatabaseClusterResourceIdentifierOutputParameter',
			{
				parameterName: `/${stage}/${stack}/${ec2App}/db-resource-identifier`,
				stringValue: cluster.clusterResourceIdentifier,
			},
		);

		const dmsSourceCredentials = new Secret(this, 'DMSSourceCredentials', {});
		const dmsTargetCredentials = new Secret(this, 'DMSTargetCredentials', {});

		const dmsSourceIamRole = new GuRole(this, 'DMSSourceIamRole', {
			assumedBy: new CompositePrincipal(
				new ServicePrincipal('dms.amazonaws.com', {
					conditions: {
						StringEquals: {
							'aws:SourceAccount': this.account,
						},
					},
				}),
				new ServicePrincipal('dms.eu-west-1.amazonaws.com', {
					conditions: {
						StringEquals: {
							'aws:SourceAccount': this.account,
						},
					},
				}),
				new ServicePrincipal('dms-data-migrations.amazonaws.com', {
					conditions: {
						StringEquals: {
							'aws:SourceAccount': this.account,
						},
					},
				}),
			),
			inlinePolicies: {
				retrieveCredentials: new PolicyDocument({
					statements: [
						new PolicyStatement({
							effect: Effect.ALLOW,
							actions: [
								'secretsmanager:DescribeSecret',
								'secretsmanager:GetSecretValue',
							],
							resources: [dmsSourceCredentials.secretArn],
						}),
					],
				}),
			},
		});

		const dmsMigrationSourceEndpoint = new CfnEndpoint(
			this,
			'DMSIdentitySourceEndpoint',
			{
				endpointType: 'source',
				engineName: 'postgres',
				databaseName: 'identitydb',
				sslMode: 'require',
				postgreSqlSettings: {
					secretsManagerAccessRoleArn: dmsSourceIamRole.roleArn,
					secretsManagerSecretId: dmsSourceCredentials.secretArn,
				},
			},
		);

		const dmsTargetIamRole = new GuRole(this, 'DMSTargetIamRole', {
			assumedBy: new CompositePrincipal(
				new ServicePrincipal('dms.amazonaws.com', {
					conditions: {
						StringEquals: {
							'aws:SourceAccount': this.account,
						},
					},
				}),
				new ServicePrincipal('dms.eu-west-1.amazonaws.com', {
					conditions: {
						StringEquals: {
							'aws:SourceAccount': this.account,
						},
					},
				}),
				new ServicePrincipal('dms-data-migrations.amazonaws.com', {
					conditions: {
						StringEquals: {
							'aws:SourceAccount': this.account,
						},
					},
				}),
			),
			inlinePolicies: {
				retrieveCredentials: new PolicyDocument({
					statements: [
						new PolicyStatement({
							effect: Effect.ALLOW,
							actions: [
								'secretsmanager:DescribeSecret',
								'secretsmanager:GetSecretValue',
							],
							resources: [dmsTargetCredentials.secretArn],
						}),
					],
				}),
			},
		});

		const dmsMigrationTargetEndpoint = new CfnEndpoint(
			this,
			'DMSGatehouseTargetEndpoint',
			{
				endpointType: 'target',
				engineName: 'postgres',
				databaseName: 'gatehouse',
				sslMode: 'require',
				postgreSqlSettings: {
					secretsManagerAccessRoleArn: dmsTargetIamRole.roleArn,
					secretsManagerSecretId: dmsTargetCredentials.secretArn,
				},
			},
		);

		const dmsSubnetGroup = new CfnReplicationSubnetGroup(
			this,
			'DMSReplicationSubnetGroup',
			{
				replicationSubnetGroupDescription: 'DMS Replication Subnet Group',
				subnetIds: subnets.map((subnet) => subnet.subnetId),
			},
		);

		new CfnReplicationConfig(this, 'DMSReplicationConfig', {
			replicationConfigIdentifier: `${ec2App}-${stage}`,
			replicationSettings: {
				Logging: {
					EnableLogging: true,
				},
			},
			computeConfig: {
				minCapacityUnits: 1,
				maxCapacityUnits: 8,
				replicationSubnetGroupId: dmsSubnetGroup.ref,
				vpcSecurityGroupIds: [
					rdsSecurityGroupClients.securityGroupId,
					rdsSecurityGroupId.valueAsString,
				],
			},
			replicationType: 'full-load',
			sourceEndpointArn: dmsMigrationSourceEndpoint.ref,
			targetEndpointArn: dmsMigrationTargetEndpoint.ref,
			tableMappings: {
				rules: [
					{
						'rule-id': '1',
						'rule-name': '1',
						'rule-type': 'selection',
						'rule-action': 'include',
						'object-locator': {
							'schema-name': 'public',
							'table-name': 'clients',
						},
						filters: [],
					},
					{
						'rule-id': '2',
						'rule-name': '2',
						'rule-type': 'selection',
						'rule-action': 'include',
						'object-locator': {
							'schema-name': 'public',
							'table-name': 'clientaccesstokens',
						},
						filters: [],
					},
					{
						'rule-id': '3',
						'rule-name': '3',
						'rule-type': 'transformation',
						'rule-target': 'table',
						'rule-action': 'add-prefix',
						'object-locator': {
							'schema-name': 'public',
							'table-name': 'clients',
						},
						value: 'identity_',
					},
					{
						'rule-id': '4',
						'rule-name': '4',
						'rule-type': 'transformation',
						'rule-target': 'table',
						'rule-action': 'add-prefix',
						'object-locator': {
							'schema-name': 'public',
							'table-name': 'clientaccesstokens',
						},
						value: 'identity_',
					},
				],
			},
		});
	}
}
