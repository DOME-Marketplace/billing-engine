package it.eng.dome.billing.engine.price.alteration;

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
import it.eng.dome.billing.engine.utils.TmfConverter;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPriceRelationship;
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
	
	private ProductCatalogManagementApis productCatalogManagementApis;
	
	@Autowired
	private TMFEntityValidator tmfEntityValidator;
	
	public PriceAlterationCalculator (ProductCatalogManagementApis productCatalogManagementApis) {
		this.productCatalogManagementApis = productCatalogManagementApis;
	}
	
	
	/**
	 * 
	 * @param orderItem
	 * @param pop
	 * @param orderPrice
	 * @throws ApiException
	 */
	public void applyAlterations (ProductOrderItem orderItem, ProductOfferingPrice pop, OrderPrice orderPrice) throws Exception {
		final var itemAlteredPrice = orderPrice.getPrice().getDutyFreeAmount().getValue();
		ProductOfferingPrice alterationPOP;
		PriceAlterationOperation alterationCalculator;
		PriceAlteration alteredPrice;
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
			orderPrice.addPriceAlterationItem(alteredPrice);
		}
	}
	
	public Price applyAlterations (ProductOfferingPrice pop, Price basePrice) throws Exception {
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
	}
	
	public Money applyAlterations (@NotNull ProductOfferingPrice pop,@NotNull Money basePrice) throws IllegalArgumentException, ApiException, BillingEngineValidationException{

		PriceAlterationOperation alterationCalculator;
		PriceAlteration priceAlteration=null;
		
		List<ProductOfferingPrice> popRels=ProductOfferingPriceUtils.getProductOfferingPriceRelationships(pop.getPopRelationship(), productCatalogManagementApis);

		if(popRels!=null && !popRels.isEmpty()) {
			for(ProductOfferingPrice popRel:popRels) {
				
				tmfEntityValidator.validateProductOfferingPrice(popRel);
				
				// to be used, the alteration must be active
				if(!ProductOfferingPriceUtils.isActive(popRel))
					continue;
				
				if (!ProductOfferingPriceUtils.isValid(popRel))
					continue;
				
				// the alteration type must be one of the types known
				alterationCalculator = priceAlterationFactory.getPriceAlterationCalculator(popRel);
				if (alterationCalculator == null)
					continue;
				
				logger.debug("Applying alteration '{}' on base price: {} {}",popRel.getPriceType(), basePrice, popRel.getPrice().getUnit());
				priceAlteration = alterationCalculator.applyAlteration(basePrice.getValue(), popRel);
			}
		}
		
		it.eng.dome.tmforum.tmf622.v4.model.Money money622=priceAlteration.getPrice().getDutyFreeAmount();
		
		return TmfConverter.convert622ToMoney(money622);
	}

}
