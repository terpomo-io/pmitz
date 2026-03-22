<meta name="google-site-verification" content="wJhSO9RTsMUSwpu-myIzdobIQFMsq3Ej-zbwxAl6blU" />
# Pmitz

[![Build Snapshot](https://github.com/terpomo-io/pmitz/actions/workflows/ci-workflow.yml/badge.svg)](https://github.com/terpomo-io/pmitz/actions)
[![Licence](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)


Pmitz is a Java library for subscription management, feature entitlements, and usage limits in multi-tenant
applications.

Pmitz helps development teams decide whether a user can access a feature, enforce per-user or per-tenant quotas, and
track usage across products, plans, and subscriptions. The project includes core domain modules, limit and
subscription verification modules, a remote server/client pair, and a Spring Boot starter for remote enforcement.

## What Pmitz Helps With

* SaaS subscription management and feature gating
* Feature entitlements based on product plans
* Usage quotas and rate limits for individual users or groups
* Multi-tenant applications that need configurable access control
* Remote limit verification with Spring Boot and HTTP clients

## Installation

### How to download Pmitz

Clone the GitHub repository [https://github.com/terpomo-io/pmitz](https://github.com/terpomo-io/pmitz).

### How to use it from Maven

```xml
<dependency>
  <groupId>io.terpomo.pmitz</groupId>
  <artifactId>pmitz-all</artifactId>
  <version>0.8.0</version>
</dependency>
```

### How to use it from Gradle
```groovy
dependencies {
  implementation 'io.terpomo.pmitz:pmitz-all:0.8.0'
}
```

### Available Artifacts

| Artifact | Purpose |
| --- | --- |
| `pmitz-all` | Aggregated core and limits modules |
| `pmitz-core` | Domain models and base abstractions |
| `pmitz-limits` | Usage limit verification and tracking |
| `pmitz-subscriptions` | Subscription management and entitlement verification |
| `pmitz-remoteserver` | Spring Boot remote server |
| `pmitz-remoteclient` | HTTP client for the remote server |
| `pmitz-spring-boot-starter-remoteserver` | Spring Boot starter for remote enforcement |

## Usage

To access examples of using Pmitz, refer to the [examples](examples) folder.

For a more complete walkthrough, see the [user guide](USERGUIDE.md).

## Requirements
* Java 17

## Reporting Issues

Use the issue manager offered with Github.
But before submitting a problem, please follow the following guidelines
* Before opening a problem, check if the problem has not already been reported.
* Please enter as much information as possible about the problem experienced in the problem sheet.
* If you include code in the problem description, please enclose it between two ``` lines to enhance code formatting

## Contributing

Pull requests are welcome. For major changes, please open an issue first
to discuss what you would like to change.

See our page explaining how to contribute ([CONTRIBUTING.md](CONTRIBUTING.md))

## License

Pmitz is Open Source software released under the [Apache 2.0 license](LICENSE)

![Licence](https://img.shields.io/badge/License-Apache%202.0-green.svg)
