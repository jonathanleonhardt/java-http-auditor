package br.com.potio.core.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class RequestDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String methodName;
	private String url;
	private String body;
	private String bodyTypeName;
	private Map< String, List< String > > headers;
	private Date date;

	public RequestDTO() {
	}

	public RequestDTO( String methodName, String url, String body, String bodyTypeName,
			Map< String, List< String > > headers, Date date ) {
		this.methodName = methodName;
		this.url = url;
		this.body = body;
		this.bodyTypeName = bodyTypeName;
		this.headers = headers;
		this.date = date;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName( String methodName ) {
		this.methodName = methodName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl( String url ) {
		this.url = url;
	}

	public String getBody() {
		return body;
	}

	public void setBody( String body ) {
		this.body = body;
	}

	public String getBodyTypeName() {
		return bodyTypeName;
	}

	public void setBodyTypeName( String bodyTypeName ) {
		this.bodyTypeName = bodyTypeName;
	}

	public Map< String, List< String > > getHeaders() {
		return headers;
	}

	public void setHeaders( Map< String, List< String > > headers ) {
		this.headers = headers;
	}

	public Date getDate() {
		return date;
	}

	public void setDate( Date date ) {
		this.date = date;
	}

	public static class Builder {
		private String methodName;
		private String url;
		private String body;
		private String bodyTypeName;
		private Map< String, List< String > > headers;
		private Date date;

		public Builder withMethodName( String methodName ) {
			this.methodName = methodName;
			return this;
		}

		public Builder withUrl( String url ) {
			this.url = url;
			return this;
		}

		public Builder withBody( String body ) {
			this.body = body;
			return this;
		}

		public Builder withBodyTypeName( String bodyTypeName ) {
			this.bodyTypeName = bodyTypeName;
			return this;
		}

		public Builder withHeaders( Map< String, List< String > > headers ) {
			this.headers = headers;
			return this;
		}

		public Builder withDate( Date date ) {
			this.date = date;
			return this;
		}

		public RequestDTO build() {
			return new RequestDTO( methodName, url, body, bodyTypeName, headers, date );
		}

	}

}
