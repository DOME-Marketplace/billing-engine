package it.eng.dome.billing.engine.price;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.util.CollectionUtils;

import it.eng.dome.billing.engine.bill.BillUtils;
import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.billing.engine.tmf.TmfApiFactory;
import it.eng.dome.brokerage.billing.utils.BillingPriceType;
import it.eng.dome.billing.engine.utils.PriceTypeKey;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOfferingPriceRef;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;

@Component(value = "priceService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PriceService implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(PriceService.class);
	
	@Autowired
	private TmfApiFactory tmfApiFactory;
	
	@Autowired
	private PriceCalculatorFactory priceCalculatorFactory;
	
	//private ProductOfferingPriceApis productOfferingPriceApis;
	private ProductCatalogManagementApis productCatalogManagementApis;
	
	//Hash Map to manage aggregation of the OrderPrice
	private Map<PriceTypeKey, List<OrderPrice>> orderPriceGroups;
	
	//Hash Map to manage the UsageCharacteristic
	private Map<String, List<UsageCharacteristic>> usageDataMap=null;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		//productOfferingPriceApis = new ProductOfferingPriceApis(tmfApiFactory.getTMF620ProductCatalogApiClient());
		productCatalogManagementApis = new ProductCatalogManagementApis(tmfApiFactory.getTMF620ProductCatalogApiClient());
	}
	

    /*
     * Calculate the prices of the ProductOrder and updates the ProductOrder with the calculate prices (i.e., OrderPrice instances)
     *   
     * @param order The ProductOrder for which the prices must be calculated
     * @throws Exception If an error occurs during the price calculation
     */
	public void calculateOrderPrice(ProductOrder order) throws Exception {
		ProductOfferingPrice pop;
	    PriceCalculator priceCalculator;
	    
	    // HashMap to manage aggregation of OrderPrice
	    this.orderPriceGroups=new HashMap<PriceTypeKey, List<OrderPrice>>();
	    
		try {
			if(order.getProductOrderItem()==null||order.getProductOrderItem().isEmpty())
				throw new BillingBadRequestException("Cannot calculate price preview for a ProductOrder with empty 'productOrderItem' attribute!");
			
			// Iteration over the ProductOrderItem elements of the ProductOrder
		    for (ProductOrderItem item : order.getProductOrderItem()) {
		    	
		    	List<OrderPrice> itemTotalPriceList = new ArrayList<OrderPrice>();
		    	
		    	if(CollectionUtils.isEmpty(item.getItemTotalPrice()))
		    		throw new BillingBadRequestException("Cannot calculate price preview for a ProductOrderItem with empty 'itemTotalPrice' attribute!");
		    		
		    	pop=new ProductOfferingPrice();
		    	for(OrderPrice op:item.getItemTotalPrice()) {
		    		
		    		// Retrieves from the server the ProductOfferingPrice
		    		pop=getReferredProductOfferingPrice(op, productCatalogManagementApis);
		    	
		    		if(pop==null) {
		    			throw new BillingBadRequestException("No valid ProductOfferingPrice found for ProductOrdeItem element: '" + item.getId() + "' !");
		    		}
				
		    		// Retrieves the price calculator for the ProductOfferingPrice
		    		priceCalculator = priceCalculatorFactory.getPriceCalculator(pop);
		    		
		    		// Calculates the OrderPrice(s)
		    		List<OrderPrice> itemPriceList = priceCalculator.calculateOrderPrice(item, pop);
		    	
		    		itemTotalPriceList.addAll(itemPriceList);

		    		updateOrderPriceGroups(itemPriceList);
		    	}
		    	
		    	if(pop.getIsBundle()!=null && pop.getIsBundle())
		    		continue;
		    	else
		    		// updates the itemTotalPrice element of the ProductOrderItem
		    		item.setItemTotalPrice(itemTotalPriceList);
		    }
		    	    
		    // Calculates orderTotalPrice element
			if (order.getOrderTotalPrice() != null)
				order.setOrderTotalPrice(new ArrayList<OrderPrice>());
			
			// Calculate orderTotalPrice over groups aggregating for PriceTypeKey
			Set<PriceTypeKey> keys= orderPriceGroups.keySet();
			for(PriceTypeKey key: keys) {
				OrderPrice orderTotalPriceElement=calculateOrderTotalPriceElement(key);
				order.addOrderTotalPriceItem(orderTotalPriceElement);
			}
			
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
			// Java exception is converted into HTTP status code by the ControllerExceptionHandler
			throw new Exception(e); //throw (e.getCause() != null) ? e.getCause() : e;
		}
	    
	}

	/*
	 * Calculates the OrderPrice that will be added to the orderTotalPrice element of the ProductOrder aggregating according to the specified key.
	 * 
	 * @param key the PriceTypeKey considered for make the aggregation of the prices
	 * @return the OrderPrice item of the orderTotalPrice element of the ProductOrder
	 */
	private OrderPrice calculateOrderTotalPriceElement(PriceTypeKey key) {
		logger.info("Calculate 'orderTotalPrice' for group with key "+key.toString());
		
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
		String priceTypeNormalized=BillingPriceType.normalize(key.getPriceType());
		orderTotalPriceElement.setPriceType(priceTypeNormalized);
		if(!(BillingPriceType.ONE_TIME.getNormalizedKey().equalsIgnoreCase(priceTypeNormalized))){
		//if(!("one time".equalsIgnoreCase(key.getPriceType())) && !("one-time".equalsIgnoreCase(key.getPriceType()))) {
			orderTotalPriceElement.setRecurringChargePeriod(key.getRecurringChargePeriod());
		}
		
		logger.info("Order total price: {} euro ", orderTotalPriceAmount);
		return orderTotalPriceElement;
	}
	
	/*
	 * Updates the hash map of OrderPrice groups adding the specified OrderPrice list 
	 * 
	 * @param itemPriceList the list of OrderPrice to add to the hash map of OrderPrice 
	 */
	private void updateOrderPriceGroups(List<OrderPrice> itemPriceList){
		if(itemPriceList!=null) {
			for(OrderPrice op:itemPriceList) {
				PriceTypeKey key;
				String priceTypeNormalized=BillingPriceType.normalize(op.getPriceType());
				
				if(priceTypeNormalized.equalsIgnoreCase(BillingPriceType.ONE_TIME.getNormalizedKey()))
				//if(("one time".equalsIgnoreCase(op.getPriceType()))||("one-time".equalsIgnoreCase(op.getPriceType())))
					key=new PriceTypeKey(priceTypeNormalized, null);
				else 
					key=new PriceTypeKey(priceTypeNormalized, op.getRecurringChargePeriod());
						
				if(orderPriceGroups.containsKey(key)) {
					orderPriceGroups.get(key).add(op);
				}else {
					List<OrderPrice> list=new ArrayList<OrderPrice>();
					list.add(op);
					orderPriceGroups.put(key, list);
				}
			}
		}
	}
	
	/*
	 * Gets the ProductOfferingPrice instance referred by the specified OrderPrice
	 * 
	 * @param orderPrice The OrderPrice instance
	 * @param popApi The ProductOfferingPriceApi instance
	 * @return the ProductOggeringPrice instance referred by the specified OrderPrice
	 * @throws Exception If the ProductOfferingPrice referenced in the OrderPrice is not found
	 */
	private ProductOfferingPrice getReferredProductOfferingPrice(OrderPrice orderPrice, ProductCatalogManagementApis productCatalogManagementApis) throws Exception {
		final Date today = new Date();

		ProductOfferingPriceRef popRef;
		ProductOfferingPrice pop;

		popRef = orderPrice.getProductOfferingPrice();
		if (popRef == null || StringUtils.isBlank(popRef.getId()))
			return null;
		
		logger.debug("Retrieving remote POP with id: '{}'", popRef.getId());
		try {
			pop = productCatalogManagementApis.getProductOfferingPrice(popRef.getId(), null);
			
			if (!PriceUtils.isActive(pop) && !PriceUtils.isValid(today, pop.getValidFor()))
				return null;
			
			return pop;
		} catch (ApiException exc) {
			if (exc.getCode() == HttpStatus.NOT_FOUND.value()) {
				throw (IllegalStateException)new IllegalStateException(String.format("ProductOfferingPrice with id %s not found on server!", popRef.getId())).initCause(exc);
			}			
			throw exc;
		}
	}


	/*
     * Calculate the prices of the ProductOrder. The Usage data, if present, are used to simulate the consumptions and calculate the price for the pay per use plans. The ProductOrder is updated with the calculated prices (i.e., OrderPrice instances)
     *   
     * @param order The ProductOrder for which the prices must be calculated
     * @param usageData The simulated usage data for pay per use plans
     * @throws Exception If an error occurs during the price calculation
     */
	public void calculateOrderPrice(ProductOrder order, List<Usage> usageData) throws Exception{
		
		ProductOfferingPrice pop;
	    PriceCalculator priceCalculator;
	    
	    // HashMap to manage aggregation of OrderPrice
	    this.orderPriceGroups=new HashMap<PriceTypeKey, List<OrderPrice>>();
	    
		try {
			if(order.getProductOrderItem()==null||order.getProductOrderItem().isEmpty())
				throw new BillingBadRequestException("Cannot calculate price preview for a ProductOrder with empty 'productOrderItem' attribute!");
			
			if(usageData!=null && !usageData.isEmpty()) {
				usageDataMap=BillUtils.createUsageCharacteristicDataMap(usageData);
				logger.info("Created UsageDataMap with keys "+usageDataMap.keySet().toString());
			}
			
			// Iteration over the ProductOrderItem elements of the ProductOrder
		    for (ProductOrderItem item : order.getProductOrderItem()) {
		    	
		    	List<OrderPrice> itemTotalPriceList = new ArrayList<OrderPrice>();
		    	
		    	if(CollectionUtils.isEmpty(item.getItemTotalPrice()))
		    		throw new BillingBadRequestException("Cannot calculate price preview for a ProductOrderItem with empty 'itemTotalPrice' attribute!");
		    		
		    	pop=new ProductOfferingPrice();
		    	for(OrderPrice op:item.getItemTotalPrice()) {
		    		List<OrderPrice> itemPriceList=new ArrayList<OrderPrice>();
		    		
		    		// Retrieves from the server the ProductOfferingPrice
		    		pop=getReferredProductOfferingPrice(op, productCatalogManagementApis);
		    	
		    		if(pop==null) {
		    			throw new BillingBadRequestException("No valid ProductOfferingPrice found for ProductOrdeItem element: '" + item.getId() + "' !");
		    		}
				
		    		// Retrieves the price calculator for the ProductOfferingPrice
		    		priceCalculator = priceCalculatorFactory.getPriceCalculator(pop);
		    		
		    		// Calculates the OrderPrice(s)
		    		// Check the pay-per-use use case
		    		if(pop.getPriceType()!=null && ((BillingPriceType.normalize(pop.getPriceType()).equalsIgnoreCase(BillingPriceType.PAY_PER_USE.getNormalizedKey())) || (BillingPriceType.normalize(pop.getPriceType()).equalsIgnoreCase(BillingPriceType.USAGE.getNormalizedKey())))) {
		    			
		    			// Retrieve all the simulated UsageCharacteristic associated to the metric
		    			String metric=(BillUtils.getPOPUnitOfMeasure_Units(pop));
		    			
		    			if(usageDataMap==null)
		    				throw new BillingBadRequestException(String.format("ProductOfferingPrice with id {} refers to a pay-per-use price plan but the simulated usage data are missing!", pop.getId()));
		    			
						List<UsageCharacteristic> usageChForMetric=usageDataMap.get(metric);
						if(usageChForMetric!=null) {
							logger.debug("Size of the list of UsageCharacteristic for metric {}: {}",metric,usageChForMetric.size());
							itemPriceList = priceCalculator.calculateOrderPriceForUsageCharacteristic(item, pop, usageChForMetric);
						}
						else
							logger.warn("It is not possible to calculate the price for the item with id '{}' related to the metric '{}' because no usage data ara available for that metric!",item.getId(),metric);
						
		    		}else {
		    			itemPriceList = priceCalculator.calculateOrderPrice(item, pop);
		    		}
		    	
		    		itemTotalPriceList.addAll(itemPriceList);

		    		updateOrderPriceGroups(itemPriceList);
		    	}
		    	
		    	if(pop.getIsBundle()!=null && pop.getIsBundle())
		    		continue;
		    	//else
		    		// updates the itemTotalPrice element of the ProductOrderItem
		    		//item.setItemTotalPrice(itemTotalPriceList);
		    }
		    	    
		    // Calculates orderTotalPrice element
			if (order.getOrderTotalPrice() != null)
				order.setOrderTotalPrice(new ArrayList<OrderPrice>());
			
			// Calculate orderTotalPrice over groups aggregating for PriceTypeKey
			Set<PriceTypeKey> keys= orderPriceGroups.keySet();
			for(PriceTypeKey key: keys) {
				OrderPrice orderTotalPriceElement=calculateOrderTotalPriceElement(key);
				order.addOrderTotalPriceItem(orderTotalPriceElement);
			}
			
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
			// Java exception is converted into HTTP status code by the ControllerExceptionHandler
			throw new Exception(e); //throw (e.getCause() != null) ? e.getCause() : e;
		}
		
	}

	
}
