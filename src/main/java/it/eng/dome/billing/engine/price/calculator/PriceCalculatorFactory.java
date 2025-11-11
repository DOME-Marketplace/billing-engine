package it.eng.dome.billing.engine.price.calculator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.validator.ValidationIssue;
import it.eng.dome.billing.engine.validator.ValidationIssueSeverity;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;

@Component
public class PriceCalculatorFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(PriceCalculatorFactory.class);
	
	public static PriceCalculator getPriceCalculatorFor(@NotNull ProductOfferingPrice pop, @NotNull Product prod, @NotNull TimePeriod billingPeriod) throws BillingEngineValidationException {
		logger.debug("*************** Price Calculator FACTORY **************");
        PriceCalculator pc=null;
        
        if(ProductOfferingPriceUtils.isBundled(pop)) {
        	pc=getBundledPriceCalculator(pop, prod, billingPeriod);
        }else {
        	if(ProductOfferingPriceUtils.isPriceTypeUsage(pop)) {
    			pc=getUsagePriceCalculator(pop,prod,billingPeriod);
    		}else {
    			if(ProductOfferingPriceUtils.hasProdSpecCharValueUses(pop)) {
    				pc=getCharacteristicPriceCalculator(pop, prod);
    			}else {
    				pc=getBasePriceCalculator(pop);
    			}
    		}
        }
		return pc;
	}

	private static PriceCalculator getUsagePriceCalculator(@NotNull ProductOfferingPrice pop, @NotNull Product prod, @NotNull TimePeriod billingPeriod) throws IllegalArgumentException{
		logger.debug("Creating UsagePriceCalculator for POP '{}'", pop.getId());
		if(billingPeriod==null)
			throw new IllegalArgumentException(String.format("Error getting UsagePriceCalculator: the POP '{}' with priceType Usage requires a not null billingPeriod to get Usage data", pop.getId()));
		return new UsagePriceCalculator(pop,prod,billingPeriod);
	}
	
	private static PriceCalculator getBasePriceCalculator(@NotNull ProductOfferingPrice pop) {
		logger.debug("Creating BasePriceCalculator for POP '{}'", pop.getId());
		return new BasePriceCalculator(pop);
	}
	
	private static PriceCalculator getCharacteristicPriceCalculator(@NotNull ProductOfferingPrice pop, @NotNull Product prod) throws BillingEngineValidationException {
		logger.debug("Creating CharacteristicPriceCalculator for POP '{}'", pop.getId());
		if(prod.getProductCharacteristic()==null ||prod.getProductCharacteristic().isEmpty()) {
			String msg=String.format("Error getting CharacteristicPriceCalculator: the ProductCharacteristic are missing in Product '{}'", prod.getId());
			ValidationIssue issue=new ValidationIssue(msg, ValidationIssueSeverity.ERROR);
			throw new BillingEngineValidationException(issue);
		}
		return new CharacteristicPriceCalculator(pop, prod);
	}
	
	private static PriceCalculator getBundledPriceCalculator(@NotNull ProductOfferingPrice pop, @NotNull Product prod, @NotNull TimePeriod billingPeriod) {
		logger.debug("Creating BundledPriceCalculator for POP '{}'", pop.getId());
		
		return new BundledPriceCalculator(pop, prod, billingPeriod);
	}


}
