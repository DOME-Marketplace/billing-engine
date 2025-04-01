package it.eng.dome.billing.engine.price;

import java.util.List;

import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;

@FunctionalInterface
public interface PriceCalculator {
	
	/**
	 * Calculate the prices for the specified ProductOrderItem and ProductOfferingPrice
	 * 
	 * @param orderItem the ProductOrderItem instance
	 * @param pop the ProductOfferingPrice instance
	 * @return a list of calculate prices (i.e., OrderPrice instances)
	 */
	List<OrderPrice> calculatePrice(ProductOrderItem orderItem, ProductOfferingPrice pop) throws Exception;
	
}
