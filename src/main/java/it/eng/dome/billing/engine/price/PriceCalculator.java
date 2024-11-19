package it.eng.dome.billing.engine.price;

import java.util.List;

import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;

public interface PriceCalculator {

	
	OrderPrice calculatePrice(ProductOrderItem orderItem, 
			ProductOfferingPrice pop,
			List<ProductOfferingPrice> bundledPops);
}
