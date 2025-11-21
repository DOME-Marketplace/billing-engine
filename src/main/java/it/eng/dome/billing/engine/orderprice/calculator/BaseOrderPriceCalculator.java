package it.eng.dome.billing.engine.orderprice.calculator;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.billing.engine.utils.OrderPriceUtils;
import it.eng.dome.billing.engine.utils.TMForumEntityUtils;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;

public class BaseOrderPriceCalculator extends AbstractBaseOrderPriceCalculator{
	
	private final Logger logger = LoggerFactory.getLogger(BaseOrderPriceCalculator.class);
	
	@Autowired
	private PriceAlterationCalculator priceAlterationCalculator; 

	public BaseOrderPriceCalculator(ProductOfferingPrice pop, ProductOrderItem productOrderItem) {
		super(pop,productOrderItem);
	}

	@Override
	public List<OrderPrice> calculateOrderPrice() throws BillingEngineValidationException, ApiException{
		
		List<OrderPrice> orderPriceList=new ArrayList<OrderPrice>();
		
		// Checks is unitOfMeasure is null or "1 unit"
		tmfEntityValidator.validateUnitOfMeasureForSinglePrice(pop);
		// Checks if Price is well formed 
		tmfEntityValidator.validatePrice(pop);
		
		logger.info("Calculating price preview for ProductOfferingPrice '{}' of ProductOrderItem", pop.getId(), productOrderItem.getId());
		Float orderPriceValue=(pop.getPrice().getValue() * productOrderItem.getQuantity());
		
		Money money=new Money(priceCurrency,orderPriceValue);
		Price price= TMForumEntityUtils.createPriceTMF622(money);
		OrderPrice op=TMForumEntityUtils.createOrderPriceTMF622(price, pop);
			
		logger.info("Price of ProductOfferingPrice '{}' in ProductOrdeIem '{}': [quantity: {}, price: '{}'] = {} {}", 
				pop.getId(), productOrderItem.getId(), productOrderItem.getQuantity(), pop.getPrice().getValue(), money.getValue(), priceCurrency);
								
		// apply price alterations
		if (ProductOfferingPriceUtils.hasRelationships(pop)) {
			OrderPrice updatedOrderPrice=priceAlterationCalculator.applyAlterations(op,ProductOfferingPriceUtils.getProductOfferingPriceRelationships(pop.getPopRelationship(), productCatalogManagementApis));
								
			logger.info("Price of ProductOfferingPrice '{}' after alterations = {} {}", 
					pop.getId(), OrderPriceUtils.getAlteredDutyFreePrice(updatedOrderPrice),priceCurrency);	
		
			orderPriceList.add(updatedOrderPrice);
		}else {
			orderPriceList.add(op);
		}
			
		return orderPriceList;
		
	}

	
}
