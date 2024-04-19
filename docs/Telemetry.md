# Telemetry

We're using AWS X-Ray in Code and Prod to (eventually) get distributed traces of client apps calling Gatehouse and Gatehouse then calling Okta.
We're using the OpenTelemetry Java agent to instrument the JVM.
This sends traces to a local collector, which forwards them to AWS X-Ray.

## Troubleshooting traces locally

It doesn't seem to be straightforward to send JVM arguments to Play apps in Dev mode.  
This is how I have been working locally.

1. In Intellij, I set JAVA_TOOL_OPTIONS in my SBT config:  
sbt Tool Window > Build Tool Settings > sbt Settings > Environment variables:  
JAVA_TOOL_OPTIONS=-javaagent:/<path>/aws-opentelemetry-agent.jar -Dotel.javaagent.configuration-file=conf/telemetry.conf

2. Download the agent to the path specified above.  
See cdk/lib/gatehouse.ts for steps to download the agent.

3. Start a new SBT session.
