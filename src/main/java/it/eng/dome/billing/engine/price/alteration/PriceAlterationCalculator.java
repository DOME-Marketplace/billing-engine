package it.eng.dome.billing.engine.price.alteration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.utils.TMForumEntityUtils;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
import jakarta.validation.constraints.NotNull;

@Component(value = "priceAlterationCalculator")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PriceAlterationCalculator {
    private final Logger logger = LoggerFactory.getLogger(PriceAlterationCalculator.class);
	
	@Autowired
	private PriceAlterationFactory priceAlterationFactory;
	
	@Autowired
	private TMFEntityValidator tmfEntityValidator;
	
	/**
	 * Applies to the {@link OrderPrice} representing the initial order price the list of {@link ProductOfferingPrice} alterations.
	 * The OrderPrice will be updated adding the calculated {@link PriceAlteration}. The amount of alteration could be a positive or negative value according to the type of alteration 
	 * (e.g., it will be a negative amount in case of a discount).   
	 * 
	 * @param orderPrice The initial {@link OrderPrice} to which the price alterations must be applied
	 * @param popRels the list of {@link ProductOfferingPrice} alterations
	 * @param quantity a float representing a quantity. If greater then zero the amount of the alteration will be multiplied for the quantity.
	 * @return the {@link OrderPrice} updated with the list of {@link PriceAlteration}
	 * @throws BillingEngineValidationException if some unexpected/missing values are find during the validation of the TMForum entities
	 */
	public OrderPrice applyAlterations(@NotNull OrderPrice orderPrice, @NotNull List<ProductOfferingPrice> popRels, float quantity) throws BillingEngineValidationException {
		PriceAlterationOperation alterationCalculator;
		BigDecimal baseOrderPriceValue=new BigDecimal(String.valueOf(orderPrice.getPrice().getDutyFreeAmount().getValue()));
		String priceCurrency=orderPrice.getPrice().getDutyFreeAmount().getUnit();
		
		if(!popRels.isEmpty()) {
			
			for(ProductOfferingPrice popRel:popRels) {
				
				tmfEntityValidator.validatePopRelationship(popRel);
				
				// to be used, the alteration must be active
				if(!ProductOfferingPriceUtils.isActive(popRel))
					continue;
				
				if (!ProductOfferingPriceUtils.isValid(popRel))
					continue;
				
				// the alteration type must be one of the types known
				alterationCalculator = priceAlterationFactory.getPriceAlterationCalculator(popRel);
				if (alterationCalculator == null)
					continue;
				
				BigDecimal alteratedPriceValue= alterationCalculator.applyAlteration(baseOrderPriceValue, popRel,quantity);
				Price alteratedPrice=TMForumEntityUtils.createPriceTMF622(new Money(priceCurrency,alteratedPriceValue.floatValue()));
				
				//logger.debug("Applying alteration '{}' on base order price: {} {}",popRel.getPriceType(), baseOrderPriceValue,priceCurrency);
				
				PriceAlteration priceAlteration=TMForumEntityUtils.createPriceAlteration(alteratedPrice, popRel);
				
				orderPrice.addPriceAlterationItem(priceAlteration);
			}
		}
		
		return orderPrice;
	}
	
	/**
	 * Applies to the {@link Money} representing a base price the list of {@link ProductOfferingPrice} alterations 
	 * 
	 * @param basePrice a {@link Money} representing the base price
	 * @param popRels the list of {@link ProductOfferingPrice} representing the price alterations that must be applied to the base price
	 * @return a {@link Money} representing the final price after the application of the price alterations
	 * @throws BillingEngineValidationException if some unexpected/missing values are find during the validation of the TMForum entities
	 * @throws ApiException if an error occurs retrieving TMForum entities
	 */
	public Money applyAlterations(@NotNull Money basePrice, @NotNull List<ProductOfferingPrice> popRels) throws BillingEngineValidationException, ApiException{

		PriceAlterationOperation alterationCalculator;
		BigDecimal totalAtlerationsAmount=BigDecimal.ZERO;
		BigDecimal basePriceValue=new BigDecimal(String.valueOf(basePrice.getValue()));
		String priceCurrency=basePrice.getUnit();

		if(!popRels.isEmpty()) {
			List<BigDecimal> alterationAmounts=new ArrayList<BigDecimal>();
			
			for(ProductOfferingPrice popRel:popRels) {
				
				tmfEntityValidator.validatePopRelationship(popRel);
				
				// to be used, the alteration must be active
				if(!ProductOfferingPriceUtils.isActive(popRel))
					continue;
				
				if (!ProductOfferingPriceUtils.isValid(popRel))
					continue;
				
				// the alteration type must be one of the types known
				alterationCalculator = priceAlterationFactory.getPriceAlterationCalculator(popRel);
				if (alterationCalculator == null)
					continue;
				
				logger.debug("Applying alteration '{}' on base price: {} {}",popRel.getPriceType(), basePriceValue,priceCurrency);
				alterationAmounts.add(alterationCalculator.applyAlteration(basePriceValue, popRel, null));
			}
			
			totalAtlerationsAmount=alterationAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
			
			Money finalPrice=new Money(priceCurrency,basePriceValue.add(totalAtlerationsAmount).floatValue());
			
			return finalPrice;
			
		}else {
			return basePrice;
		}
	}
	
	

}
