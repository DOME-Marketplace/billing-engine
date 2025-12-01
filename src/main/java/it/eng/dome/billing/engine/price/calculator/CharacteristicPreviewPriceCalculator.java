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
import it.eng.dome.billing.engine.utils.TmfConverter;
import it.eng.dome.billing.engine.validator.ValidationIssue;
import it.eng.dome.billing.engine.validator.ValidationIssueSeverity;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;

@Component
public class CharacteristicPreviewPriceCalculator extends AbstractPriceCalculator<ProductOrderItem, List<OrderPrice>>{

	private final Logger logger = LoggerFactory.getLogger(CharacteristicPreviewPriceCalculator.class);
	
	public CharacteristicPreviewPriceCalculator() {
		super();
	}

	@Override
	public List<OrderPrice> calculatePrice(ProductOrderItem prodOrderItem) throws BillingEngineValidationException, ApiException {
		logger.info("Calculating price preview for POP  '{}' with Characteristic of Product '{}'", pop.getId(), prodOrderItem.getId());
		
		List<OrderPrice> orderPrices=new ArrayList<OrderPrice>();
		it.eng.dome.billing.engine.model.Characteristic matchChar=null;
		
		tmfEntityValidator.validateCharacteristicsInProductOrderItem(prodOrderItem);
		
		List<it.eng.dome.billing.engine.model.Characteristic> characteristics=TmfConverter.convert622ToCharacteristics(prodOrderItem.getProduct().getProductCharacteristic());
		matchChar=this.findMachingCharacteristic(characteristics);
			
		if(matchChar==null) {
			String msg=String.format("Error! No matching Characteristic found for the ProductOfferingPrice '%s' in ProductOrderItem '%s'", pop.getId(), prodOrderItem.getId());
			ValidationIssue issue=new ValidationIssue(msg,ValidationIssueSeverity.ERROR);
			throw new BillingEngineValidationException(issue);
		}
			
		Money chPrice=calculatePriceForCharacteristic(matchChar);
		
		chPrice=OrderPriceUtils.applyQuantity(chPrice, prodOrderItem.getQuantity());
		
		Price price= TMForumEntityUtils.createPriceTMF622(chPrice);
		OrderPrice orderPrice=TMForumEntityUtils.createOrderPriceTMF622(price, pop);
			
		// applies price alterations
		if (ProductOfferingPriceUtils.hasRelationships(pop)) {
			OrderPrice updatedOrderPrice=priceAlterationCalculator.applyAlterations(orderPrice,ProductOfferingPriceUtils.getProductOfferingPriceRelationships(pop.getPopRelationship(),productCatalogManagementApis), prodOrderItem.getQuantity());
			
			logger.info("Price of Characteristic '{}' '{}' after alterations: {} {}", 
			matchChar.getName(), matchChar.getValue(), OrderPriceUtils.getAlteredDutyFreePrice(updatedOrderPrice));
			
			orderPrices.add(updatedOrderPrice);
		}else {
			orderPrices.add(orderPrice);
		}
			
		return orderPrices;
	}

}
