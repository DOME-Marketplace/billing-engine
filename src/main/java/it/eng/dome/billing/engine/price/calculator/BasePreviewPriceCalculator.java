package it.eng.dome.billing.engine.price.calculator;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.utils.OrderPriceUtils;
import it.eng.dome.billing.engine.utils.TMForumEntityUtils;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;

@Component
public class BasePreviewPriceCalculator extends AbstractPriceCalculator<ProductOrderItem,List<OrderPrice>>{
	
	private final Logger logger = LoggerFactory.getLogger(BasePreviewPriceCalculator.class);
	
	public BasePreviewPriceCalculator() {
		super();
	}

	@Override
	public List<OrderPrice> calculatePrice(ProductOrderItem productOrderItem) throws BillingEngineValidationException, ApiException {
		logger.info("Calculating price preview for ProductOfferingPrice '{}' of ProductOrderItem", pop.getId(), productOrderItem.getId());
		
		List<OrderPrice> orderPrices=new ArrayList<OrderPrice>();
		
		// Checks is unitOfMeasure is null or "1 unit"
		tmfEntityValidator.validateUnitOfMeasureForSinglePrice(pop);
		// Checks if Price is well formed 
		tmfEntityValidator.validatePrice(pop);
		
		Float orderPriceValue=(pop.getPrice().getValue() * productOrderItem.getQuantity());
		
		Money money=new Money(priceCurrency,orderPriceValue);
		Price price= TMForumEntityUtils.createPriceTMF622(money);
		OrderPrice orderPrice=TMForumEntityUtils.createOrderPriceTMF622(price, pop);
			
		logger.info("Price of ProductOfferingPrice '{}' in ProductOrdeIem '{}': [quantity: {}, price: '{}'] = {} {}", 
				pop.getId(), productOrderItem.getId(), productOrderItem.getQuantity(), pop.getPrice().getValue(), money.getValue(), priceCurrency);
								
		// apply price alterations
		if (ProductOfferingPriceUtils.hasRelationships(pop)) {
			OrderPrice updatedOrderPrice=priceAlterationCalculator.applyAlterations(orderPrice,ProductOfferingPriceUtils.getProductOfferingPriceRelationships(pop.getPopRelationship(), productCatalogManagementApis),productOrderItem.getQuantity());
								
			logger.info("Price of ProductOfferingPrice '{}' after alterations = {} {}", 
					pop.getId(), OrderPriceUtils.getAlteredDutyFreePrice(updatedOrderPrice),priceCurrency);	
		
			orderPrices.add(updatedOrderPrice);
		}else {
			orderPrices.add(orderPrice);
		}
			
		return orderPrices;
		
	}

}

