package it.eng.dome.billing.engine.price.alteration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.price.PriceUtils;
import it.eng.dome.billing.engine.utils.TMForumEntityUtils;
import it.eng.dome.billing.engine.utils.TmfConverter;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPriceRelationship;
import it.eng.dome.tmforum.tmf620.v4.model.TimePeriod;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
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
	 * Applies to the {@link OrderPrice} representing the initial order price the list of {@link ProductOfferingPrice} alterations 
	 * 
	 * @param orderPrice The initial {@link OrderPrice} to which the price alterations must be applied
	 * @param popRels the list of {@link ProductOfferingPrice} alterations
	 * @return the {@link OrderPrice} updated with the {@link PriceAlteration}
	 * @throws BillingEngineValidationException if some unexpected/missing values are find during the validation of the TMForum entities
	 */
	public OrderPrice applyAlterations(@NotNull OrderPrice orderPrice, @NotNull List<ProductOfferingPrice> popRels) throws BillingEngineValidationException {
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
				
				BigDecimal alteratedPriceValue= alterationCalculator.applyAlteration(baseOrderPriceValue, popRel);
				Price alteratedPrice=TMForumEntityUtils.createPriceTMF622(new Money(priceCurrency,alteratedPriceValue.floatValue()));
				
				logger.debug("Applying alteration '{}' on base order price: {} {}",popRel.getPriceType(), baseOrderPriceValue,priceCurrency);
				
				PriceAlteration priceAlteration=TMForumEntityUtils.createPriceAlteration(alteratedPrice, popRel);
				
				orderPrice.addPriceAlterationItem(priceAlteration);
			}
		}
		
		return orderPrice;
	}
	
	/*public Price applyAlterations (ProductOfferingPrice pop, Price basePrice) throws Exception {
		final var itemAlteredPrice = basePrice.getDutyFreeAmount().getValue();
		ProductOfferingPrice alterationPOP;
		PriceAlterationOperation alterationCalculator;
		PriceAlteration alteredPrice=new PriceAlteration();
		final Date today = new Date();
		
		// loops for all the alterations
		for (ProductOfferingPriceRelationship popR : pop.getPopRelationship()) {
			// retrieve pops from server
			alterationPOP = productCatalogManagementApis.getProductOfferingPrice(popR.getId(), null);
			
			// to be used, the alteration must be active
			if (!PriceUtils.isActive(alterationPOP))
				continue;
			
			if (!PriceUtils.isValid(today, alterationPOP.getValidFor()))
				continue;
			
			// the alteration type must be one of the types known
			alterationCalculator = priceAlterationFactory.getPriceAlterationCalculator(alterationPOP);
			if (alterationCalculator == null)
				continue;
			
			logger.debug("Applying alteration '{}' on base item price: {} euro", alterationPOP.getPriceType(), itemAlteredPrice);
			alteredPrice = alterationCalculator.applyAlteration(itemAlteredPrice, alterationPOP);
		}
		
		return alteredPrice.getPrice();
	}*/
	
	
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
				alterationAmounts.add(alterationCalculator.applyAlteration(basePriceValue, popRel));
			}
			
			totalAtlerationsAmount=alterationAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
			
			Money finalPrice=new Money(priceCurrency,basePriceValue.add(totalAtlerationsAmount).floatValue());
			
			return finalPrice;
			
		}else {
			return basePrice;
		}
	}
	
	

}
