import {GuPlayApp} from '@guardian/cdk';
import {AccessScope} from '@guardian/cdk/lib/constants/access';
import {GuStack} from '@guardian/cdk/lib/constructs/core';
import type {GuStackProps} from '@guardian/cdk/lib/constructs/core';
import {GuCname} from '@guardian/cdk/lib/constructs/dns';
import {GuPolicy, ReadParametersByName} from "@guardian/cdk/lib/constructs/iam";
import type {App} from 'aws-cdk-lib';
import {Duration} from 'aws-cdk-lib';
import {InstanceClass, InstanceSize, InstanceType} from 'aws-cdk-lib/aws-ec2';

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

        const {loadBalancer} = new GuPlayApp(this, {
            app: ec2App,
            instanceType: InstanceType.of(InstanceClass.T4G, InstanceSize.MICRO),
            access: {scope: AccessScope.PUBLIC},
            userData: {
                distributable: {
                    fileName: `${ec2App}.deb`,
                    executionStatement: `dpkg -i /${ec2App}/${ec2App}.deb && echo "stage=${this.stage}" | sudo tee "/etc/gatehouse/stage.conf" > /dev/null`,
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
            imageRecipe: 'arm-identity-base-jammy-java11-cdk-base',
            roleConfiguration: {
                additionalPolicies: [readAppSsmParamsPolicy],
            },
        });

        new GuCname(this, 'EC2AppDNS', {
            app: ec2App,
            ttl: Duration.hours(1),
            domainName: props.domainName,
            resourceRecord: loadBalancer.loadBalancerDnsName,
        });
    }
}