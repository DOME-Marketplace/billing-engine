package it.eng.dome.billing.engine.exception;


import java.net.ConnectException;
import java.net.UnknownHostException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import it.eng.dome.brokerage.exception.ErrorResponse;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import jakarta.servlet.http.HttpServletRequest;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {
	
	@ExceptionHandler(BillingBadRequestException.class)
	protected ResponseEntity<Object> handleBillingBadRequestException(HttpServletRequest request,BillingBadRequestException ex) {
		return buildResponseEntity(new ErrorResponse(request,HttpStatus.BAD_REQUEST, ex));
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	protected ResponseEntity<Object> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException ex) {
		return buildResponseEntity(new ErrorResponse(request,HttpStatus.BAD_REQUEST, ex));
	}
	
	@ExceptionHandler(IllegalStateException.class)
	protected ResponseEntity<Object> handleIllegalStateException(HttpServletRequest request, IllegalStateException ex) {
		return buildResponseEntity(new ErrorResponse(request,HttpStatus.BAD_REQUEST, ex));
	}
	
	@ExceptionHandler(ConnectException.class)
	protected ResponseEntity<Object> handleConnectionException(HttpServletRequest request,ConnectException ex) {
		return buildResponseEntity(new ErrorResponse(request,HttpStatus.SERVICE_UNAVAILABLE, ex));
	}	
	
	@ExceptionHandler(UnknownHostException.class)
	protected ResponseEntity<Object> handleConnectionException(HttpServletRequest request,UnknownHostException ex) {
		return buildResponseEntity(new ErrorResponse(request,HttpStatus.SERVICE_UNAVAILABLE, ex));
	}
	
	@ExceptionHandler(ApiException.class)
	protected ResponseEntity<Object> handleIllegalStateException(HttpServletRequest request, ApiException ex) {
		return buildResponseEntity(new ErrorResponse(request,HttpStatus.SERVICE_UNAVAILABLE, ex));
	}
	
	@ExceptionHandler(it.eng.dome.tmforum.tmf678.v4.ApiException.class)
	protected ResponseEntity<Object> handleIllegalStateException(HttpServletRequest request, it.eng.dome.tmforum.tmf678.v4.ApiException ex) {
		return buildResponseEntity(new ErrorResponse(request, HttpStatus.SERVICE_UNAVAILABLE, ex));
	}
	
	@ExceptionHandler(it.eng.dome.tmforum.tmf622.v4.ApiException.class)
	protected ResponseEntity<Object> handleIllegalStateException(HttpServletRequest request, it.eng.dome.tmforum.tmf622.v4.ApiException ex) {
		return buildResponseEntity(new ErrorResponse(request, HttpStatus.SERVICE_UNAVAILABLE, ex));
	}

	private ResponseEntity<Object> buildResponseEntity(ErrorResponse errorResponse) {
		return new ResponseEntity<>(errorResponse, errorResponse.getStatus());
	}
	
}
