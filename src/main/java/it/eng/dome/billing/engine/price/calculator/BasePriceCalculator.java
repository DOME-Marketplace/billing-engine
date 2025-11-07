package it.eng.dome.billing.engine.price.calculator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;

public class BasePriceCalculator implements PriceCalculator{
	
	private final Logger logger = LoggerFactory.getLogger(BasePriceCalculator.class);
	private ProductOfferingPrice pop;
	
	private final String priceCurrency;
	private final String DEFAULT_CURRENCY = "EUR";
	
	@Autowired
	private PriceAlterationCalculator priceAlterationCalculator; 

	public BasePriceCalculator(ProductOfferingPrice pop) {
		super();
		this.pop = pop;
		
		if(this.pop.getPrice().getUnit()!=null && !this.pop.getPrice().getUnit().isEmpty())
			this.priceCurrency=this.pop.getPrice().getUnit();
		else
			this.priceCurrency=DEFAULT_CURRENCY;
	}

	@Override
	public Money calculatePrice() throws BillingEngineValidationException, ApiException {
		
		logger.info("Calculating base price for POP '{}'...", pop.getId());
		Money totalAmountMoney=new Money(priceCurrency,pop.getPrice().getValue());
			
		logger.info("Price of ProductOfferingPrice '{}' = {} {}", 
					pop.getId(), totalAmountMoney.getValue(),priceCurrency);
								
		// apply price alterations
		if (ProductOfferingPriceUtils.hasRelationships(pop)) {
			Money alteretedPrice=priceAlterationCalculator.applyAlterations(pop, totalAmountMoney);
								
			logger.info("Price of ProductOfferingPrice '{}' after alterations = {} {}", 
					pop.getId(), alteretedPrice.getValue(),priceCurrency);	
		
			return alteretedPrice;
		}
			
		return totalAmountMoney;
		
	}

	
}
