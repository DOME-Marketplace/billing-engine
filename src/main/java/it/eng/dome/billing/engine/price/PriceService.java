package it.eng.dome.billing.engine.price;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.billing.engine.tmf.TmfApiFactory;
import it.eng.dome.tmforum.tmf620.v4.ApiClient;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingPriceApi;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.Product;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOfferingPriceRef;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf622.v4.model.ProductPrice;

@Component(value = "priceService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PriceService implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(PriceService.class);
	
	@Autowired
	private TmfApiFactory tmfApiFactory;
	
	@Autowired
	private PriceCalculatorFactory priceCalculatorFactory;
	
	@Autowired
	private PriceAlterationCalculator priceAlterationCalculator;
	
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
	    	// 1) retrieve from the server the ProductOfferingPrice
	    	pop = getProductOfferingPrice(item, popApi);
	    	// 2) retrieve the price calculator for the ProductOfferingPrice
	    	priceCalculator = priceCalculatorFactory.getPriceCalculator(pop);
	    	// 3) calculates the price
	    	itemPrice = priceCalculator.calculatePrice(item, pop);
	    	// TODO: da cambiare l'alterations non può fare parte di un calcolo esterno al prezzo
	    	// 4) calculates the alteration of the price
			if (PriceUtils.hasRelationships(pop)) {
				priceAlterationCalculator.applyAlterations(item, pop, itemPrice);
			}
			// 5) 
			if (PriceUtils.hasAlterations(itemPrice))
				orderTotalPriceAmount += PriceUtils.getAlteredDutyFreePrice(itemPrice);
			else
				orderTotalPriceAmount += PriceUtils.getDutyFreePrice(itemPrice);
	    }
	    
	    // 6) calculates order total price
		if (order.getOrderTotalPrice() != null)
			order.setOrderTotalPrice(new ArrayList<OrderPrice>());
		
		OrderPrice orderTotal = new OrderPrice();
		Price orderTotalPrice = new Price();
	    EuroMoney euro = new EuroMoney(orderTotalPriceAmount);
		orderTotalPrice.setDutyFreeAmount(euro.toMoney());
		orderTotalPrice.setTaxIncludedAmount(null);
		orderTotal.setPrice(orderTotalPrice);
		order.addOrderTotalPriceItem(orderTotal);
		
		logger.info("Calculated order total price: {} ", euro.getAmount());

	    return itemPrice;
	}

	/*
	public ProductPrice calculateProductPrice(Product product) throws Exception {
		final ApiClient apiClient = tmfApiFactory.getTMF620ProductCatalogApiClient();
	    final var popApi = new ProductOfferingPriceApi(apiClient);

	    return null;
	}
	*/
	
	
	private ProductOfferingPrice getProductOfferingPrice(ProductOrderItem orderItem, ProductOfferingPriceApi popApi) throws ApiException {
		var itemPricesIterator = orderItem.getItemPrice().iterator();
		OrderPrice currentOrderPrice;
		ProductOfferingPriceRef currentPOPRef;
		ProductOfferingPrice currentPOP;
		while (itemPricesIterator.hasNext()) {
			currentOrderPrice = itemPricesIterator.next();
			currentPOPRef = currentOrderPrice.getProductOfferingPrice();
			if (currentPOPRef == null || StringUtils.isBlank(currentPOPRef.getId()))
				continue;
			
			logger.debug("Retrieving from server POP with id: '{}'", currentPOPRef.getId());
			currentPOP = popApi.retrieveProductOfferingPrice(currentPOPRef.getId(), null);
			if (!PriceUtils.isActive(currentPOP))
				continue;
			
			return currentPOP;
		}
		
		return null;
	}

	
}
