package it.eng.dome.billing.engine.price;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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

import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.billing.engine.tmf.TmfApiFactory;
import it.eng.dome.tmforum.tmf620.v4.ApiClient;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingPriceApi;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.Quantity;
import it.eng.dome.tmforum.tmf622.v4.model.Characteristic;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;

@Component(value = "bundledPriceCalculator")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BundledPriceCalculator implements PriceCalculator, InitializingBean {

	@Autowired
	private TmfApiFactory tmfApiFactory;
	
	@Autowired
	private PriceAlterationCalculator priceAlterationCalculator;
	
	private ProductOfferingPriceApi popApi;
	
    private final Logger logger = LoggerFactory.getLogger(BundledPriceCalculator.class);
	
	@Override
	public void afterPropertiesSet() throws Exception {
		final ApiClient apiClient = tmfApiFactory.getTMF620ProductCatalogApiClient();
	    popApi = new ProductOfferingPriceApi(apiClient);
	}
	
	/**
	 * A BundledPrice is a composite price, where there is one container price and
	 * several contained prices. Each contained price is the price of one of 
	 * the Characteristics selected by the customer. 
	 * 
	 * An Order is composed of order items and every order items is composed by Characteristics
	 * each one with its own price and its own quantity defined by the customer.
	 */
	@Override
	public OrderPrice calculatePrice(ProductOrderItem orderItem, ProductOfferingPrice pop) throws Exception {
		logger.debug("Starting bundled price calculation...");
		Assert.state(orderItem.getProduct() != null, "Error! Cannot calculate price for a ProductOrderItem with a null Product");
		// gets the characteristic chosen by the Customer
		final var productChars = orderItem.getProduct().getProductCharacteristic();
		Assert.state(!CollectionUtils.isEmpty(productChars), "Error! Cannot calculate price for a ProductOrder with a null or empty ProductCharacteristic");
		
		// retrieves the OrderPrice instance linked to the ProductOfferingPrice received
		Optional<OrderPrice> orderPriceOpt = orderItem.getItemTotalPrice()
		.stream()
		.filter( op -> (op.getProductOfferingPrice() != null && op.getProductOfferingPrice().getId().equals(pop.getId())))
		.findFirst();
		Assert.state(orderPriceOpt.isPresent(), "Error! Cannot retrieve OrderPrice instance linked to POP: " + pop.getId());
		final OrderPrice orderItemPrice = orderPriceOpt.get();
		
		// Retrieves list of bundled prices of pop
		final PriceMatcher priceMatcher = new PriceMatcher();
		final List<ProductOfferingPrice> bundledPops = getBundledPops(pop);
		priceMatcher.initialize(bundledPops);
		
		// calculates price for each characteristic
		if (orderItem.getItemPrice() != null)
			orderItem.setItemPrice(new ArrayList<OrderPrice>());
			
		ProductOfferingPrice matchingPop;
		OrderPrice characteristicPrice;
		float itemTotalPrice = 0;
		final Date today = new Date();
		for (Characteristic productCharacteristic : productChars) {
			logger.debug("Calculating price for Characteristic: '{}' value '{}'", productCharacteristic.getName(), productCharacteristic.getValue());
			// find matching POP
			matchingPop = priceMatcher.match(productCharacteristic, today);
			Assert.state(matchingPop != null , String.format("Error! No matching POP found for Characteristic: '%s' value '%s'", productCharacteristic.getName(), productCharacteristic.getValue()));
			// calculates the base price of the Characteristic 
			characteristicPrice = calculatePriceForCharacteristic(orderItem, matchingPop, productCharacteristic);
			
	    	// applies price alterations
			if (PriceUtils.hasRelationships(matchingPop)) {
				priceAlterationCalculator.applyAlterations(orderItem, matchingPop, characteristicPrice);
				logger.info("Price of Characteristic '{}' '{}' after alterations: {} euro", 
						productCharacteristic.getName(), productCharacteristic.getValue(), PriceUtils.getAlteredDutyFreePrice(characteristicPrice));
				
				itemTotalPrice += PriceUtils.getAlteredDutyFreePrice(characteristicPrice);
			} else {
				itemTotalPrice += characteristicPrice.getPrice().getDutyFreeAmount().getValue();
			}
			
			orderItem.addItemPriceItem(characteristicPrice);
		}
				
		EuroMoney euro = new EuroMoney(itemTotalPrice);
		final Price itemPrice = new Price();
		itemPrice.setDutyFreeAmount(euro.toMoney());
		itemPrice.setTaxIncludedAmount(null);
		orderItemPrice.setName(pop.getName());
		orderItemPrice.setDescription(pop.getDescription());
		orderItemPrice.setPriceType(pop.getPriceType());
		orderItemPrice.setRecurringChargePeriod(pop.getRecurringChargePeriodType());
		orderItemPrice.setPrice(itemPrice);
		orderItem.addItemTotalPriceItem(orderItemPrice);
		
		logger.info("Item '{}' total price: {} euro", orderItem.getId(), orderItemPrice.getPrice().getDutyFreeAmount().getValue());
		
		return orderItemPrice;
	}
	
	
	protected List<ProductOfferingPrice> getBundledPops(ProductOfferingPrice pop) throws Exception {
		final List<ProductOfferingPrice> bundledPops = new ArrayList<ProductOfferingPrice>();
		
		for (var bundledPopRel : pop.getBundledPopRelationship()) {
			logger.debug("Retrieving remote POP with id: '{}'", bundledPopRel.getId());
			try {
				bundledPops.add(popApi.retrieveProductOfferingPrice(bundledPopRel.getId(), null));
			} catch (ApiException exc) {
				if (exc.getCode() == HttpStatus.NOT_FOUND.value())
					throw new IllegalStateException(String.format("ProductOfferingPrice with id %s not found on server!", bundledPopRel.getId()));
				
				throw new Exception(exc); //throw exc;
			}
		}
		
		return bundledPops;
	}
	
	/*
	 * 
	 */
	protected OrderPrice calculatePriceForCharacteristic(ProductOrderItem orderItem, ProductOfferingPrice pop, Characteristic ch) {
		final OrderPrice chOrderPrice = new OrderPrice();
		chOrderPrice.setName(pop.getName());
		chOrderPrice.setDescription(pop.getDescription());
		chOrderPrice.setPriceType(pop.getPriceType());
		chOrderPrice.setRecurringChargePeriod(pop.getRecurringChargePeriodType());
		
		final Price chPrice = new Price();
		EuroMoney chAmount;
		final String chName = ch.getName();
		final Double chOrderValue = Double.parseDouble(ch.getValue().toString());

		if (PriceUtils.isForfaitPrice(pop)) {
			chAmount = new EuroMoney(orderItem.getQuantity() * pop.getPrice().getValue());
			logger.info("Price of Characteristic '{}' '{}' [quantity: {}, price: '{}'] = {} euro", 
					chName, chOrderValue, orderItem.getQuantity(), pop.getPrice().getValue(), chAmount.getAmount());
		} else {
			final Quantity unitOfMeasure = pop.getUnitOfMeasure();
			chAmount = new EuroMoney(orderItem.getQuantity() * ((pop.getPrice().getValue() * chOrderValue) / unitOfMeasure.getAmount()));
			logger.info("Price of Characteristic '{}' [quantity: {}, value: '{} {}', price: '{}' per '{} {}'] = {} euro", 
					chName, orderItem.getQuantity(), chOrderValue, unitOfMeasure.getUnits(),
					pop.getPrice().getValue(), unitOfMeasure.getAmount(), unitOfMeasure.getUnits(), chAmount.getAmount());
		}
		
		chPrice.setDutyFreeAmount(chAmount.toMoney());
		chPrice.setTaxIncludedAmount(null);
		chOrderPrice.setPrice(chPrice);
		
		return chOrderPrice;
	}
	
}
