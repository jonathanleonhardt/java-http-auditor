## Audit any HTTP request

This library fulfills its role of auditing every HTTP request sent to or received by your application, discerning and capturing only the pertinent data.

## Prerequisites

- Java 17 >
- Be using HTTP in your endpoints ou in a http client

## Highly compatible:

- Quarkus
- Spring Boot

## USE CASES

- [Logging requests](#logging-requests), response and relative informations to requests made by http clients.
- [Intercepting http requests made by clients](#active-requests) to save in database ou obtain specific infos.
- [Intercepting http requests received by your service](#passive-requests) to save in database ou obtain specific infos.

## HOW TO USE

Import the most recent version in your `pom.xml`:

```xml
<dependency>
	<groupId>br.com.potio</groupId>
	<artifactId>http-auditor</artifactId>
	<version>0.2.0</version>
</dependency>
```

## Logging Requests

Just inject the `LoggingFilter` provider

## Active Requests

## Passive Requests



