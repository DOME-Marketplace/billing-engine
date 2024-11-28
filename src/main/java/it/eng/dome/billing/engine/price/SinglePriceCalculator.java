package it.eng.dome.billing.engine.price;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
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
	public OrderPrice calculatePrice(ProductOrderItem orderItem, ProductOfferingPrice pop) throws Exception {
		logger.debug("Starting single price calculation...");
		// check if match prod chars and pop chars
		// should check that all the Characteristics of the pop are the same of the ProductOrderItem
		
		// check if unit is null
		Assert.state(pop.getUnitOfMeasure() == null, 
				String.format("Unit of Measure of single price '%s' must be null!", pop.getId()));
		
		// 1) retrieves the OrderPrice instance linked to the ProductOfferingPrice received
		Optional<OrderPrice> orderPriceOpt = orderItem.getItemTotalPrice()
		.stream()
		.filter( op -> (op.getProductOfferingPrice() != null && op.getProductOfferingPrice().getId().equals(pop.getId())))
		.findFirst();
		
		Assert.state(orderPriceOpt.isPresent(), "Cannot retrieve OrderPrice instance linked to POP: " + pop.getId());
		final OrderPrice orderItemPrice = orderPriceOpt.get();
		
		// 2 calculates base price
		final Price itemPrice = new Price();
		EuroMoney euro = new EuroMoney(pop.getPrice().getValue() * orderItem.getQuantity());
		itemPrice.setDutyFreeAmount(euro.toMoney());
		itemPrice.setTaxIncludedAmount(null);
		orderItemPrice.setName(pop.getName());
		orderItemPrice.setDescription(pop.getDescription());
		orderItemPrice.setPriceType(pop.getPriceType());
		orderItemPrice.setRecurringChargePeriod(pop.getRecurringChargePeriodType());
		orderItemPrice.setPrice(itemPrice);
		
		logger.info("Price of item '{}': [quantity: {}, price: '{}'] = {} euro", 
				orderItem.getId(), orderItem.getQuantity(), pop.getPrice().getValue(), euro.getAmount());
		
    	// 3) apply price alterations
		if (PriceUtils.hasRelationships(pop)) {
			priceAlterationCalculator.applyAlterations(orderItem, pop, orderItemPrice);
			
			logger.info("Price of item '{}' after alterations = {} euro", 
					orderItem.getId(), PriceUtils.getAlteredDutyFreePrice(orderItemPrice));
		}
		
		return orderItemPrice;
	}
	
}
