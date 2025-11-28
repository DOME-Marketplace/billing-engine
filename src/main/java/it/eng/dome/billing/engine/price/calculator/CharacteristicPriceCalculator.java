package it.eng.dome.billing.engine.price.calculator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.utils.TmfConverter;
import it.eng.dome.billing.engine.validator.ValidationIssue;
import it.eng.dome.billing.engine.validator.ValidationIssueSeverity;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf637.v4.model.Product;

@Component
public class CharacteristicPriceCalculator extends AbstractPriceCalculator<Product,Money>{
	
	private final Logger logger = LoggerFactory.getLogger(CharacteristicPriceCalculator.class);
	
	public CharacteristicPriceCalculator() {
		super();
	}


	@Override
	public Money calculatePrice(Product prod) throws BillingEngineValidationException, ApiException {
		logger.info("Calculating price for POP '{}' with Characteristic of Product '{}'", pop.getId(), prod.getId());
		
		it.eng.dome.billing.engine.model.Characteristic matchChar=null;
		
		tmfEntityValidator.validateCharacteristicsInProduct(prod);
		
		List<it.eng.dome.billing.engine.model.Characteristic> characteristics=TmfConverter.convert637ToCharacteristics(prod.getProductCharacteristic());
		matchChar=this.findMachingCharacteristic(characteristics);
			
		if(matchChar==null) {
			String msg=String.format("Error! No matching Characteristic found for the ProductOfferingPrice '%s' in Product '%s'", pop.getId(), prod.getId());
			ValidationIssue issue=new ValidationIssue(msg,ValidationIssueSeverity.ERROR);
			throw new BillingEngineValidationException(issue);
		}
			
		Money chPrice=calculatePriceForCharacteristic(matchChar);
			
		 // applies price alterations
		if (ProductOfferingPriceUtils.hasRelationships(pop)) {
			Money alteratedChPrice=priceAlterationCalculator.applyAlterations(chPrice,ProductOfferingPriceUtils.getProductOfferingPriceRelationships(pop.getPopRelationship(),productCatalogManagementApis));
			
			logger.info("Price of Characteristic '{}' '{}' after alterations: {} {}", 
			matchChar.getName(), matchChar.getValue(), alteratedChPrice.getValue());
			
			return alteratedChPrice;
		}
		
		return chPrice;
	}

}
