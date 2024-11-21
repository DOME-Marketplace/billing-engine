package it.eng.dome.billing.engine.price;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.billing.engine.tmf.TmfApiFactory;
import it.eng.dome.tmforum.tmf620.v4.ApiClient;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingPriceApi;
import it.eng.dome.tmforum.tmf620.v4.model.CharacteristicValueSpecification;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductSpecificationCharacteristicValueUse;
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
	
	private ProductOfferingPriceApi popApi;
	
    private final Logger logger = LoggerFactory.getLogger(BundledPriceCalculator.class);
	
	@Override
	public void afterPropertiesSet() throws Exception {
		final ApiClient apiClient = tmfApiFactory.getTMF620ProductCatalogApiClient();
	    popApi = new ProductOfferingPriceApi(apiClient);
	}
	
	@Override
	public OrderPrice calculatePrice(ProductOrderItem orderItem, ProductOfferingPrice pop) throws Exception {
		Assert.state(orderItem.getProduct() != null, "Error! Cannot calculate price for a ProductOrderItem with a null Product");
		// gets the characteristic chosen by the Customer
		final var productChars = orderItem.getProduct().getProductCharacteristic();
		Assert.state(productChars != null && productChars.size() > 0, "Error! Cannot calculate price for a ProductOrder with a null or empty ProductCharacteristic");
		
		// retrieves the OrderPrice instance linked to the ProductOfferingPrice received
		Optional<OrderPrice> orderPriceOpt = orderItem.getItemPrice()
		.stream()
		.filter( op -> (op.getProductOfferingPrice() != null && op.getProductOfferingPrice().getId().equals(pop.getId())))
		.findFirst();
		Assert.state(orderPriceOpt.isPresent(), "Error! Cannot retrieve OrderPrice instance linked to POP: " + pop.getId());
		final OrderPrice orderItemPrice = orderPriceOpt.get();
		
		// Retrieves list of bundled prices of pop
		List<ProductOfferingPriceMatcher> bundledPops = getBundledPops(pop);
		// calculates price for each characteristic
		if (orderItem.getItemPrice() != null)
			orderItem.setItemPrice(new ArrayList<OrderPrice>());
			
		ProductOfferingPrice matchingPop;
		String chName;
		Object chValue;
		OrderPrice chPrice;
		float itemTotalPrice = 0;
		for (Characteristic ch : productChars) {
			chName = ch.getName();
			chValue = ch.getValue();
			matchingPop = findMatchingPop(chName, chValue, bundledPops);
			Assert.state(matchingPop != null , String.format("Error! Not matching POP found for Characteristic: %s with value %s ", chName, chValue));
			chPrice = calculatePriceForCharacteristic(orderItem, pop, chName, chValue);
			itemTotalPrice += chPrice.getPrice().getDutyFreeAmount().getValue();
			orderItem.addItemPriceItem(chPrice);
		}
				
		final Price itemPrice = new Price();
		EuroMoney euro = new EuroMoney(itemTotalPrice);
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
	
	
	protected List<ProductOfferingPriceMatcher> getBundledPops(ProductOfferingPrice pop) throws ApiException {
		final List<ProductOfferingPriceMatcher> bundledPops = new ArrayList<ProductOfferingPriceMatcher>();
		var bundledPopRels = pop.getBundledPopRelationship().iterator();
		
		while (bundledPopRels.hasNext()) {
			bundledPops.add(
				new ProductOfferingPriceMatcher(popApi.retrieveProductOfferingPrice(bundledPopRels.next().getId(), null))
			);
		}
		
		return bundledPops;
	}
	
	// TODO: must consider also the unit for fixed-price or dynamic-price
	protected ProductOfferingPrice findMatchingPop(String name, Object value, List<ProductOfferingPriceMatcher> bundledPops) {
		for (ProductOfferingPriceMatcher popm : bundledPops) {
			if (popm.match(name, value))
				return popm.getPop();
		}
		
		return null;
	}
	
	// TODO: must include price alterations
	protected OrderPrice calculatePriceForCharacteristic(ProductOrderItem orderItem, ProductOfferingPrice pop, String name, Object value) {
		final OrderPrice chOrderPrice = new OrderPrice();
		chOrderPrice.setName(pop.getName());
		chOrderPrice.setDescription(pop.getDescription());
		chOrderPrice.setPriceType(pop.getPriceType());
		chOrderPrice.setRecurringChargePeriod(pop.getRecurringChargePeriodType());
		
		final Price chPrice = new Price();
		EuroMoney chAmount = null;

		if (PriceUtils.asFixedPrice(pop)) {
			chAmount = new EuroMoney(orderItem.getQuantity() * pop.getPrice().getValue());
			logger.info("Calculated price for Characteristic: '{}', with value: '{}', using total price of: {}, final price: {}", 
					name, value, pop.getId(), pop.getPrice().getValue(), chAmount.getAmount());
		} else {
			final float chQuantity = Float.parseFloat(value.toString());
			final Quantity unitOfMeasure = pop.getUnitOfMeasure();
			chAmount = new EuroMoney(orderItem.getQuantity() * ((pop.getPrice().getValue() * chQuantity) / unitOfMeasure.getAmount()));
			logger.info("Calculated price for Characteristic: '{}', with value: '{}', using price of: {} per unit, final price: {}", 
					name, value, pop.getId(), pop.getPrice().getValue(), chAmount.getAmount());
		}
		chPrice.setDutyFreeAmount(chAmount.toMoney());
		chPrice.setTaxIncludedAmount(null);
		chOrderPrice.setPrice(chPrice);
		
		return chOrderPrice;
	}
	
	
	/*
	 * Implemented only to provide business logic for matching 
	 */
	class ProductOfferingPriceMatcher {
		private ProductOfferingPrice pop;
		
		ProductOfferingPriceMatcher(ProductOfferingPrice pop) {
			this.pop = pop;
		}
		
		public boolean match(String name, Object value) {
			var matchingChars = pop.getProdSpecCharValueUse()
			.stream()
			.filter(pscvu -> name.equalsIgnoreCase(pscvu.getName()))
			.toList();
			
			for (ProductSpecificationCharacteristicValueUse pscvu : matchingChars) {
				for (CharacteristicValueSpecification cvs : pscvu.getProductSpecCharacteristicValue()) {
					if (value.equals(cvs.getValue())) 
						return true;
				}
			}

			return false;
		}
		
		public ProductOfferingPrice getPop() {
			return this.pop;
		}
	}
	
}
