# Spring Boot Trading Agent implementation

This repository contains a Spring Boot
[Trading Agent](https://github.com/alexandreroman/sta-trading-agent)
implementation for [Spring Trading App](https://github.com/alexandreroman/sta).

You may use this repository as a project template.

Be creative, write the best algorithm and make some money 🤑

## Prerequisites

Make sure you get the OAuth2 credentials required for accessing the
[Stock Marketplace](https://github.com/alexandreroman/sta-marketplace) API,
as well as the marketplace URL and the user you picked for placing bids.

### Setting the configuration for your local workstation

Edit the configuration in [`src/main/resources/application-dev.yaml`](src/main/resources/application-dev.yaml):

```yaml
app:
  marketplace:
    url: https://sta.az.run.withtanzu.com
  agent:
    strategy: buy-lower-stock
    user: insert-user
```

You also need to set up the OAuth2 configuration in order to connect to
the Marketplace API.

Create the file `application-oauth2.yaml` (using the [template](src/main/resources/application-oauth2.yaml.template)):

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          sso:
            client-id: insert-client-id
            client-secret: insert-client-secret
            authorization-grant-type: client_credentials
            scope:
            - bid
        provider:
          sso:
            issuer-uri: https://login.sso.az.run.withtanzu.com
```

### Setting the configuration for TAP Sandbox

Edit the configuration in [`config/app-operator`](config/app-operator/).

You have 2 files in this directory (defined as Kubernetes Secret resources).

These configuration parameters are actually defined as
[Service Binding](https://servicebinding.io/) attributes:

| Name   | Key                 | Value                         |
|--------|---------------------|-------------------------------|
| config | app.agent.user      | User defined for placing bids |
| config | app.marketplace.url | Spring Trading App URL        |
| sso    | client-id           | OAuth2 Client Id              |
| sso    | client-secret       | OAuth2 Client Secret          |
| sso    | issuer-uri          | OAuth2 Issuer URI             |

## How to run the app?

Run this command to build and run the app:

```shell
mvn spring-boot:run -Dspring-boot.run.profiles=dev,oauth2
```

The app is available at http://localhost:8082.

## Deploy this app to TAP Sandbox

Run this command to deploy your app to TAP Sandbox:

```shell
kubectl apply -f config -f config/app-operator
```
