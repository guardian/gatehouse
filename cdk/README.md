# Infrastructure

This directory defines the components to be deployed to AWS.

See [`package.json`](./package.json) for a list of available scripts.

## Contributing

1. Run `yarn` to install dependencies.
2. Make your change to [`gatehouse.ts`](./lib/gatehouse.ts).
3. Update the CDK Snapshots with `yarn test -u`. Verify that the snapshot diff contains the expected changes.
4. Lint and format your changes with `yarn lint && yarn format`.
5. Open a pull request and get it merged!