package it.eng.dome.billing.engine.model;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import it.eng.dome.billing.engine.service.PriceAlterationCalculator;
import it.eng.dome.billing.engine.service.PriceAlterationFactory;
import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingPriceApi;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Money;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOfferingPriceRef;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;

public class BillingOrderItem {
	private ProductOrderItem orderItem;
	private OrderPrice itemOrderPrice;
	private ProductOfferingPriceApi offeringPriceApi;
    private final Logger logger = LoggerFactory.getLogger(BillingOrderItem.class);
	
	public BillingOrderItem(ProductOrderItem orderItem, ProductOfferingPriceApi offeringPriceApi) {
		super();
		this.orderItem = orderItem;
		this.offeringPriceApi = offeringPriceApi;
	}

	public float getPriceAmount() {
		if (itemOrderPrice.getPriceAlteration() != null && itemOrderPrice.getPriceAlteration().size() > 0) {
			PriceAlteration alteredPrice = itemOrderPrice.getPriceAlteration().get(itemOrderPrice.getPriceAlteration().size() - 1);
			return alteredPrice.getPrice().getDutyFreeAmount().getValue();
		} else
			return itemOrderPrice.getPrice().getDutyFreeAmount().getValue();
	}
	
	protected void setOrderItemPrice() throws Exception {
		// 0) Validation
		Assert.state(orderItem.getQuantity() > 0, "Invalid state: ProductOrderItem cannot have quantity less or equal to zero.");
		Assert.state(orderItem.getItemPrice() != null && orderItem.getItemPrice().size() > 0, "Invalid state: ProductOrderItem has no order item price.");

		// 1) retrieves the first active referred ProductOfferingPrice of ProductOrderItem
		final OrderPriceAndProductOfferingPrice priceAndPOP = this.getFirstActivePOP();
		Assert.state(priceAndPOP != null, "Invalid state: ProductOrderItem has no active referenced ProductOfferingPrice.");
		itemOrderPrice = priceAndPOP.itemPrice;
		final ProductOfferingPrice offeringPrice = priceAndPOP.productOfferingPrice;
		
		// 2) calculates the price amount of the order item
		final Price itemPrice = new Price();
		EuroMoney euro = new EuroMoney(offeringPrice.getPrice().getValue() * orderItem.getQuantity());
		logger.debug("Calculated base price {} for order item {}", euro.getAmount(), orderItem.getId());

		Money itemPriceAmount = new Money();
		itemPriceAmount.setUnit(euro.getCurrency());
		itemPriceAmount.setValue(euro.getAmount());
		itemPrice.setDutyFreeAmount(itemPriceAmount);
		itemPrice.setTaxIncludedAmount(null);
		itemOrderPrice.setName(offeringPrice.getName());
		itemOrderPrice.setDescription(offeringPrice.getDescription());
		itemOrderPrice.setPriceType(offeringPrice.getPriceType());
		itemOrderPrice.setRecurringChargePeriod(offeringPrice.getRecurringChargePeriodType());
		itemOrderPrice.setPrice(itemPrice);
		
		// checks if prices alteration must applied
		if (offeringPrice.getPopRelationship() != null) {
			var itemAlteredPrice = itemPriceAmount.getValue();
			var alterations = offeringPrice.getPopRelationship().iterator();
			ProductOfferingPrice alterationPOP;
			PriceAlterationCalculator alterationCalculator;
			PriceAlteration alteredPrice;
			// loops for all the alterations
			while (alterations.hasNext()) {
				alterationPOP = offeringPriceApi.retrieveProductOfferingPrice(alterations.next().getId(), null);
				// alteration must be active
				if (!"active".equalsIgnoreCase(alterationPOP.getLifecycleStatus()))
					continue;
				// alteration type must be one of the types known
				alterationCalculator = PriceAlterationFactory.getPriceAlterationCalculator(alterationPOP);
				if (alterationCalculator == null)
					continue;
				
				logger.debug("Applying alteration '{}' on base price", alterationPOP.getPriceType());
				alteredPrice = alterationCalculator.applyAlteration(itemAlteredPrice);
				itemOrderPrice.addPriceAlterationItem(alteredPrice);
			}
		}
		
		logger.debug("Item {} price = {}", orderItem.getId(), getPriceAmount());
	}
	
	
	/*
	 * Several instances of OrderPrice may be linked to a ProductOrderItem,
	 * to calculate the price, it is used the first active ProductOfferingPrice found.
	 */
	private OrderPriceAndProductOfferingPrice getFirstActivePOP() throws ApiException {
		var itemPricesIterator = orderItem.getItemPrice().iterator();
		OrderPrice currentOrderPrice;
		ProductOfferingPriceRef currentPOPRef;
		ProductOfferingPrice currentPOP;
		while (itemPricesIterator.hasNext()) {
			currentOrderPrice = itemPricesIterator.next();
			currentPOPRef = currentOrderPrice.getProductOfferingPrice();
			if (currentPOPRef == null || StringUtils.isBlank(currentPOPRef.getId()))
				continue;
			
			currentPOP = offeringPriceApi.retrieveProductOfferingPrice(currentPOPRef.getId(), null);
			if (!"active".equalsIgnoreCase(currentPOP.getLifecycleStatus()))
				continue;
			
			return new OrderPriceAndProductOfferingPrice(currentOrderPrice, currentPOP);
		}
		
		return null;
	}

	
	private class OrderPriceAndProductOfferingPrice {
		private OrderPrice itemPrice;
		private ProductOfferingPrice productOfferingPrice;
		
		public OrderPriceAndProductOfferingPrice(OrderPrice orderPrice, ProductOfferingPrice productOfferingPrice) {
			super();
			this.itemPrice = orderPrice;
			this.productOfferingPrice = productOfferingPrice;
		}
	}
}
