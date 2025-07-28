package it.eng.dome.billing.engine.price;

import java.util.List;

import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;

//@FunctionalInterface
public interface PriceCalculator {
	
	/**
	 * Calculate the prices for the specified ProductOrderItem and ProductOfferingPrice
	 * 
	 * @param orderItem the ProductOrderItem instance
	 * @param pop the ProductOfferingPrice instance
	 * @return a list of calculate prices (i.e., OrderPrice instances)
	 */
	List<OrderPrice> calculateOrderPrice(ProductOrderItem orderItem, ProductOfferingPrice pop) throws Exception;
	
	/**
	 * Calculate the prices for the specified ProductOrderItem referring a pay-per-use price plan.
	 * 
	 * @param orderItem the ProductOrderItem instance
	 * @param pop the ProductOfferingPrice instance
	 * @param usageCharacteristics the usage data to calculate the pay-per-use plan
	 * @return a list of calculate prices (i.e., OrderPrice instances)
	 */
	List<OrderPrice> calculateOrderPriceForUsageCharacteristic(ProductOrderItem orderItem, ProductOfferingPrice pop, List<UsageCharacteristic> usageCharacteristics) throws Exception;
	
}
