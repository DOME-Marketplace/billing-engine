package it.eng.dome.billing.engine.price;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;

@Component(value = "singlePriceCalculator")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SinglePriceCalculator implements PriceCalculator {

    private final Logger logger = LoggerFactory.getLogger(SinglePriceCalculator.class);

	@Override
	public OrderPrice calculatePrice(ProductOrderItem orderItem, ProductOfferingPrice pop) throws Exception {
		
		// retrieves the OrderPrice instance linked to the ProductOfferingPrice received
		Optional<OrderPrice> orderPriceOpt = orderItem.getItemPrice()
		.stream()
		.filter( op -> (op.getProductOfferingPrice() != null && op.getProductOfferingPrice().getId().equals(pop.getId())))
		.findFirst();
		
		Assert.state(orderPriceOpt.isPresent(), "Cannot retrieve OrderPrice instance linked to POP: " + pop.getId());
		final OrderPrice orderItemPrice = orderPriceOpt.get();
		
		final Price itemPrice = new Price();
		EuroMoney euro = new EuroMoney(pop.getPrice().getValue() * orderItem.getQuantity());
		itemPrice.setDutyFreeAmount(euro.toMoney());
		itemPrice.setTaxIncludedAmount(null);
		orderItemPrice.setName(pop.getName());
		orderItemPrice.setDescription(pop.getDescription());
		orderItemPrice.setPriceType(pop.getPriceType());
		orderItemPrice.setRecurringChargePeriod(pop.getRecurringChargePeriodType());
		orderItemPrice.setPrice(itemPrice);
		
		logger.info("Calculated item base price: {} euro", orderItemPrice.getPrice().getDutyFreeAmount().getValue());
		
		return orderItemPrice;
	}
	
}
