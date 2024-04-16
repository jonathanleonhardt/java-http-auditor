package br.com.potio.core.dto;

public class AuditionDTO {

	private String id;
	private String idAccount;
	private String description;
	private String headers;
	private String requestBody;
	private String requestMethod;
	private String requestUrl;
	private String responseBody;
	private Integer responseStatus;
	private String tookSeconds;

	public AuditionDTO( String id, String idAccount, String description, String headers,
			String requestBody, String requestMethod, String requestUrl, String responseBody,
			Integer responseStatus, String tookSeconds ) {
		this.id = id;
		this.idAccount = idAccount;
		this.description = description;
		this.headers = headers;
		this.requestBody = requestBody;
		this.requestMethod = requestMethod;
		this.requestUrl = requestUrl;
		this.responseBody = responseBody;
		this.responseStatus = responseStatus;
		this.tookSeconds = tookSeconds;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getId() {
		return id;
	}

	public void setId( String id ) {
		this.id = id;
	}

	public String getIdAccount() {
		return idAccount;
	}

	public void setIdAccount( String idAccount ) {
		this.idAccount = idAccount;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription( String description ) {
		this.description = description;
	}

	public String getHeaders() {
		return headers;
	}

	public void setHeaders( String headers ) {
		this.headers = headers;
	}

	public String getRequestBody() {
		return requestBody;
	}

	public void setRequestBody( String requestBody ) {
		this.requestBody = requestBody;
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod( String requestMethod ) {
		this.requestMethod = requestMethod;
	}

	public String getRequestUrl() {
		return requestUrl;
	}

	public void setRequestUrl( String requestUrl ) {
		this.requestUrl = requestUrl;
	}

	public String getResponseBody() {
		return responseBody;
	}

	public void setResponseBody( String responseBody ) {
		this.responseBody = responseBody;
	}

	public Integer getResponseStatus() {
		return responseStatus;
	}

	public void setResponseStatus( Integer responseStatus ) {
		this.responseStatus = responseStatus;
	}

	public String getTookSeconds() {
		return tookSeconds;
	}

	public void setTookSeconds( String tookSeconds ) {
		this.tookSeconds = tookSeconds;
	}

	public static class Builder {
		private String id;
		private String idAccount;
		private String description;
		private String headers;
		private String requestBody;
		private String requestMethod;
		private String requestUrl;
		private String responseBody;
		private Integer responseStatus;
		private String tookSeconds;

		public Builder withId( String id ) {
			this.id = id;
			return this;
		}

		public Builder withIdAccount( String idAccount ) {
			this.idAccount = idAccount;
			return this;
		}

		public Builder withDescription( String description ) {
			this.description = description;
			return this;
		}

		public Builder withHeaders( String headers ) {
			this.headers = headers;
			return this;
		}

		public Builder withRequestBody( String requestBody ) {
			this.requestBody = requestBody;
			return this;
		}

		public Builder withRequestMethod( String requestMethod ) {
			this.requestMethod = requestMethod;
			return this;
		}

		public Builder withRequestUrl( String requestUrl ) {
			this.requestUrl = requestUrl;
			return this;
		}

		public Builder withResponseBody( String responseBody ) {
			this.responseBody = responseBody;
			return this;
		}

		public Builder withResponseStatus( Integer responseStatus ) {
			this.responseStatus = responseStatus;
			return this;
		}

		public Builder withTookSeconds( String tookSeconds ) {
			this.tookSeconds = tookSeconds;
			return this;
		}

		public AuditionDTO build() {
			return new AuditionDTO( id, idAccount, description, headers, requestBody, requestMethod,
					requestUrl, responseBody, responseStatus, tookSeconds );
		}
	}

}
