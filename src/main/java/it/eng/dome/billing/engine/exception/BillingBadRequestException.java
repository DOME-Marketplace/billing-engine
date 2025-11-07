package it.eng.dome.billing.engine.exception;

import lombok.Getter;

/**
 * Custom exception raised when the Billing Engine service will not process the request due to something that is perceived to be
 * a client error 
 */
public class BillingBadRequestException extends Exception{
	

	private static final long serialVersionUID = 1L;
	@Getter
	private String message;
	
	public BillingBadRequestException() {
		super();
	}
	
	public BillingBadRequestException(String msg) {
		super(msg);
	    this.message = msg;
	}
}
