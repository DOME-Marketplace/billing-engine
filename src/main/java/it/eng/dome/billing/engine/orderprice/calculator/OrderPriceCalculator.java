package it.eng.dome.billing.engine.orderprice.calculator;

import java.util.List;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;

public interface OrderPriceCalculator {
	
	List<OrderPrice> calculateOrderPrice() throws BillingEngineValidationException, ApiException;

}
