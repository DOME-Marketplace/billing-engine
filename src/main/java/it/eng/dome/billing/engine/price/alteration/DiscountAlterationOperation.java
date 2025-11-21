package it.eng.dome.billing.engine.price.alteration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.utils.TmfConverter;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.tmforum.tmf620.v4.model.Money;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingTerm;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
import jakarta.validation.constraints.NotNull;
import kotlin.contracts.ReturnsNotNull;

@Component(value = "discountAlterationOperation")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DiscountAlterationOperation implements PriceAlterationOperation{
    private final Logger logger = LoggerFactory.getLogger(DiscountAlterationOperation.class);
    
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    
    @Autowired
	private TMFEntityValidator tmfEntityValidator;

	//@Override
	/*public PriceAlteration applyAlteration(float basePrice,  @NotNull ProductOfferingPrice alterationPOP) {
		BigDecimal discount = new BigDecimal(basePrice * (alterationPOP.getPercentage() / 100));
		discount = discount.setScale(2, RoundingMode.HALF_EVEN);
		
		Float discountedAmount=(basePrice - discount.floatValue()) < 0 ? 0 : basePrice - discount.floatValue();
		Money discounteMoney = new Money(alterationPOP.getPrice().getUnit(),discountedAmount);
		
		Price price = new Price();

		price.setDutyFreeAmount(TmfConverter.convertMoneyTo622(discounteMoney));
		PriceAlteration priceAlteration = new PriceAlteration();
		priceAlteration
		.description(alterationPOP.getDescription())
		.name(alterationPOP.getName())
		.priceType(alterationPOP.getPriceType())
		.setPrice(price);
		
		if (!CollectionUtils.isEmpty(alterationPOP.getProductOfferingTerm())) {
			ProductOfferingTerm term = alterationPOP.getProductOfferingTerm().get(0);
			
			priceAlteration
			.applicationDuration(term.getDuration().getAmount())
			.setUnitOfMeasure(term.getDuration().getUnits());
		}
		
		logger.info("Applied {}% discount to price of {} euro. Discounted price: {} {}", 
				alterationPOP.getPercentage(), basePrice, discounteMoney.getValue(), discounteMoney.getUnit());
		
		return priceAlteration;
	}*/
	

	@Override
	public BigDecimal applyAlteration(BigDecimal basePrice, ProductOfferingPrice alterationPOP) throws BillingEngineValidationException {
		BigDecimal discount =BigDecimal.ZERO;
		
		// If percentage is set
		if (alterationPOP.getPercentage() != null) {
            discount = basePrice
                    .multiply(new BigDecimal(String.valueOf(alterationPOP.getPercentage())))
                    .divide(ONE_HUNDRED)
                    .setScale(2, RoundingMode.HALF_EVEN);
            
            logger.info("Applied {}% discount to base price '{}'. Discount '{}'", 
    				alterationPOP.getPercentage(), basePrice, discount);
        } 
		// if the percentage is not set will be considered as discount the price of the pop alteration
		else {
        	Money price=alterationPOP.getPrice();
        	if(price!=null) {
        		tmfEntityValidator.validatePrice(alterationPOP);
        		
        		discount=new BigDecimal(String.valueOf(price.getValue()));
        		logger.info("Discount applied to base price '{}': Discount '{}'", 
        				basePrice, discount);
        	}
        }
		
		return discount.abs().negate();
	}
	
	

}
