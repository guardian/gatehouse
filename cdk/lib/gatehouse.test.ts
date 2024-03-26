import {App} from 'aws-cdk-lib';
import {Template} from 'aws-cdk-lib/assertions';
import {Gatehouse} from './gatehouse';

describe('The Gatehouse stack', () => {
    it('matches the snapshot', () => {
        const app = new App();
        const stack = new Gatehouse(app, 'gatehouse-TEST', {
            stack: 'identity',
            stage: 'TEST',
            domainName: 'id.test.dev-guardianapis.com',
            env: {region: 'eu-west-1'},
        });
        const template = Template.fromStack(stack);
        expect(template.toJSON()).toMatchSnapshot();
    });
});
