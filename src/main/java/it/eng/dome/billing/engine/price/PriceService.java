package it.eng.dome.billing.engine.price;

import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.billing.engine.tmf.TmfApiFactory;
import it.eng.dome.tmforum.tmf620.v4.ApiClient;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingPriceApi;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOfferingPriceRef;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;

@Component(value = "priceService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PriceService implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(PriceService.class);
	
	@Autowired
	private TmfApiFactory tmfApiFactory;
	
	@Autowired
	private PriceCalculatorFactory priceCalculatorFactory;
	
	private ProductOfferingPriceApi popApi;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		final ApiClient apiClient = tmfApiFactory.getTMF620ProductCatalogApiClient();
	    popApi = new ProductOfferingPriceApi(apiClient);
	}
	
	/**
	 * 
	 * @param pOrder: the order has been previously validated by the controller
	 * @return
	 */
	public OrderPrice calculateOrderPrice(ProductOrder order) throws Exception {
	    ProductOfferingPrice pop;
	    PriceCalculator priceCalculator;
	    OrderPrice itemPrice = null;
	    float orderTotalPriceAmount = 0F;
	    for (ProductOrderItem item : order.getProductOrderItem()) {
			Assert.state(!CollectionUtils.isEmpty(item.getItemTotalPrice()), "Cannot calculate price for order item with empty 'itemTotalPrice' attribute!");

	    	// 1) retrieve from the server the ProductOfferingPrice
	    	pop = getReferredProductOfferingPrice(item, popApi);
			Assert.state(pop != null, "No valid ProductOfferingPrice found for item: '" + item.getId() + "' !");

	    	// 2) retrieve the price calculator for the ProductOfferingPrice
	    	priceCalculator = priceCalculatorFactory.getPriceCalculator(pop);
	    	// 3) calculates the price
	    	itemPrice = priceCalculator.calculatePrice(item, pop);
	    	
	    	if (PriceUtils.hasAlterations(itemPrice))
	    		orderTotalPriceAmount += PriceUtils.getAlteredDutyFreePrice(itemPrice);
	    	else
	    		orderTotalPriceAmount += PriceUtils.getDutyFreePrice(itemPrice);
	    }
	    
	    // 4) calculates order total price
		if (order.getOrderTotalPrice() != null)
			order.setOrderTotalPrice(new ArrayList<OrderPrice>());
		
		OrderPrice orderTotal = new OrderPrice();
		Price orderTotalPrice = new Price();
	    EuroMoney euro = new EuroMoney(orderTotalPriceAmount);
		orderTotalPrice.setDutyFreeAmount(euro.toMoney());
		orderTotalPrice.setTaxIncludedAmount(null);
		orderTotal.setPrice(orderTotalPrice);
		order.addOrderTotalPriceItem(orderTotal);
		
		logger.info("Calculated order total price: {} euro ", orderTotalPriceAmount);

	    return itemPrice;
	}

	/*
	public ProductPrice calculateProductPrice(Product product) throws Exception {
		final ApiClient apiClient = tmfApiFactory.getTMF620ProductCatalogApiClient();
	    final var popApi = new ProductOfferingPriceApi(apiClient);

	    return null;
	}
	*/
	
	/*
	 * Loops over list of OrderPrice named itemTotalPrice, to retrieve the first active ProductOfferingPrice
	 * (pop.status == 'Launched' and today between pop.validFor)
	 * 
	 */
	private ProductOfferingPrice getReferredProductOfferingPrice(ProductOrderItem orderItem, ProductOfferingPriceApi popApi) throws ApiException {
		final Date today = new Date();
		final var itemPrices = orderItem.getItemTotalPrice();
		ProductOfferingPriceRef currentPopRef;
		ProductOfferingPrice currentPop;
		for (OrderPrice currentOrderPrice : itemPrices) {
			currentPopRef = currentOrderPrice.getProductOfferingPrice();
			if (currentPopRef == null || StringUtils.isBlank(currentPopRef.getId()))
				continue;
			
			logger.debug("Retrieving remote POP with id: '{}'", currentPopRef.getId());
			currentPop = popApi.retrieveProductOfferingPrice(currentPopRef.getId(), null);
			if (!PriceUtils.isActive(currentPop))
				continue;
			
			if (!PriceUtils.isValid(today, currentPop.getValidFor()))
				continue;
			
			return currentPop;
		}
		
		return null;
	}

	
}
