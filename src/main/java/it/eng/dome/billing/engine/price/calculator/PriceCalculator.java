package it.eng.dome.billing.engine.price.calculator;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;


public interface PriceCalculator<T,R> {

	public R calculatePrice(T target) throws BillingEngineValidationException, ApiException;
	
	public void setProductOfferingPrice(ProductOfferingPrice pop);

}
