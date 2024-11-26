package it.eng.dome.billing.engine.exception;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;

import lombok.Getter;

public class BillingExceptionMessage {
	
	@Getter
	private int code;
	@Getter
	private HttpStatus status;
	@Getter
	private String message;
	@Getter
	private OffsetDateTime date;
	
	public BillingExceptionMessage(HttpStatus status, Throwable exc) {
		this(status, exc.getMessage());
	}

	public BillingExceptionMessage(HttpStatus status, String message) {
		this.status = status;
		this.code = status.value();
		this.message = message;
		date = OffsetDateTime.now();
	}
}
