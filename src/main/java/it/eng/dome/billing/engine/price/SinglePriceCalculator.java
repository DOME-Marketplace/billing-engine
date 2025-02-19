package it.eng.dome.billing.engine.price;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;

@Component(value = "singlePriceCalculator")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SinglePriceCalculator implements PriceCalculator {

	@Autowired
	private PriceAlterationCalculator priceAlterationCalculator; 
	
    private final Logger logger = LoggerFactory.getLogger(SinglePriceCalculator.class);

	/**
	 * Calculates the price of a product item using only one price as a forfait price.
	 * In this case, all the Characteristics of the product item are predefined by the
	 * provider. The Order is composed of order items and every order items is composed 
	 * by Characteristics each one with its own price and its own quantity defined by the provider.
	 */
	@Override
	public List<OrderPrice> calculatePrice(ProductOrderItem orderItem, ProductOfferingPrice pop) throws Exception {
		try {
			
			List<OrderPrice> orderPriceList=new ArrayList<OrderPrice>();
		
			logger.debug("Starting single price calculation...");
			
			// check if UnitofMeasure is not null and different from "1 unit"
			if(pop.getUnitOfMeasure()!=null && !"unit".equalsIgnoreCase(pop.getUnitOfMeasure().getUnits()) && pop.getUnitOfMeasure().getAmount()!=1) {
				throw new BillingBadRequestException(String.format("The UnitOfMeasure element of ProductOfferingPrice '%s' with single price  must be null or 1 unit!", pop.getId()));
			}
			
			// calculates base price
			OrderPrice orderItemPrice=PriceUtils.calculatePrice(pop, orderItem, priceAlterationCalculator);
			
			orderPriceList.add(orderItemPrice);
			
			return orderPriceList;
			
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
			// Java exception is converted into HTTP status code by the ControllerExceptionHandler
			throw new Exception(e); //throw (e.getCause() != null) ? e.getCause() : e;
		}
	}
	
}
