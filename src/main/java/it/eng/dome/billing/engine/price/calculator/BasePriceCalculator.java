package it.eng.dome.billing.engine.price.calculator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf637.v4.model.Product;

@Component
public class BasePriceCalculator extends AbstractPriceCalculator<Product,Money>{
	
	private final Logger logger = LoggerFactory.getLogger(BasePriceCalculator.class); 

	public BasePriceCalculator() {
		super();
	}

	@Override
	public Money calculatePrice(Product prod) throws BillingEngineValidationException, ApiException {	
		logger.info("Calculating base price for POP '{}' of Product '{}'", pop.getId(), prod.getId());
		
		Money totalAmountMoney=new Money(priceCurrency,pop.getPrice().getValue());
			
		logger.info("Price of ProductOfferingPrice '{}' = {} {}", 
					pop.getId(), totalAmountMoney.getValue(),priceCurrency);
								
		// apply price alterations
		if (ProductOfferingPriceUtils.hasRelationships(pop)) {
			Money alteretedPrice=priceAlterationCalculator.applyAlterations(totalAmountMoney,ProductOfferingPriceUtils.getProductOfferingPriceRelationships(pop.getPopRelationship(), productCatalogManagementApis));
								
			logger.info("Price of ProductOfferingPrice '{}' after alterations = {} {}", 
					pop.getId(), alteretedPrice.getValue(),priceCurrency);	
		
			return alteretedPrice;
		}
			
		return totalAmountMoney;
		
	}

	
}
