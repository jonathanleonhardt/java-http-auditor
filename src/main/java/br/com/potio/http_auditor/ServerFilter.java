package br.com.potio.http_auditor;

import java.io.BufferedInputStream;
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
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
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
	private static final String HEADER_AUDITION_ENTITY = "audition-entity";
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	private static final String DATE_PATTERN = "EEE MMM d HH:mm:ss yyyy";
	protected static final ZoneId DEFAULT_ZONE_ID = ZoneId.of( "America/Sao_Paulo" );

	public void auditRequestResponse( RequestDTO request, ResponseDTO response ) {
		throw new UnsupportedOperationException( "Persist Audition Not Implemented" );
	}

	@Override
	@Transactional( value = TxType.REQUIRES_NEW )
	public void filter( ContainerRequestContext requestContext ) throws IOException {
		try {
			var request = this.createRequest( requestContext );
			requestContext.getHeaders().add( HEADER_AUDITION_ENTITY, jsonb.toJson( request ) );
		} catch ( IOException | ParseException e) {
			ServerFilter.logger.log( Level.SEVERE, "Error while intercepting client requests", e );
		}
	}

	@Override
	@Transactional( value = TxType.REQUIRES_NEW )
	public void filter( ContainerRequestContext requestContext, ContainerResponseContext responseContext ) throws IOException {
		try {
			Map< String, List< String > > headersRequest = this.extractHeadersRequest( requestContext.getHeaders() );
			if ( !headersRequest.containsKey( HEADER_AUDITION_ENTITY ) ) {
				return;
			}
			String auditionEntityJson = requestContext.getHeaders().get( HEADER_AUDITION_ENTITY ).get( 0 );
			var request = jsonb.fromJson( auditionEntityJson , RequestDTO.class );
			var response = this.createResponse( responseContext );

			Long tookSeconds = null;
			if ( !Objects.isNull( response.getDate() ) && !Objects.isNull( request.getDate() ) ) {
				tookSeconds = Math.abs( response.getDate().getTime() - request.getDate().getTime() );
				tookSeconds = TimeUnit.MILLISECONDS.toSeconds( tookSeconds );
			}
			response.setTookSeconds( Objects.isNull( tookSeconds ) ? tookSeconds + "s" : "< 1s" );

			this.auditRequestResponse( request, response );
		} catch ( IOException | ParseException e) {
			ServerFilter.logger.log( Level.SEVERE, "Error while intercepting client requests", e );
		}
	}

	private Map< String, List< String > > extractHeadersRequest( MultivaluedMap< String, String > multivaluedHeaderMap ) {
		Map< String, List< String > > headers = new HashMap<>();
		multivaluedHeaderMap.keySet().stream()
				.forEach( key -> headers.put( key, multivaluedHeaderMap.get( key ) ) );
		return headers;
	}

	private RequestDTO createRequest( ContainerRequestContext context ) throws IOException, ParseException {
		Map< String, List< String > > headers = this.extractHeadersRequest( context.getHeaders() );
		var uri = context.getUriInfo().getRequestUri();
		InputStream is = context.getEntityStream();
		InputStream inputStream = !is.markSupported()
				? new BufferedInputStream( is )
				: is;
		inputStream.mark( inputStream.available() + 1 );
		byte[] data = inputStream.readAllBytes();
		inputStream.reset();
		context.setEntityStream( inputStream );
		String requestBody = new String( data, DEFAULT_CHARSET );
		return RequestDTO.builder()
				.withUrl( uri.toString() )
				.withBody( requestBody )
				.withMethodName( context.getMethod() )
				.withHeaders( headers )
				.withDate( new Date() )
				.build();
	}

	private ResponseDTO createResponse( ContainerResponseContext context ) throws IOException, ParseException {
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