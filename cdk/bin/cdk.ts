import 'source-map-support/register';
import {GuRoot} from '@guardian/cdk/lib/constructs/root';
import {Gatehouse} from '../lib/gatehouse';

const app = new GuRoot();

const stack = 'identity';

const env = {
    region: 'eu-west-1',
};

new Gatehouse(app, 'gatehouse-CODE', {
    stack,
    env,
    stage: 'CODE',
    domainName: 'gatehouse-origin.code.dev-guardianapis.com',
});

new Gatehouse(app, 'gatehouse-PROD', {
    stack,
    env,
    stage: 'PROD',
    domainName: 'gatehouse-origin.guardianapis.com',
});
