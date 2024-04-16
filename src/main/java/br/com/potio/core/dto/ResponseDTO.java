package br.com.potio.core.dto;

import java.io.Serializable;
import java.util.Date;

public class ResponseDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer status;
	private String body;
	private String entityTag;
	private Date date;
	private String tookSeconds;

	public ResponseDTO() {
	}

	public ResponseDTO( Integer status, String body, String entityTag, Date date, String tookSeconds ) {
		this.status = status;
		this.body = body;
		this.entityTag = entityTag;
		this.date = date;
		this.tookSeconds = tookSeconds;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus( Integer status ) {
		this.status = status;
	}

	public String getBody() {
		return body;
	}

	public void setBody( String body ) {
		this.body = body;
	}

	public String getEntityTag() {
		return entityTag;
	}

	public void setEntityTag( String entityTag ) {
		this.entityTag = entityTag;
	}

	public Date getDate() {
		return date;
	}

	public void setDate( Date date ) {
		this.date = date;
	}

	public String getTookSeconds() {
		return tookSeconds;
	}

	public void setTookSeconds( String tookSeconds ) {
		this.tookSeconds = tookSeconds;
	}

	public static class Builder {
		private Integer status;
		private String entityTag;
		private String body;
		private String tookSeconds;
		private Date date;

		public Builder withStatus( Integer status ) {
			this.status = status;
			return this;
		}

		public Builder withEntityTag( String entityTag ) {
			this.entityTag = entityTag;
			return this;
		}

		public Builder withTookSeconds( String tookSeconds ) {
			this.tookSeconds = tookSeconds;
			return this;
		}

		public Builder withBody( String body ) {
			this.body = body;
			return this;
		}

		public Builder withDate( Date date ) {
			this.date = date;
			return this;
		}

		public ResponseDTO build() {
			return new ResponseDTO( status, body, entityTag, date, tookSeconds );
		}
	}

}
