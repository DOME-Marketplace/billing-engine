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
import it.eng.dome.tmforum.tmf635.v4.model.Usage;

@Component
public class UsagePreviewPriceCalculator extends AbstractPriceCalculator<ProductOrderItem, List<OrderPrice>>{
	
	private final Logger logger = LoggerFactory.getLogger(UsagePreviewPriceCalculator.class);
	
	private List<Usage> usages;

	@Override
	public List<OrderPrice> calculatePrice(ProductOrderItem productOrderItem) throws BillingEngineValidationException, ApiException {

		logger.info("Calculating price preview for POP '{}' USAGE of ProductOrderItem '{}'", pop.getId(), productOrderItem.getId());
		
		List<OrderPrice> orderPrices=new ArrayList<OrderPrice>();
		usageData=inizializeUsageData(usages);
		
		Money totalAmountMoney=this.calculatePriceforUsageCharacteristics();
		Money finalAmountMoney=OrderPriceUtils.applyQuantity(totalAmountMoney, productOrderItem.getQuantity());
		
		logger.info("Price of ProductOfferingPrice '{}' of ProductOrderItem '{}': [quantity: {}, price: '{}'] = {}{}", pop.getId(), 
				productOrderItem.getId(),productOrderItem.getQuantity(),totalAmountMoney.getValue(),finalAmountMoney.getValue(), priceCurrency);

		
		Price price= TMForumEntityUtils.createPriceTMF622(finalAmountMoney);
		OrderPrice orderPrice=TMForumEntityUtils.createOrderPriceTMF622(price, pop);
		
		// apply price alterations
		if (ProductOfferingPriceUtils.hasRelationships(pop)) {
			OrderPrice alteretedOrderPrice=priceAlterationCalculator.applyAlterations(orderPrice, ProductOfferingPriceUtils.getProductOfferingPriceRelationships(pop.getPopRelationship(), productCatalogManagementApis),productOrderItem.getQuantity());
											
			logger.info("Price of ProductOfferingPrice '{}' after alterations = {} {}", 
					pop.getId(), OrderPriceUtils.getAlteredDutyFreePrice(orderPrice),priceCurrency);	
		
			orderPrices.add(alteretedOrderPrice);
		}else{
			orderPrices.add(orderPrice);
		}
	
		return orderPrices;
		
	}
	
	public void setUsages(List<Usage> usages) {
		if (usages == null) {
		    this.usages = new ArrayList<>();
		} else {
		    this.usages=usages;
		}
	}

}
