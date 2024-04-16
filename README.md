## Audit any HTTP request

This library fulfills its role of auditing every HTTP request sent to or received by your application, discerning and capturing only the pertinent data.

## Prerequisites

- Java 17 >
- Be using HTTP in your endpoints or in a http client

## Highly compatible:

- Quarkus
- Microprofile

## USE CASES

- [Logging requests](#logging-requests), response and relative informations to requests made by http clients.
- [Intercepting http requests made by clients](#active-requests) to save in database ou obtain specific infos.
- [Intercepting http requests received by your service](#passive-requests) to save in database ou obtain specific infos.

## HOW TO USE WITH QUARKUS

Import the most recent version in your `pom.xml`:

```xml
<dependency>
	<groupId>br.com.potio</groupId>
	<artifactId>http-auditor</artifactId>
	<version>0.4.0</version>
</dependency>
```

## Logging Requests

Just inject the `LoggingFilter` provider in your client interface with `@RegisterProvider`:

```java
@RegisterProvider( LoggingFilter.class )
```

E.g.:

```java
...
@Path( "/my-api" )
@RegisterProvider( LoggingFilter.class )
public interface IAPIClient {

	@GET
	List< Something > listSomething( );
...
```

## Active Requests

Extends `ClientFilter` and `@Override` the `auditRequestRespons` method:

```java
public class MyAPIClientFilter extends ClientFilter {

	@Override
	public void auditRequestRespons( RequestDTO request, ResponseDTO response ) {
		// WHATEVER YOU WANT FROM REQ OR RES
	}
}
```

E.g. persisting request/response in database as an audition entity:

```java
import java.util.List;
import br.com.potio.core.dto.AuditionDTO;
import br.com.potio.core.dto.RequestDTO;
import br.com.potio.core.dto.ResponseDTO;
import br.com.potio.http_auditor.ClientFilter;
...

@JBossLog
public class MyAPIClientFilter extends ClientFilter {

	@Inject
	PersistAudition persistAudition;

	@Override
	public void auditRequestRespons( RequestDTO request, ResponseDTO response ) {
		log.infov( "Saving Request and Response from {0}", request.getMethodName() + " - " + request.getUrl() );
		List< String > originActionHeader = request.getHeaders().get( ClientFilter.HEADER_ORIGIN_ACTION );
		String[] originActions = originActionHeader.get( 0 ).split( "-" );
		String method = originActions[ 0 ];
		String birCode = originActions[ 1 ];
		AuditionDTO audition = AuditionDTO.builder()
				.withIdAccount( birCode )
				.withDescription( method )
				.withRequestBody( request.getBody() )
				.withRequestMethod( request.getMethodName() )
				.withHeaders( request.getHeaders().toString() )
				.withRequestUrl( request.getUrl() )
				.withResponseBody( response.getBody() )
				.withResponseStatus( response.getStatus() )
				.withTookSeconds( response.getTookSeconds() )
				.build();
		this.persistAudition.publish( audition );
	}

}
```

Do this to each http client you need or want.

Inject `MyAPIClientFilter` provider in your client interface with `@RegisterProvider`:

```java
@RegisterProvider( MyAPIClientFilter.class )
```

E.g. using both myFilter and logging filter:

```java
...
@Path( "/my-api" )
@RegisterProviders( {
		@RegisterProvider( LoggingFilter.class ),
		@RegisterProvider( MyAPIClientFilter.class ) } )
public interface IAPIClient {

	@GET
	List< Something > listSomething( );
...
```

## Passive Requests

Very similar to active request, but most of the time with less infos.

Extends `ServerFilter` and `@Override` the `auditRequestRespons` method:

```java
public class MyServiceFilter extends ServerFilter {

	@Override
	public void auditRequestRespons( RequestDTO request, ResponseDTO response ) {
		// WHATEVER YOU WANT FROM REQ OR RES
	}
}
```

E.g. persisting request/response in database as an audition entity:

```java
import java.util.List;
import br.com.potio.core.dto.AuditionDTO;
import br.com.potio.core.dto.RequestDTO;
import br.com.potio.core.dto.ResponseDTO;
import br.com.potio.http_auditor.ClientFilter;
...

public class ConnectorServerFilter extends ServerFilter {

	@Inject
	PersistAudition persistAudition;

	@Override
	public void auditRequestRespons( RequestDTO request, ResponseDTO response ) {
		AuditionDTO audition = AuditionDTO.builder()
				.withRequestBody( request.getBody() )
				.withRequestMethod( request.getMethodName() )
				.withRequestUrl( request.getUrl() )
				.withHeaders( request.getHeaders().toString() )
				.withResponseBody( response.getBody() )
				.withResponseStatus( response.getStatus() )
				.withTookSeconds( response.getTookSeconds() )
				.build();
		this.persistAudition.publish( audition );
	}

}
```

Do this to each http client you need or want.

Inject `ConnectorServerFilter` provider in your client interface with `@RegisterProvider`:

```java
@RegisterProvider( ConnectorServerFilter.class )
```

E.g. using both custom filter in entry endpoints:

```java
...
@Path( "/api/user" )
@RolesAllowed( "ADMIN" )
@SecurityScheme( securitySchemeName = "Basic Auth", type = SecuritySchemeType.HTTP, scheme = "basic" )
@SecurityRequirement( name = "Basic Auth" )
@RegisterProviders( @RegisterProvider( ConnectorServerFilter.class ) )
public class UserResource {

	@POST
	public Response createUser( @RequestBody User user ) {
		return Response.ok().build();
	}

}
...
```




