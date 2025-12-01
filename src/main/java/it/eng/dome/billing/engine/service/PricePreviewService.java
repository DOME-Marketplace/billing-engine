package it.eng.dome.billing.engine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.price.calculator.PriceCalculator;
import it.eng.dome.billing.engine.price.calculator.PriceCalculatorFactory;
import it.eng.dome.billing.engine.utils.OrderPriceUtils;
import it.eng.dome.billing.engine.utils.PriceTypeKey;
import it.eng.dome.billing.engine.utils.TMForumEntityUtils;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.brokerage.model.PriceType;
import it.eng.dome.brokerage.model.RecurringChargePeriod;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;

@Service
public class PricePreviewService {
	
	private final Logger logger = LoggerFactory.getLogger(PricePreviewService.class);
	
	@Autowired
	private ProductCatalogManagementApis productCatalogManagementApis;
	
	@Autowired
	private TMFEntityValidator tmfEntityValidator;
	
	@Autowired
	private PriceCalculatorFactory priceCalculatorFactory;
	
	//Hash Map to manage aggregation of the OrderPrice
	private Map<PriceTypeKey, List<OrderPrice>> orderPriceGroups;
	

	/**
     * Calculate the prices of the specified {@link ProductOrder}. The list of {@link Usage}, if present, are used to simulate the consumptions and calculate the price for the pay per use plans. The ProductOrder is updated with the calculated prices.
     *   
     * @param productOrder The {@link ProductOrder} for which the prices must be calculated
     * @param usageData The simulated {@link Usage} for pay per use plans
	 * @throws BillingEngineValidationException 
	 * @throws ApiException 
	 * @throws IllegalArgumentException 
     * @throws Exception If an error occurs during the price calculation
     */
	public ProductOrder calculateOrderPrice(ProductOrder productOrder, List<Usage> usageData) throws BillingEngineValidationException, IllegalArgumentException, ApiException{

	    
	    // HashMap to manage aggregation of OrderPrice
	    this.orderPriceGroups=new HashMap<PriceTypeKey, List<OrderPrice>>();
	    

		tmfEntityValidator.validateProductOrder(productOrder);
		
		// Iteration over the ProductOrderItem elements of the ProductOrder
	    for (ProductOrderItem productOrderitem : productOrder.getProductOrderItem()) {
	    	
	    	List<OrderPrice> itemPriceList = new ArrayList<OrderPrice>();
	    		
	    	tmfEntityValidator.validateProductOrderItem(productOrderitem, productOrder.getId());
	 
	    	for(OrderPrice op:productOrderitem.getItemTotalPrice()) {

	    		tmfEntityValidator.validateOrderPrice(op, productOrderitem.getId(), productOrder.getId());
	    		
	    		// Retrieves from the ProductOfferingPrice
	    		ProductOfferingPrice pop =ProductOfferingPriceUtils.getProductOfferingPrice(op.getProductOfferingPrice().getId(), productCatalogManagementApis);
			
	    		// Retrieves the price calculator for the ProductOfferingPrice
	    		PriceCalculator<ProductOrderItem, List<OrderPrice>> priceCalculator = priceCalculatorFactory.getPriceCalculatorForProductOrderItem(pop, usageData);
	    		
	    		// Calculates the OrderPrice(s)
	    		List<OrderPrice> orderPrices=priceCalculator.calculatePrice(productOrderitem);
	    		
	    		itemPriceList.addAll(orderPrices);

	    		updateOrderPriceGroups(orderPrices);
	    	}
	    	
	    	// updates the itemPrice element of the ProductOrderItem
	    	productOrderitem.setItemPrice(itemPriceList);
	    }
	    	    
	    // Calculates orderTotalPrice element
		if (productOrder.getOrderTotalPrice() != null)
			productOrder.setOrderTotalPrice(new ArrayList<OrderPrice>());
		
		// Calculate orderTotalPrice over groups aggregating for PriceTypeKey
		Set<PriceTypeKey> keys= orderPriceGroups.keySet();
		for(PriceTypeKey key: keys) {
			OrderPrice orderTotalPriceElement=calculateOrderTotalPriceElement(key);
			productOrder.addOrderTotalPriceItem(orderTotalPriceElement);
		}
		
		return productOrder;
		
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

				PriceType priceType=OrderPriceUtils.getPriceType(op);
				if(priceType==PriceType.ONE_TIME||priceType==PriceType.DISCOUNT||priceType==PriceType.CUSTOM)
					key=new PriceTypeKey(priceType, null);
				else {
					RecurringChargePeriod rcp=OrderPriceUtils.getRecurrigChargePeriod(op);
					key=new PriceTypeKey(priceType, rcp);
				}
						
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
	 * Calculates the OrderPrice that will be added to the orderTotalPrice element of the ProductOrder aggregating according to the specified key.
	 * 
	 * @param key the PriceTypeKey considered for make the aggregation of the prices
	 * @return the OrderPrice item of the orderTotalPrice element of the ProductOrder
	 */
	private OrderPrice calculateOrderTotalPriceElement(PriceTypeKey key) {
		logger.info("Calculate 'orderTotalPrice' for group with key "+key.toString());
		
		float orderTotalPriceAmount = 0F;
		String currency = null;
		
		List<OrderPrice> orderPrices=orderPriceGroups.get(key);
		// rounds order prices in a group to calculate the orderTotalPriceAmount
		for(OrderPrice op:orderPrices) {
			if(currency==null)
				currency=OrderPriceUtils.getCurrency(op);
			
			if (OrderPriceUtils.hasAlterations(op))
				orderTotalPriceAmount += OrderPriceUtils.getAlteredDutyFreePrice(op);
			else
				orderTotalPriceAmount += OrderPriceUtils.getDutyFreePrice(op);			
		}
		
		Money money=new Money(currency,orderTotalPriceAmount);
		Price orderTotalPrice=TMForumEntityUtils.createPriceTMF622(money);
		OrderPrice orderTotalPriceElement=TMForumEntityUtils.createOrderTotalPriceItemTMF622(orderTotalPrice, key);
		
		logger.info("Order total price: {} euro ", orderTotalPriceAmount);
		return orderTotalPriceElement;
	}


}
