package it.eng.dome.billing.engine.exception;

import java.net.ConnectException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {
	
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return buildResponseEntity(new BillingException(HttpStatus.BAD_REQUEST, ex));
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	protected ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
		return buildResponseEntity(new BillingException(HttpStatus.BAD_REQUEST, ex));
	}
	
	@ExceptionHandler(IllegalStateException.class)
	protected ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex) {
		return buildResponseEntity(new BillingException(HttpStatus.BAD_REQUEST, ex));
	}
	
	@ExceptionHandler(ConnectException.class)
	protected ResponseEntity<Object> handleConnectionException(ConnectException ex) {
		return buildResponseEntity(new BillingException(HttpStatus.GATEWAY_TIMEOUT, ex));
	}

	private ResponseEntity<Object> buildResponseEntity(BillingException billingException) {
		return new ResponseEntity<>(billingException, billingException.getStatus());
	}
	
}
