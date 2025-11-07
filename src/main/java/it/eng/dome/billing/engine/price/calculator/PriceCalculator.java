package it.eng.dome.billing.engine.price.calculator;

import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.model.Money;

public interface PriceCalculator {
	
	public Money calculatePrice() throws BillingBadRequestException;
	
	//public void setBillingPeriod(TimePeriod billingPeriod);
	
	//public void setProduct(Product product);
	
	//public void setProductOfferingPrice(ProductOfferingPrice pop);
	
	//public TimePeriod getBillingPeriod();

}
