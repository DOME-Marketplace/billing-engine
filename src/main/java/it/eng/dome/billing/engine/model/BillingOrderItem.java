package it.eng.dome.billing.engine.model;

import org.springframework.util.Assert;

import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingPriceApi;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf622.v4.model.Money;

public class BillingOrderItem {
	private ProductOrderItem orderItem;
	private ProductOfferingPriceApi offeringPriceApi;
	private Money orderItemPriceAmount = new Money();

	
	public BillingOrderItem(ProductOrderItem orderItem, ProductOfferingPriceApi offeringPriceApi) {
		super();
		this.orderItem = orderItem;
		this.offeringPriceApi = offeringPriceApi;
	}

	protected void setOrderItemPrice() throws Exception {
		// Validation
		Assert.state(orderItem.getQuantity() > 0, "Invalid state: ProductOrderItem cannot have quantity less or equal to zero.");
		Assert.state(orderItem.getItemPrice() != null && orderItem.getItemPrice().size() > 0, "Invalid state: ProductOrderItem has no order item price.");
		OrderPrice itemPrice = orderItem.getItemPrice().get(0);
		Assert.state(itemPrice.getProductOfferingPrice() != null, "Invalid state: ProductOrderItem has "
				+ "an invalid OrderPrice, it does not refer to an instance of ProductOfferingPrice.");

		// Price calculation
		var offeringPriceRef = itemPrice.getProductOfferingPrice();
		// retrieves the offering price from the AccessNode
		try {
			var offeringPrice = offeringPriceApi.retrieveProductOfferingPrice(offeringPriceRef.getId(), null);
			var calculatedPrice = new Price();
			// calculates the amount of the order item price
			orderItemPriceAmount.setUnit("EUR");
			orderItemPriceAmount.setValue(offeringPrice.getPrice().getValue() * orderItem.getQuantity());
			calculatedPrice.setDutyFreeAmount(orderItemPriceAmount);
			calculatedPrice.setTaxIncludedAmount(null);
			// sets attributes of the 
			itemPrice.setName(offeringPrice.getName());
			itemPrice.setDescription(offeringPrice.getDescription());
			itemPrice.setPriceType(offeringPrice.getPriceType());
			itemPrice.setRecurringChargePeriod(offeringPrice.getRecurringChargePeriodType());
			itemPrice.setPrice(calculatedPrice);
		} catch (Exception e) {
			System.out.println(offeringPriceRef.getId());
			e.printStackTrace();
			throw e;
		}
	}
	
	public float getPriceAmount() {
		return orderItemPriceAmount.getValue();
	}

}
