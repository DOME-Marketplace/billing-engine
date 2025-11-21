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
import org.springframework.util.CollectionUtils;

import it.eng.dome.billing.engine.bill.BillUtils;
import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.price.calculator.PriceCalculator;
//import it.eng.dome.billing.engine.price.PriceCalculator;
import it.eng.dome.billing.engine.utils.PriceTypeKey;
import it.eng.dome.billing.engine.utils.UsageUtils;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.billing.utils.BillingPriceType;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;

@Service
public class PricePreviewService {
	
	private final Logger logger = LoggerFactory.getLogger(PricePreviewService.class);
	
	@Autowired
	private ProductCatalogManagementApis productCatalogManagementApis;
	
	@Autowired
	private TMFEntityValidator tmfEntityValidator;
	
	//Hash Map to manage aggregation of the OrderPrice
	private Map<PriceTypeKey, List<OrderPrice>> orderPriceGroups;
	
	//Hash Map to manage the UsageCharacteristic
	//private Map<String, List<UsageCharacteristic>> usageDataMap = null;
	

	/**
     * Calculate the prices of the specified {@link ProductOrder}. The list of {@link Usage}, if present, are used to simulate the consumptions and calculate the price for the pay per use plans. The ProductOrder is updated with the calculated prices.
     *   
     * @param order The {@link ProductOrder} for which the prices must be calculated
     * @param usageData The simulated {@link Usage} for pay per use plans
     * @throws Exception If an error occurs during the price calculation
     */
	public ProductOrder calculateOrderPrice(ProductOrder order, List<Usage> usageData) throws Exception{
		
		ProductOfferingPrice pop;
		PriceCalculator priceCalculator;

	    
	    // HashMap to manage aggregation of OrderPrice
	    this.orderPriceGroups=new HashMap<PriceTypeKey, List<OrderPrice>>();
	    
		try {
			tmfEntityValidator.validateProductOrder(order);
			
			/*
			 * if(usageData!=null && !usageData.isEmpty()) {
			 * usageDataMap=UsageUtils.createUsageCharacteristicDataMap(usageData);
			 * logger.info("Created UsageDataMap with keys "+usageDataMap.keySet().toString(
			 * )); }
			 */
			
			// Iteration over the ProductOrderItem elements of the ProductOrder
		    for (ProductOrderItem item : order.getProductOrderItem()) {
		    	
		    	List<OrderPrice> itemTotalPriceList = new ArrayList<OrderPrice>();
		    	
		    	//if(CollectionUtils.isEmpty(item.getItemTotalPrice()))
		    		//throw new BillingBadRequestException("Cannot calculate price preview for a ProductOrderItem with empty 'itemTotalPrice' attribute!");
		    		
		    	//tmfEntityValidator.validateProductOrderItem(item, order.getId());
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
