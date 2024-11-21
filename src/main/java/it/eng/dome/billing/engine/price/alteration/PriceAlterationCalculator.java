package it.eng.dome.billing.engine.price.alteration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.price.PriceUtils;
import it.eng.dome.billing.engine.tmf.TmfApiFactory;
import it.eng.dome.tmforum.tmf620.v4.ApiClient;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingPriceApi;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPriceRelationship;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;

@Component(value = "priceAlterationCalculator")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PriceAlterationCalculator implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(PriceAlterationCalculator.class);
	
	@Autowired
	private TmfApiFactory tmfApiFactory;
	
	@Autowired
	private PriceAlterationFactory priceAlterationFactory;
	
	private ProductOfferingPriceApi popApi;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		final ApiClient apiClient = tmfApiFactory.getTMF620ProductCatalogApiClient();
	    popApi = new ProductOfferingPriceApi(apiClient);
	}
	
	/**
	 * 
	 * @param orderItem
	 * @param pop
	 * @param orderPrice
	 * @throws ApiException
	 */
	public void applyAlterations (ProductOrderItem orderItem, ProductOfferingPrice pop, OrderPrice orderPrice) throws ApiException {
		final var itemAlteredPrice = orderPrice.getPrice().getDutyFreeAmount().getValue();
		ProductOfferingPrice alterationPOP;
		PriceAlterationOperation alterationCalculator;
		PriceAlteration alteredPrice;
		// loops for all the alterations
		for (ProductOfferingPriceRelationship popR : pop.getPopRelationship()) {
			// retrieve pops from server
			alterationPOP = popApi.retrieveProductOfferingPrice(popR.getId(), null);
			
			// to be used, the alteration must be active
			if (!PriceUtils.isActive(alterationPOP))
				continue;
			
			// the alteration type must be one of the types known
			alterationCalculator = priceAlterationFactory.getPriceAlterationCalculator(alterationPOP);
			if (alterationCalculator == null)
				continue;
			
			logger.info("Applying alteration '{}' on base item price: {} euro", alterationPOP.getPriceType(), itemAlteredPrice);
			alteredPrice = alterationCalculator.applyAlteration(itemAlteredPrice, alterationPOP);
			orderPrice.addPriceAlterationItem(alteredPrice);
		}
	}

}
