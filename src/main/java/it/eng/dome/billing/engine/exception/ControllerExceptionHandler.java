package it.eng.dome.billing.engine.exception;

import java.net.ConnectException;
import java.net.UnknownHostException;

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

import it.eng.dome.tmforum.tmf620.v4.ApiException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {
	
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return buildResponseEntity(new BillingExceptionMessage(HttpStatus.BAD_REQUEST, ex));
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	protected ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
		return buildResponseEntity(new BillingExceptionMessage(HttpStatus.BAD_REQUEST, ex));
	}
	
	@ExceptionHandler(IllegalStateException.class)
	protected ResponseEntity<Object> handleIllegalStateException(IllegalStateException ex) {
		return buildResponseEntity(new BillingExceptionMessage(HttpStatus.BAD_REQUEST, ex));
	}
	
	@ExceptionHandler(ConnectException.class)
	protected ResponseEntity<Object> handleConnectionException(ConnectException ex) {
		return buildResponseEntity(new BillingExceptionMessage(HttpStatus.SERVICE_UNAVAILABLE, ex));
	}	
	
	@ExceptionHandler(UnknownHostException.class)
	protected ResponseEntity<Object> handleConnectionException(UnknownHostException ex) {
		return buildResponseEntity(new BillingExceptionMessage(HttpStatus.SERVICE_UNAVAILABLE, ex));
	}
	
	@ExceptionHandler(ApiException.class)
	protected ResponseEntity<Object> handleIllegalStateException(ApiException ex) {
		return buildResponseEntity(new BillingExceptionMessage(HttpStatus.SERVICE_UNAVAILABLE, ex));
	}
	
	@ExceptionHandler(it.eng.dome.tmforum.tmf678.v4.ApiException.class)
	protected ResponseEntity<Object> handleIllegalStateException(it.eng.dome.tmforum.tmf678.v4.ApiException ex) {
		return buildResponseEntity(new BillingExceptionMessage(HttpStatus.SERVICE_UNAVAILABLE, ex));
	}
	
	@ExceptionHandler(it.eng.dome.tmforum.tmf622.v4.ApiException.class)
	protected ResponseEntity<Object> handleIllegalStateException(it.eng.dome.tmforum.tmf622.v4.ApiException ex) {
		return buildResponseEntity(new BillingExceptionMessage(HttpStatus.SERVICE_UNAVAILABLE, ex));
	}

	private ResponseEntity<Object> buildResponseEntity(BillingExceptionMessage billingException) {
		return new ResponseEntity<>(billingException, billingException.getStatus());
	}
	
}
