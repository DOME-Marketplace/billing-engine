package it.eng.dome.billing.engine.exception;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;

import lombok.Getter;

public class BillingException {
	
	@Getter
	private HttpStatus status;
	@Getter
	private String message;
	@Getter
	private OffsetDateTime date;
	
	public BillingException(HttpStatus status, Throwable exc) {
		this(status, exc.getMessage());
	}

	public BillingException(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
		date = OffsetDateTime.now();
	}
}
