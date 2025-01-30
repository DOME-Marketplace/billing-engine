package it.eng.dome.billing.engine.price;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.billing.engine.tmf.TmfApiFactory;
import it.eng.dome.billing.engine.utils.PriceTypeKey;
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
	
	private HashMap<PriceTypeKey, List<OrderPrice>> orderPriceGroups;
	
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
	public List<OrderPrice> calculateOrderPrice(ProductOrder order) throws Exception {
	    ProductOfferingPrice pop;
	    PriceCalculator priceCalculator;
	    OrderPrice itemPrice = null;
	    List<OrderPrice> itemPriceList=new ArrayList<OrderPrice>();
	    //float orderTotalPriceAmount = 0F;
	    this.orderPriceGroups=new HashMap<PriceTypeKey, List<OrderPrice>>();
	    
	    for (ProductOrderItem item : order.getProductOrderItem()) {
			Assert.state(!CollectionUtils.isEmpty(item.getItemTotalPrice()), "Cannot calculate price for order item with empty 'itemTotalPrice' attribute!");

	    	// 1) retrieves from the server the ProductOfferingPrice
	    	pop = getReferredProductOfferingPrice(item, popApi);
			Assert.state(pop != null, "No valid ProductOfferingPrice found for item: '" + item.getId() + "' !");

	    	// 2) retrieves the price calculator for the ProductOfferingPrice
	    	priceCalculator = priceCalculatorFactory.getPriceCalculator(pop);
	    	
	    	// 3) calculates the price
	    	itemPrice = priceCalculator.calculatePrice(item, pop);
	    	
	    	updateOrderPriceGroups(itemPrice,pop);
	    	
	    	itemPriceList.add(itemPrice);
	    	/*if (PriceUtils.hasAlterations(itemPrice))
	    		orderTotalPriceAmount += PriceUtils.getAlteredDutyFreePrice(itemPrice);
	    	else
	    		orderTotalPriceAmount += PriceUtils.getDutyFreePrice(itemPrice);*/
	    }
	    	    
	    // 4) calculates orderTotalPrice
		if (order.getOrderTotalPrice() != null)
			order.setOrderTotalPrice(new ArrayList<OrderPrice>());
		
		// Calculate orderTotalPrice over groups aggregating for PriceTypeKey
		Set<PriceTypeKey> keys= orderPriceGroups.keySet();
		for(PriceTypeKey key: keys) {
			
			OrderPrice orderTotalPriceElement=calculateOrderTotalPriceElement(key);
			order.addOrderTotalPriceItem(orderTotalPriceElement);
			
		}
		
	    return itemPriceList;
	}

	private OrderPrice calculateOrderTotalPriceElement(PriceTypeKey key) {
		logger.info("Calculate order total price element for group with key "+key.toString());
		
		OrderPrice orderTotalPriceElement = new OrderPrice();
		Price orderTotalPrice = new Price();
		float orderTotalPriceAmount = 0F;
		
		List<OrderPrice> orderPrices=orderPriceGroups.get(key);
		// rounds order prices in a group to calculate the orderTotalPriceAmount
		for(OrderPrice op:orderPrices) {
			if (PriceUtils.hasAlterations(op))
				orderTotalPriceAmount += PriceUtils.getAlteredDutyFreePrice(op);
			else
				orderTotalPriceAmount += PriceUtils.getDutyFreePrice(op);			
		}
		
	    BigDecimal bd = new BigDecimal(orderTotalPriceAmount);
	    bd = bd.setScale(2, RoundingMode.HALF_UP);
	    orderTotalPriceAmount = bd.floatValue();

	    EuroMoney euro = new EuroMoney(orderTotalPriceAmount);
		orderTotalPrice.setDutyFreeAmount(euro.toMoney());
		orderTotalPrice.setTaxIncludedAmount(null);
		orderTotalPriceElement.setPrice(orderTotalPrice);
		orderTotalPriceElement.setPriceType(key.getPriceType());
		orderTotalPriceElement.setRecurringChargePeriod(key.getRecurringChargePeriodLength()+" "+key.getRecurringChargePeriodType());
		
		logger.info("Order total price: {} euro ", orderTotalPriceAmount);
		return orderTotalPriceElement;
	}

	private void updateOrderPriceGroups(OrderPrice itemPrice, ProductOfferingPrice pop){
		if(itemPrice!=null && pop!=null) {
			PriceTypeKey key=new PriceTypeKey(pop.getPriceType(), pop.getRecurringChargePeriodType(), pop.getRecurringChargePeriodLength());
			if(orderPriceGroups.containsKey(key)) {
				orderPriceGroups.get(key).add(itemPrice);
			}else {
				List<OrderPrice> list=new ArrayList<OrderPrice>();
				list.add(itemPrice);
				orderPriceGroups.put(key, list);
			}
		}
	}
	
	/*
	 * Loops over list of OrderPrice named itemTotalPrice, to retrieve the first active ProductOfferingPrice
	 * (pop.status == 'Launched' and today between pop.validFor)
	 * 
	 */
	private ProductOfferingPrice getReferredProductOfferingPrice(ProductOrderItem orderItem, ProductOfferingPriceApi popApi) throws Exception {
		final Date today = new Date();
		final var itemPrices = orderItem.getItemTotalPrice();
		ProductOfferingPriceRef currentPopRef;
		ProductOfferingPrice currentPop;
		for (OrderPrice currentOrderPrice : itemPrices) {
			currentPopRef = currentOrderPrice.getProductOfferingPrice();
			if (currentPopRef == null || StringUtils.isBlank(currentPopRef.getId()))
				continue;
			
			logger.debug("Retrieving remote POP with id: '{}'", currentPopRef.getId());
			try {
				currentPop = popApi.retrieveProductOfferingPrice(currentPopRef.getId(), null);
				
				if (!PriceUtils.isActive(currentPop))
					continue;
				
				if (!PriceUtils.isValid(today, currentPop.getValidFor()))
					continue;
				
				return currentPop;
			} catch (ApiException exc) {
				if (exc.getCode() == HttpStatus.NOT_FOUND.value()) {
					throw (IllegalStateException)new IllegalStateException(String.format("ProductOfferingPrice with id %s not found on server!", currentPopRef.getId())).initCause(exc);
				}			
				throw exc;
			}
		}
		
		return null;
	}

	
}
