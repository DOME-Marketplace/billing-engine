package it.eng.dome.billing.engine.price.calculator;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.tmforum.tmf620.v4.ApiException;


public interface PriceCalculator {
	
	public Money calculatePrice() throws BillingEngineValidationException, ApiException;

}
