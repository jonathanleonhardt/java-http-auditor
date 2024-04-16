package br.com.potio.http_auditor;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;

/**
 * @see <a href=
 *      "https://stackoverflow.com/questions/55550652/how-do-i-debug-a-quarkus-smallrye-client-request/56015300#56015300">
 *      Based on stackoverflow answers</a>
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ClientRequestFilter,
			ContainerResponseFilter, ClientResponseFilter, WriterInterceptor {

	private static final Logger logger = Logger.getLogger( LoggingFilter.class.getName() );

	private static final String NOTIFICATION_PREFIX = "* ";

	private static final String REQUEST_PREFIX = "> ";

	private static final String RESPONSE_PREFIX = "< ";

	private static final String ENTITY_LOGGER_PROPERTY = LoggingFilter.class
			.getName() + ".entityLogger";

	private static final String LOGGING_ID_PROPERTY = LoggingFilter.class
			.getName() + ".id";

	private static final Comparator< Map.Entry< String, List< String > > > COMPARATOR = (
			o1, o2 ) -> o1.getKey().compareToIgnoreCase( o2.getKey() );

	private static final int DEFAULT_MAX_ENTITY_SIZE = 8 * 1024;

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private final AtomicLong atomicId = new AtomicLong( 0 );

	private final int maxEntitySize;

	public LoggingFilter() {

		this.maxEntitySize = LoggingFilter.DEFAULT_MAX_ENTITY_SIZE;
	}

	private void log( final StringBuilder stringBuilder ) {

		LoggingFilter.logger.info( stringBuilder.toString() );
	}

	private StringBuilder prefixId( final StringBuilder stringBuilder, final long id ) {

		stringBuilder.append( id ).append( " " );
		return stringBuilder;
	}

	private void printRequestLine( final StringBuilder stringBuilder, final String note,
			final long id, final String method, final URI uri ) {

		this.prefixId( stringBuilder, id ).append( LoggingFilter.NOTIFICATION_PREFIX )
				.append( note )
				.append( " on thread " )
				.append( Thread.currentThread().getName() )
				.append( "\n" );
		this.prefixId( stringBuilder, id ).append( LoggingFilter.REQUEST_PREFIX )
				.append( method ).append( " " )
				.append( uri.toASCIIString() ).append( "\n" );
	}

	private void printResponseLine( final StringBuilder b, final String note,
			final long id, final int status ) {

		this.prefixId( b, id ).append( LoggingFilter.NOTIFICATION_PREFIX )
				.append( note )
				.append( " on thread " )
				.append( Thread.currentThread().getName() ).append( "\n" );
		this.prefixId( b, id ).append( LoggingFilter.RESPONSE_PREFIX )
				.append( status )
				.append( "\n" );
	}

	private void printPrefixedHeaders( final StringBuilder stringBuilder,
			final long id,
			final String prefix,
			final MultivaluedMap< String, String > headers ) {

		if ( headers == null || headers.isEmpty() ) {
			return;
		}
		for ( final Map.Entry< String, List< String > > headerEntry : this
				.getSortedHeaders( headers.entrySet() ) ) {
			final List< ? > val = headerEntry.getValue();
			final String header = headerEntry.getKey();

			if ( val.size() == 1 ) {
				this.prefixId( stringBuilder, id ).append( prefix ).append( header )
						.append( ": " ).append( val.get( 0 ) ).append( "\n" );
			} else {
				final var sb = new StringBuilder();
				var add = false;
				for ( final Object s : val ) {
					if ( add ) {
						sb.append( ',' );
					}
					add = true;
					sb.append( s );
				}
				this.prefixId( stringBuilder, id ).append( prefix ).append( header )
						.append( ": " ).append( sb.toString() ).append( "\n" );
			}
		}
	}

	private Set< Map.Entry< String, List< String > > > getSortedHeaders(
			final Set< Map.Entry< String, List< String > > > headers ) {

		final TreeSet< Map.Entry< String, List< String > > > sortedHeaders = new TreeSet<>(
				LoggingFilter.COMPARATOR );
		sortedHeaders.addAll( headers );
		return sortedHeaders;
	}

	private InputStream logInboundEntity( final StringBuilder stringBuilder,
			InputStream stream, final Charset charset ) throws IOException {
		if ( !stream.markSupported() ) {
			stream = new BufferedInputStream( stream );
		}
		stream.mark( this.maxEntitySize + 1 );
		final var entity = new byte[ this.maxEntitySize + 1 ];
		final int entitySize = stream.read( entity );
		stringBuilder.append( new String( entity, 0,
				Math.min( entitySize, this.maxEntitySize ), charset ) );
		if ( entitySize > this.maxEntitySize ) {
			stringBuilder.append( "...more..." );
		}
		stringBuilder.append( '\n' );
		stream.reset();
		return stream;
	}

	@Override
	public void filter( final ClientRequestContext context )
			throws IOException {

		final long id = this.atomicId.incrementAndGet();
		context.setProperty( LoggingFilter.LOGGING_ID_PROPERTY, id );

		final var b = new StringBuilder();

		this.printRequestLine( b, "Sending client request", id,
				context.getMethod(), context.getUri() );
		this.printPrefixedHeaders( b, id, LoggingFilter.REQUEST_PREFIX,
				context.getStringHeaders() );

		if ( context.hasEntity() ) {
			try {
				final OutputStream stream = new LoggingFilter.LoggingStream( b,
						context.getEntityStream() );
				context.setEntityStream( stream );
				context.setProperty( LoggingFilter.ENTITY_LOGGER_PROPERTY, stream );
			} catch( Exception e ) {
				logger.info( e.getMessage() );
			}
		} else {
			this.log( b );
		}
	}

	@Override
	public void filter( final ClientRequestContext requestContext,
			final ClientResponseContext responseContext )
			throws IOException {

		final Object requestId = requestContext
				.getProperty( LoggingFilter.LOGGING_ID_PROPERTY );
		final long id = requestId != null ? ( Long ) requestId
				: this.atomicId.incrementAndGet();

		final var b = new StringBuilder();

		this.printResponseLine( b, "Client response received", id,
				responseContext.getStatus() );
		this.printPrefixedHeaders( b, id, LoggingFilter.RESPONSE_PREFIX,
				responseContext.getHeaders() );

		if ( responseContext.hasEntity() ) {
			responseContext.setEntityStream(
					this.logInboundEntity( b, responseContext.getEntityStream(),
							DEFAULT_CHARSET ) );
		}

		this.log( b );
	}

	@Override
	public void filter( final ContainerRequestContext context )
			throws IOException {

		final long id = this.atomicId.incrementAndGet();
		context.setProperty( LoggingFilter.LOGGING_ID_PROPERTY, id );

		final var b = new StringBuilder();

		this.printRequestLine( b, "Server has received a request", id,
				context.getMethod(), context.getUriInfo().getRequestUri() );
		this.printPrefixedHeaders( b, id, LoggingFilter.REQUEST_PREFIX,
				context.getHeaders() );

		if ( context.hasEntity() ) {
			context.setEntityStream(
					this.logInboundEntity( b, context.getEntityStream(),
							DEFAULT_CHARSET ) );
		}

		this.log( b );
	}

	@Override
	public void filter( final ContainerRequestContext requestContext,
			final ContainerResponseContext responseContext )
			throws IOException {

		final Object requestId = requestContext
				.getProperty( LoggingFilter.LOGGING_ID_PROPERTY );
		final long id = requestId != null ? ( Long ) requestId
				: this.atomicId.incrementAndGet();

		final var b = new StringBuilder();

		this.printResponseLine( b, "Server responded with a response", id,
				responseContext.getStatus() );
		this.printPrefixedHeaders( b, id, LoggingFilter.RESPONSE_PREFIX,
				responseContext.getStringHeaders() );

		if ( responseContext.hasEntity() ) {
			final OutputStream stream = new LoggingFilter.LoggingStream( b,
					responseContext.getEntityStream() );
			responseContext.setEntityStream( stream );
			requestContext.setProperty( LoggingFilter.ENTITY_LOGGER_PROPERTY,
					stream );
		} else {
			this.log( b );
		}
	}

	@Override
	public void aroundWriteTo(
			final WriterInterceptorContext writerInterceptorContext )
			throws IOException, WebApplicationException {

		final LoggingFilter.LoggingStream stream = ( LoggingFilter.LoggingStream ) writerInterceptorContext
				.getProperty( LoggingFilter.ENTITY_LOGGER_PROPERTY );
		writerInterceptorContext.proceed();
		if ( stream != null ) {
			this.log( stream.getStringBuilder( DEFAULT_CHARSET ) );
		}
	}

	public class LoggingStream extends FilterOutputStream {

		private final StringBuilder stringBuilder;

		private final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();

		LoggingStream( final StringBuilder stringBuilder, final OutputStream inner ) {

			super( inner );

			this.stringBuilder = stringBuilder;
		}

		StringBuilder getStringBuilder( final Charset charset ) {
			final byte[] entity = this.byteArrayStream.toByteArray();

			this.stringBuilder.append( new String( entity, 0,
					Math.min( entity.length, LoggingFilter.this.maxEntitySize ),
					charset ) );
			if ( entity.length > LoggingFilter.this.maxEntitySize ) {
				this.stringBuilder.append( "...more..." );
			}
			this.stringBuilder.append( '\n' );

			return this.stringBuilder;
		}

		@Override
		public void write( final int byteCode ) throws IOException {

			if ( this.byteArrayStream.size() <= LoggingFilter.this.maxEntitySize ) {
				this.byteArrayStream.write( byteCode );
			}
			this.out.write( byteCode );
		}

		@Override
		public void write( byte[] arrBytesToWrite, int startOffset, int numBytesToWrite ) throws IOException {
			if ( this.byteArrayStream.size() <= LoggingFilter.this.maxEntitySize ) {
				byteArrayStream.write( arrBytesToWrite, startOffset, numBytesToWrite );
			}
			this.out.write( arrBytesToWrite, startOffset, numBytesToWrite );
		}

	}

}