package it.eng.dome.billing.engine.price;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.billing.utils.BillingPriceType;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.Quantity;
import it.eng.dome.tmforum.tmf622.v4.model.Characteristic;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;

@Component(value = "bundledPriceCalculator")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BundledPriceCalculator implements PriceCalculator {
	
	@Autowired
	private PriceAlterationCalculator priceAlterationCalculator;
	
	private ProductCatalogManagementApis productCatalogManagementApis;
	
    private final Logger logger = LoggerFactory.getLogger(BundledPriceCalculator.class);
    
    
    public BundledPriceCalculator(ProductCatalogManagementApis productCatalogManagementApis) {
    	this.productCatalogManagementApis = productCatalogManagementApis;
    }
	
	
	/*
	 * Calculates the prices (i.e., OrderPrice instance) for a bundled ProductOfferingPrice referenced in the specified ProductOrderItem.
	 * A BundledPrice is a composite price, where there is one container price and
	 * several contained prices. A contained price could be the price of one of 
	 * the Characteristics selected by the customer. 
	 * 
	 * A ProductOrder is composed of ProductOrderItems and every ProductOrderItem can be composed by Characteristics
	 * each one with its own price and its own quantity defined by the customer.
	 */
	@Override
	public List<OrderPrice> calculateOrderPrice(ProductOrderItem orderItem, ProductOfferingPrice pop) throws Exception {
			
		logger.info("Starting bundled price calculation...");
		try {
			List<OrderPrice> orderPriceList=new ArrayList<OrderPrice>();
			if (orderItem.getItemPrice() != null)
				orderItem.setItemPrice(new ArrayList<OrderPrice>());
				
			
			if(orderItem.getProduct()==null) {
				throw new BillingBadRequestException(String.format("Error! Cannot calculate bundled price for a ProductOrderItem of a ProductOrder with a null Product"));
			}
			
			// gets the characteristic chosen by the Customer
			final var productChars = orderItem.getProduct().getProductCharacteristic();
			
			if(CollectionUtils.isEmpty(productChars)) {
				throw new BillingBadRequestException(String.format("Error! Cannot calculate bundled price for a ProductOrder with a null or empty ProductCharacteristic"));
			}
			
			// Retrieves list of bundled prices of pop
			final PriceMatcher priceMatcher = new PriceMatcher();
			final List<ProductOfferingPrice> bundledPops = getBundledPops(pop);
			if(bundledPops==null||bundledPops.isEmpty()) {
				throw new BillingBadRequestException(String.format("Error! Started calculation of bundled ProductOfferingPrice %s but the 'bundledPopRelationship' is empty!" + pop.getId()));
			}else {
				for(ProductOfferingPrice bundledPop: bundledPops) {
					if(bundledPop.getProdSpecCharValueUse()==null || bundledPop.getProdSpecCharValueUse().isEmpty()) {
						OrderPrice op=PriceUtils.calculateOrderPrice(bundledPop, orderItem, priceAlterationCalculator);
					    op.setProductOfferingPrice(PriceUtils.createProductOfferingPriceRef(bundledPop));
						orderPriceList.add(op);
						// updated itemPrice element of the ProductOrderItem
						orderItem.addItemPriceItem(op);
					}
					else {
						priceMatcher.addPrice(bundledPop);
					}
				}
			}

			ProductOfferingPrice matchingPop;
			OrderPrice characteristicPrice;
			//float itemTotalPrice = 0;
			final Date today = new Date();
			for (Characteristic productCharacteristic : productChars) {
				logger.info("Calculating price for Characteristic: '{}' value '{}'", productCharacteristic.getName(), productCharacteristic.getValue());
				// find matching POP
				matchingPop = priceMatcher.match(productCharacteristic, today);
				if(matchingPop!=null) {
				
					// calculates the base price of the Characteristic 
					characteristicPrice = calculatePriceForCharacteristic(orderItem, matchingPop, productCharacteristic);
				
					// applies price alterations
					if (PriceUtils.hasRelationships(matchingPop)) {
						priceAlterationCalculator.applyAlterations(orderItem, matchingPop, characteristicPrice);
						logger.info("Price of ProductOfferingPrice '{}' with Characteristic '{}' '{}' after alterations: {} euro", pop.getId(),
							productCharacteristic.getName(), productCharacteristic.getValue(), PriceUtils.getAlteredDutyFreePrice(characteristicPrice));
					
						//itemTotalPrice += PriceUtils.getAlteredDutyFreePrice(characteristicPrice);
					} /*else {
						itemTotalPrice += characteristicPrice.getPrice().getDutyFreeAmount().getValue();
					}*/
					
					orderItem.addItemPriceItem(characteristicPrice);
					orderPriceList.add(characteristicPrice);
				}else {
					logger.info("No matching ProductOfferingPrice found for Characteristic: '{}' value '{}", productCharacteristic.getName(), productCharacteristic.getValue());
				}
			}
		
			return orderPriceList;
			
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
			// Java exception is converted into HTTP status code by the ControllerExceptionHandler
			throw new Exception(e); //throw (e.getCause() != null) ? e.getCause() : e;
		}
	}
	
    /*
     * Returns the list of ProductOfferingPrice referenced in the bundle ProductOfferingPrice (i.e., stored in the bundledPopRelationship element of the ProductOfferingPrice)
     */
	protected List<ProductOfferingPrice> getBundledPops(ProductOfferingPrice pop) throws Exception {
		final List<ProductOfferingPrice> bundledPops = new ArrayList<ProductOfferingPrice>();
		
		for (var bundledPopRel : pop.getBundledPopRelationship()) {
			logger.debug("Retrieving remote ProductOfferingPrice with id: '{}'", bundledPopRel.getId());
			
			ProductOfferingPrice productOfferingPrice = productCatalogManagementApis.getProductOfferingPrice(bundledPopRel.getId(), null);
			if (productOfferingPrice != null) {
				bundledPops.add(productCatalogManagementApis.getProductOfferingPrice(bundledPopRel.getId(), null));
			}else {
				throw (IllegalStateException)new IllegalStateException(String.format("ProductOfferingPrice with id %s not found on server!", bundledPopRel.getId()));
			}
			
			/*try {
				bundledPops.add(productOfferingPriceApis.getProductOfferingPrice(bundledPopRel.getId(), null));
			  } catch (ApiException exc) {
				if (exc.getCode() == HttpStatus.NOT_FOUND.value()) {
					throw (IllegalStateException)new IllegalStateException(String.format("ProductOfferingPrice with id %s not found on server!", bundledPopRel.getId())).initCause(exc);
				}
				throw exc;
			}*/	
		}
		
		return bundledPops;
	}
	
	/*
	 * Calculate the OrderPrice element for a ProductOfferingPrice and a Characteristic according to the quantity specified on the ProductOrderItem
	 */
	protected OrderPrice calculatePriceForCharacteristic(ProductOrderItem orderItem, ProductOfferingPrice pop, Characteristic ch) {
		final OrderPrice chOrderPrice = new OrderPrice();
		chOrderPrice.setName(pop.getName());
		chOrderPrice.setDescription(pop.getDescription());
		String priceTypeNormalized=BillingPriceType.normalize(pop.getPriceType());
		chOrderPrice.setPriceType(priceTypeNormalized);
		chOrderPrice.setProductOfferingPrice(PriceUtils.createProductOfferingPriceRef(pop));
		
		//if(!("one time".equalsIgnoreCase(pop.getPriceType())) && !("one-time".equalsIgnoreCase(pop.getPriceType()))) {
		if(!priceTypeNormalized.equalsIgnoreCase(BillingPriceType.ONE_TIME.getNormalizedKey())) {
			chOrderPrice.setRecurringChargePeriod(pop.getRecurringChargePeriodLength()+" "+pop.getRecurringChargePeriodType());
		}
		
		final Price chPrice = new Price();
		EuroMoney chAmount;
		final String chName = ch.getName();
		final Double chOrderValue = Double.parseDouble(ch.getValue().toString());

		if (PriceUtils.isForfaitPrice(pop)) {
			chAmount = new EuroMoney(orderItem.getQuantity() * pop.getPrice().getValue());
			logger.info("Price of ProductOfferingPrice '{}' with Characteristic '{}' '{}' [quantity: {}, price: '{}'] = {} euro", pop.getId(),
					chName, chOrderValue, orderItem.getQuantity(), pop.getPrice().getValue(), chAmount.getAmount());
		} else {
			final Quantity unitOfMeasure = pop.getUnitOfMeasure();
			chAmount = new EuroMoney(orderItem.getQuantity() * ((pop.getPrice().getValue() * chOrderValue) / unitOfMeasure.getAmount()));
			logger.info("Price of of ProductOfferingPrice '{}' with Characteristic '{}' [quantity: {}, value: '{} {}', price: '{}' per '{} {}'] = {} euro", 
					pop.getId(), chName, orderItem.getQuantity(), chOrderValue, unitOfMeasure.getUnits(),
					pop.getPrice().getValue(), unitOfMeasure.getAmount(), unitOfMeasure.getUnits(), chAmount.getAmount());
		}
		
		chPrice.setDutyFreeAmount(chAmount.toMoney());
		chPrice.setTaxIncludedAmount(null);
		chOrderPrice.setPrice(chPrice);
		
		return chOrderPrice;
	}
	
	/*
	 * Calculates the prices (i.e., OrderPrice instance) for a bundled ProductOfferingPrice referenced in the specified ProductOrderItem.
	 * A BundledPrice is a composite price, where there is one container price and
	 * several contained prices. A contained price could be the price of one of 
	 * the Characteristics selected by the customer or a pay-per-use plan. In case of a pay-per-use plan will be considered the usage data provided in input to calculate the price. 
	 * 
	 * A ProductOrder is composed of ProductOrderItems and every ProductOrderItem can be composed by Characteristics
	 * each one with its own price and its own quantity defined by the customer.
	 */
	@Override
	public List<OrderPrice> calculateOrderPriceForUsageCharacteristic(ProductOrderItem orderItem, ProductOfferingPrice pop,List<UsageCharacteristic> usageChForMetric) throws Exception {
			
		logger.debug("Starting bundled price calculation for order item {} and pay-per-use price plan...");
		try {
			List<OrderPrice> orderPriceList=new ArrayList<OrderPrice>();
			if (orderItem.getItemPrice() != null)
				orderItem.setItemPrice(new ArrayList<OrderPrice>());
				
			
			if(orderItem.getProduct()==null) {
				throw new BillingBadRequestException(String.format("Error! Cannot calculate bundled price for a ProductOrderItem of a ProductOrder with a null Product"));
			}
			
			// gets the characteristic chosen by the Customer
			final var productChars = orderItem.getProduct().getProductCharacteristic();
			
			if(CollectionUtils.isEmpty(productChars)) {
				throw new BillingBadRequestException(String.format("Error! Cannot calculate bundled price for a ProductOrder with a null or empty ProductCharacteristic"));
			}
			
			// Retrieves list of bundled prices of pop
			final PriceMatcher priceMatcher = new PriceMatcher();
			final List<ProductOfferingPrice> bundledPops = getBundledPops(pop);
			if(bundledPops==null||bundledPops.isEmpty()) {
				throw new BillingBadRequestException(String.format("Error! Started calculation of bundled ProductOfferingPrice %s but the 'bundledPopRelationship' is empty!" + pop.getId()));
			}else {
				for(ProductOfferingPrice bundledPop: bundledPops) {
					
					//Check the pay-per-use price plan
					if(BillingPriceType.normalize(bundledPop.getPriceType()).equalsIgnoreCase(BillingPriceType.PAY_PER_USE.getNormalizedKey()) || BillingPriceType.normalize(bundledPop.getPriceType()).equalsIgnoreCase(BillingPriceType.USAGE.getNormalizedKey())) {
						OrderPrice op=PriceUtils.calculateOrderPriceForUsageCharacterisic(pop, orderItem, priceAlterationCalculator, usageChForMetric);
						op.setProductOfferingPrice(PriceUtils.createProductOfferingPriceRef(bundledPop));
						orderPriceList.add(op);
						// updated itemPrice element of the ProductOrderItem
						orderItem.addItemPriceItem(op);
					}
					if(bundledPop.getProdSpecCharValueUse()==null || bundledPop.getProdSpecCharValueUse().isEmpty()) {
						OrderPrice op=PriceUtils.calculateOrderPrice(bundledPop, orderItem, priceAlterationCalculator);
					    op.setProductOfferingPrice(PriceUtils.createProductOfferingPriceRef(bundledPop));
						orderPriceList.add(op);
						// updated itemPrice element of the ProductOrderItem
						orderItem.addItemPriceItem(op);
					}
					else {
						priceMatcher.addPrice(bundledPop);
					}
				}
			}

			ProductOfferingPrice matchingPop;
			OrderPrice characteristicPrice;

			final Date today = new Date();
			for (Characteristic productCharacteristic : productChars) {
				logger.debug("Calculating price for Characteristic: '{}' value '{}'", productCharacteristic.getName(), productCharacteristic.getValue());
				// find matching POP
				matchingPop = priceMatcher.match(productCharacteristic, today);
				if(matchingPop==null) {
					throw new BillingBadRequestException(String.format("Error! No matching ProductOfferingPrice found for Characteristic: '%s' value '%s'", productCharacteristic.getName(), productCharacteristic.getValue()));
				}
				
				// calculates the base price of the Characteristic 
				characteristicPrice = calculatePriceForCharacteristic(orderItem, matchingPop, productCharacteristic);
				
		    	// applies price alterations
				if (PriceUtils.hasRelationships(matchingPop)) {
					priceAlterationCalculator.applyAlterations(orderItem, matchingPop, characteristicPrice);
					logger.info("Price of Characteristic '{}' '{}' after alterations: {} euro", 
							productCharacteristic.getName(), productCharacteristic.getValue(), PriceUtils.getAlteredDutyFreePrice(characteristicPrice));
					
					//itemTotalPrice += PriceUtils.getAlteredDutyFreePrice(characteristicPrice);
				} /*else {
					itemTotalPrice += characteristicPrice.getPrice().getDutyFreeAmount().getValue();
				}*/
				
				orderItem.addItemPriceItem(characteristicPrice);
				orderPriceList.add(characteristicPrice);
			}
		
			return orderPriceList;
			
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
			// Java exception is converted into HTTP status code by the ControllerExceptionHandler
			throw new Exception(e); //throw (e.getCause() != null) ? e.getCause() : e;
		}
	}
	
	
	
}
