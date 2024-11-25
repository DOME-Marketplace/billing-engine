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
		OrderPrice chPrice;
		float itemTotalPrice = 0;
		for (Characteristic ch : productChars) {
			logger.debug("Calculating price for Characteristic: '{}' value '{}'", ch.getName(), ch.getValue());
			matchingPop = findMatchingPop(ch, bundledPops);
			Assert.state(matchingPop != null , String.format("Error! No matching POP found for Characteristic: '%s' value '%s'", ch.getName(), ch.getValue()));
			chPrice = calculatePriceForCharacteristic(orderItem, matchingPop, ch);
			itemTotalPrice += chPrice.getPrice().getDutyFreeAmount().getValue();
			orderItem.addItemPriceItem(chPrice);
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
		
		logger.info("Calculated item '{}' base price: {} euro", orderItem.getId(), orderItemPrice.getPrice().getDutyFreeAmount().getValue());
		
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
	protected ProductOfferingPrice findMatchingPop(Characteristic characteristic, List<ProductOfferingPriceMatcher> bundledPops) {
		for (ProductOfferingPriceMatcher popm : bundledPops) {
			if (popm.match(characteristic))
				return popm.getPop();
		}
		
		return null;
	}
	
	// TODO: must include price alterations
	protected OrderPrice calculatePriceForCharacteristic(ProductOrderItem orderItem, ProductOfferingPrice pop, Characteristic ch) {
		final OrderPrice chOrderPrice = new OrderPrice();
		chOrderPrice.setName(pop.getName());
		chOrderPrice.setDescription(pop.getDescription());
		chOrderPrice.setPriceType(pop.getPriceType());
		chOrderPrice.setRecurringChargePeriod(pop.getRecurringChargePeriodType());
		
		final Price chPrice = new Price();
		EuroMoney chAmount = null;
		final String chName = ch.getName();
		final Double chOrderValue = Double.parseDouble(ch.getValue().toString());

		if (PriceUtils.isFixedPrice(pop)) {
			chAmount = new EuroMoney(orderItem.getQuantity() * pop.getPrice().getValue());
			logger.info("Calculated price for Characteristic: '{}' value '{}', using price: '{}', calculated price: {}", 
					chName, chOrderValue, pop.getName(), chAmount.getAmount());
		} else {
			final Quantity unitOfMeasure = pop.getUnitOfMeasure();
			chAmount = new EuroMoney(orderItem.getQuantity() * ((pop.getPrice().getValue() * chOrderValue) / unitOfMeasure.getAmount()));
			logger.info("Calculated price for Characteristic: '{}' value '{}', using price: '{}' per '{} {}', calculated price: {}", 
					chName, chOrderValue, pop.getPrice().getValue(), chOrderValue, unitOfMeasure.getUnits(), chAmount.getAmount());
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
		
		public boolean match(Characteristic orderChar) {
			final String name = orderChar.getName();
			final Object value = orderChar.getValue();
			// logger.debug("Evaluating match between Pop '{}' with Characteristic '{}' value '{}' ", this.pop.getName(), name, value);

			if (pop.getProdSpecCharValueUse() == null) {
				logger.warn("Pop {}({}) has no related ProdSpecCharValueUse.", this.pop.getId(), this.pop.getName());
				return false;
			}
			
			// filters only Characteristics having name equals to the passed name
			var matchingChars = pop.getProdSpecCharValueUse()
			.stream()
			.filter(pscvu -> name.equalsIgnoreCase(pscvu.getName()))
			.toList();
			
			// checks the value of the two characteristics to verify if they match
			for (ProductSpecificationCharacteristicValueUse priceChar : matchingChars) {
				for (CharacteristicValueSpecification priceCharValue : priceChar.getProductSpecCharacteristicValue()) {
					if (PriceUtils.isFixedPrice(pop)) {
						System.out.println("Invoking isSameValue()");
						if (isSameValue(orderChar, priceCharValue)) {
							logger.debug("Found perfect match between Pop '{}' with Characteristic '{}' value '{}' ", this.pop.getName(), name, value);
							return true;
						}
					} else {
						logger.debug("Found match between Pop '{}' with Characteristic '{}' value '{}' ", this.pop.getName(), name, value);
						return true;
					}
				}
			}

			return false;
		}
		
		/*
		 * 
		 */
		private boolean isSameValue(Characteristic orderChar, CharacteristicValueSpecification priceChar) {
			//if (!orderChar.getValueType().equalsIgnoreCase(priceChar.getValueType()))
			//	return false;
						
			if ("number".equalsIgnoreCase(orderChar.getValueType())) {
				double orderCharValue = 0;
				if (orderChar.getValue() instanceof String) 
					orderCharValue = Double.parseDouble(orderChar.getValue().toString());
				else if (orderChar.getValue() instanceof Double)
					orderCharValue = (Double)orderChar.getValue();
				else if (orderChar.getValue() instanceof Integer)
					orderCharValue = (Integer)orderChar.getValue();

				double priceCharValue = 0;
				if (priceChar.getValue() instanceof String) 
					priceCharValue = Double.parseDouble(priceChar.getValue().toString());
				else if (priceChar.getValue() instanceof Double)
					priceCharValue = (Double)priceChar.getValue();
				else if (priceChar.getValue() instanceof Integer)
					priceCharValue = (Integer)priceChar.getValue();
				
				return orderCharValue == priceCharValue;
			} else if ("string".equalsIgnoreCase(orderChar.getValueType())) {
				return orderChar.getValue().toString().equalsIgnoreCase(priceChar.getValue().toString());
			}
			
			return false;
		}
		
		public ProductOfferingPrice getPop() {
			return this.pop;
		}
	}
	
}
