<meta name="google-site-verification" content="wJhSO9RTsMUSwpu-myIzdobIQFMsq3Ej-zbwxAl6blU" />
# Pmitz

[![Build Snapshot](https://github.com/terpomo-io/pmitz/actions/workflows/ci-workflow.yml/badge.svg)](https://github.com/terpomo-io/pmitz/actions)
[![Licence](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)


Pmitz is a tool to control users' access to application features based on users' subscriptions and / or configured usage limits.

Pmitz encapsulates the complexity related to the management of subscriptions, plans and limits, allowing development teams to effectively implement a subscription model within their software solutions. Pmitz allows development teams to implement usage limits in a configurable and flexible (per user) way.

## Installation

### How to download Pmitz

Clone github directory [https://github.com/terpomo-io/pmitz](https://github.com/terpomo-io/pmitz).

### How to use it from Maven.

```xml
<dependency>
  <groupId>io.terpomo.pmitz</groupId>
  <artifactId>pmitz</artifactId>
  <version>0.8.0</version>
</dependency>
```

### How to use it from Gradle.
```groovy
dependencies {
  implementation 'io.terpomo.pmitz:pmitz:0.8.0'
}
```

## Usage

To access examples of using Pmitz, refer to the [examples](examples) folder

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

