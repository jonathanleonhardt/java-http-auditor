package br.com.potio.http_auditor;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer.Form;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.potio.core.dto.RequestDTO;
import br.com.potio.core.dto.ResponseDTO;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.bind.Jsonb;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

@Provider
public abstract class ServerFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static final Logger logger = Logger.getLogger( ServerFilter.class.getName() );
	private static final Jsonb jsonb = CDI.current().select( Jsonb.class ).get();
	private static final String HEADER_ENTITY = "audition-entity";
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	private static final String DATE_PATTERN = "EEE MMM d HH:mm:ss yyyy";
	protected static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

	public void auditRequestResponse( RequestDTO request, ResponseDTO response ) {
		throw new UnsupportedOperationException( "Persist Audition Not Implemented" );
	}

	@Override
	public void filter( ContainerRequestContext requestContext ) throws IOException {
		try {
			var request = this.createRequest( requestContext );
			requestContext.getHeaders().add( HEADER_ENTITY, jsonb.toJson( request ) );
		} catch ( IOException | ParseException e ) {
			ServerFilter.logger.log( Level.SEVERE, "Error while intercepting client requests", e );
		}
	}

	@Override
	public void filter( ContainerRequestContext reqContext, ContainerResponseContext resContext ) throws IOException {
		try {
			Map< String, List< String > > headersRequest = this.extractHeaders( reqContext.getHeaders() );
			if ( !headersRequest.containsKey( HEADER_ENTITY ) ) {
				return;
			}
			String auditionEntityJson = reqContext.getHeaders().get( HEADER_ENTITY ).get( 0 );
			var request = jsonb.fromJson( auditionEntityJson, RequestDTO.class );
			var response = this.createResponse( resContext );

			Long tookSeconds = null;
			if ( !Objects.isNull( response.getDate() ) && !Objects.isNull( request.getDate() ) ) {
				tookSeconds = Math.abs( response.getDate().getTime() - request.getDate().getTime() );
				tookSeconds = TimeUnit.MILLISECONDS.toSeconds( tookSeconds );
			}
			response.setTookSeconds( Objects.isNull( tookSeconds ) ? tookSeconds + "s" : "< 1s" );

			this.auditRequestResponse( request, response );
		} catch ( IOException | ParseException e ) {
			ServerFilter.logger.log( Level.SEVERE, "Error while intercepting client requests", e );
		}
	}

	private Map< String, List< String > > extractHeaders( MultivaluedMap< String, String > headerMap ) {
		Map< String, List< String > > headers = new HashMap<>();
		headerMap.keySet().stream()
				.forEach( key -> headers.put( key, headerMap.get( key ) ) );
		return headers;
	}

	private RequestDTO createRequest( ContainerRequestContext context ) throws IOException, ParseException {
		Map< String, List< String > > headers = this.extractHeaders( context.getHeaders() );
		var uri = context.getUriInfo().getRequestUri();
		InputStream is = context.getEntityStream();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] chunk = new byte[ 8192 ];
		int bytesRead;

		while ( ( bytesRead = is.read( chunk ) ) != -1 ) {
			buffer.write( chunk, 0, bytesRead );
		}

		byte[] data = buffer.toByteArray();

		ByteArrayInputStream newInputStream = new ByteArrayInputStream( data );

		context.setEntityStream( newInputStream );

		String requestBody = new String( data, DEFAULT_CHARSET );
		return RequestDTO.builder()
				.withUrl( uri.toString() )
				.withBody( requestBody )
				.withMethodName( context.getMethod() )
				.withHeaders( headers )
				.withDate( new Date() )
				.build();
	}

	private ResponseDTO createResponse( ContainerResponseContext context )
			throws IOException, ParseException {
		String body = null;
		Object entity = null;
		var hasEntity = context.hasEntity()
				&& !( ( entity = context.getEntity() ) instanceof Form );
		if ( hasEntity && entity instanceof String ) {
			body = ( String ) entity;
		} else if ( hasEntity && entity instanceof InputStream ) {
			try ( InputStream inputStream = ( InputStream ) entity ;) {
				byte[] bytes = inputStream.readAllBytes();
				body = new String( bytes, StandardCharsets.UTF_8 );
			} catch ( IOException e ) {
				ServerFilter.logger.log( Level.SEVERE, "Erro converting request body", e );
			}
		}
		var simpleDateFormat = new SimpleDateFormat( DATE_PATTERN, Locale.US );
		String formattedDate = simpleDateFormat.format( new Date() );
		Date responseDate = simpleDateFormat.parse( formattedDate );
		return ResponseDTO.builder()
				.withStatus( context.getStatus() )
				.withBody( body )
				.withDate( responseDate )
				.build();
	}

}