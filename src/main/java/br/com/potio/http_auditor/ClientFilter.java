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
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.potio.core.dto.RequestDTO;
import br.com.potio.core.dto.ResponseDTO;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

@Provider
public abstract class ClientFilter implements ClientRequestFilter, ClientResponseFilter {

	private static final Logger logger = Logger.getLogger( ClientFilter.class.getName() );
	private static final int MAX_ENTITY_SIZE = 16 * 1024;
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	private static final String DATE_PATTERN = "EEE MMM d HH:mm:ss yyyy";
	private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of( "America/Sao_Paulo" );
	protected static final String HEADER_ORIGIN_ACTION = "origin-action";

	public void auditRequestResponse( RequestDTO request, ResponseDTO response ) {
		throw new UnsupportedOperationException( "Persist Audition Not Implemented" );
	}

	@Override
	@Transactional( value = TxType.REQUIRES_NEW )
	public void filter( ClientRequestContext requestContext, ClientResponseContext responseContext ) {
		try {
			var response = this.createResponse( responseContext );
			var request = this.createRequest( requestContext );
			Long tookSeconds = null;
			if ( !Objects.isNull( response.getDate() )
					&&  !Objects.isNull( request.getDate() ) ) {
				tookSeconds = Math.abs( response.getDate().getTime() - request.getDate().getTime() );
				tookSeconds = TimeUnit.MILLISECONDS.toSeconds( tookSeconds );
			}
			response.setTookSeconds( Objects.isNull( tookSeconds ) ? tookSeconds + "s" : "< 1s" );
			this.auditRequestResponse( request, response );
		} catch ( IOException | ParseException e) {
			ClientFilter.logger.log( Level.SEVERE, "Error while intercepting client requests", e );
		}
	}

	private RequestDTO createRequest( ClientRequestContext context ) throws IOException, ParseException {
		var uri = context.getUri();
		Map< String, List< String > > headers = extractHeaders( context.getStringHeaders() );
		String body = null;
		String bodyTypeName = null;
		Object entity = null;
		var hasEntity = context.hasEntity()
				&& !( ( entity = context.getEntity() ) instanceof Form );
		if ( hasEntity ) {
			Jsonb jsonb = JsonbBuilder.create();
			body = jsonb.toJson(entity);
			bodyTypeName = entity.getClass().getName();
		}
		String requestDate = headers.get( "date" ).get( 0 );
		var dateParser = new SimpleDateFormat( DATE_PATTERN, Locale.US );
		dateParser.setTimeZone( TimeZone.getTimeZone( DEFAULT_ZONE_ID ) );
		return RequestDTO.builder()
				.withUrl( uri.toString() )
				.withBody( body )
				.withBodyTypeName( bodyTypeName )
				.withMethodName( context.getMethod() )
				.withDate( dateParser.parse( requestDate ) )
				.withHeaders( headers )
				.build();
	}

	private ResponseDTO createResponse( ClientResponseContext context ) throws IOException {
		String body = null;
		String entityTag = null;
		if ( context.hasEntity() ) {
			InputStream entityStream = context.getEntityStream();
			var bodyBuilder = new StringBuilder();

			context.setEntityStream(
					inBoundEntity( entityStream, bodyBuilder ) );

			body = bodyBuilder.toString();
			entityTag = Optional.ofNullable( context.getEntityTag() )
					.map( EntityTag::getValue )
					.orElse( null );
		}
		return ResponseDTO.builder()
				.withStatus( context.getStatus() )
				.withBody( body )
				.withEntityTag( entityTag )
				.withDate( context.getDate() )
				.build();
	}

	private InputStream inBoundEntity( InputStream stream, StringBuilder builder ) throws IOException {
		if ( !stream.markSupported() ) {
			stream = new BufferedInputStream( stream );
		}
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			int nRead;
			byte[] data = new byte[MAX_ENTITY_SIZE];
			while ( ( nRead = stream.read ( data, 0, data.length ) ) != -1 ) {
				buffer.write( data, 0, nRead );
				builder.append( new String( data, 0, nRead, DEFAULT_CHARSET ) );
			}
			ByteArrayInputStream retorno = new ByteArrayInputStream( buffer.toByteArray() );
			retorno.mark(0);
			return retorno;
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	
		return new BufferedInputStream( stream );
	}

	Map< String, List< String > > extractHeaders( MultivaluedMap< String, String > multivaluedHeaderMap ) {
		Map< String, List< String > > headers = new HashMap<>();
		multivaluedHeaderMap.keySet().stream()
				.forEach( key -> headers.put( key, multivaluedHeaderMap.get( key ) ) );
		return headers;
	}

	@Override
	public void filter( ClientRequestContext requestContext ) throws IOException {
		var simpleDateFormat = new SimpleDateFormat( DATE_PATTERN, Locale.US );
		simpleDateFormat.setTimeZone( TimeZone.getTimeZone( DEFAULT_ZONE_ID ) );
		requestContext.getHeaders().add( "date", simpleDateFormat.format( new Date() ) );
	}

}
