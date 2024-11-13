package it.eng.dome.billing.engine.model;

import java.util.ArrayList;

import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingPriceApi;
import it.eng.dome.tmforum.tmf622.v4.model.Money;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;

public class BillingProductOrder {
	
	private ProductOrder order;
	private ProductOfferingPriceApi offeringPriceApi;

	public BillingProductOrder(ProductOrder order, ProductOfferingPriceApi offeringPriceApi) {
		super();
		this.order = order;
		this.offeringPriceApi = offeringPriceApi;
	}
	
	public OrderPrice calculateOrderPrice() throws Exception {
		float totalOrderValue = 0F;
		BillingOrderItem billingOrderItem;
		
		// request to each order item to calculate its price amount
	    var orderItems = order.getProductOrderItem().iterator();
		while (orderItems.hasNext()) {
			billingOrderItem = new BillingOrderItem(orderItems.next(), offeringPriceApi);
			billingOrderItem.setOrderItemPrice();
			totalOrderValue += billingOrderItem.getPriceAmount();
		}
		
		EuroMoney euro = new EuroMoney(totalOrderValue);
		Money orderTotalPriceAmount = (new Money()).unit(euro.getCurrency()).value(euro.getAmount());

		// set the orderTotalPrice to the ProductOrder
		if (order.getOrderTotalPrice() != null)
			order.setOrderTotalPrice(new ArrayList<OrderPrice>());
		
		OrderPrice orderPrice = new OrderPrice();
		Price orderTotalPrice = new Price();
		orderTotalPrice.setDutyFreeAmount(orderTotalPriceAmount);
		orderTotalPrice.setTaxIncludedAmount(null);
		orderPrice.setPrice(orderTotalPrice);
		order.addOrderTotalPriceItem(orderPrice);
		
		return orderPrice;
	}
	
	

}
