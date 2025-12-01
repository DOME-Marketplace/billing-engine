package it.eng.dome.billing.engine.price.alteration;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.tmforum.tmf620.v4.model.Money;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;

@Component(value = "discountAlterationOperation")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DiscountAlterationOperation implements PriceAlterationOperation{
    private final Logger logger = LoggerFactory.getLogger(DiscountAlterationOperation.class);
    
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    
    @Autowired
	private TMFEntityValidator tmfEntityValidator;

	@Override
	public BigDecimal applyAlteration(BigDecimal basePrice, ProductOfferingPrice alterationPOP, Float quantity) throws BillingEngineValidationException {
		BigDecimal discount =BigDecimal.ZERO;
		
		// If percentage is set
		if (alterationPOP.getPercentage() != null) {
            discount = basePrice
                    .multiply(new BigDecimal(String.valueOf(alterationPOP.getPercentage())))
                    .divide(ONE_HUNDRED)
                    .setScale(2, RoundingMode.HALF_EVEN);
            
            logger.info("Applied {}% discount to base price '{}'. Discount '{}'", 
    				alterationPOP.getPercentage(), basePrice, discount.abs().negate());
        } 
		// if the percentage is not set will be considered as discount the price of the pop alteration
		else {
        	Money price=alterationPOP.getPrice();
        	if(price!=null) {
        		tmfEntityValidator.validatePrice(alterationPOP);
        		
        		discount=new BigDecimal(String.valueOf(price.getValue()));
        		logger.info("Discount applied to base price '{}': Discount '{}'", 
        				basePrice, discount.abs().negate());
        	}
        }
		
		if(quantity!=null && quantity>=0f) {
			discount.multiply(new BigDecimal(String.valueOf(quantity)));
			logger.info("Quantity is {}: Total Discount {}", quantity, discount.abs().negate());
		}
			
		
		return discount.abs().negate();
	}
	
	

}
