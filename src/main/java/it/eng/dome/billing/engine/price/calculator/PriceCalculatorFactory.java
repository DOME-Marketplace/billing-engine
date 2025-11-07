package it.eng.dome.billing.engine.price.calculator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;

@Component
public class PriceCalculatorFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(PriceCalculatorFactory.class);
	
	public static PriceCalculator getPriceCalculatorFor(@NotNull ProductOfferingPrice pop, @NotNull Product prod, @NotNull TimePeriod billingPeriod) throws BillingBadRequestException {
		logger.debug("*************** Price Calculator FACTORY **************");
        PriceCalculator pc=null;
        
		if(ProductOfferingPriceUtils.isPriceTypeUsage(pop)) {
			if(billingPeriod==null)
				throw new BillingBadRequestException(String.format("Error getting PriceCalculator: the POP '{}' with priceType Usage requires a not null billingPeriod to get Usage data", pop.getId()));
			pc=getUsagePriceCalculator(pop,prod,billingPeriod);
		}
		
		return pc;
	}

	private static PriceCalculator getUsagePriceCalculator(@NotNull ProductOfferingPrice pop, @NotNull Product prod, @NotNull TimePeriod billingPeriod) {
		logger.debug("Creating UsagePriceCalculator for POP '{}'", pop.getId());
		return new UsagePriceCalculator(pop,prod,billingPeriod);
	}

}
