package it.eng.dome.billing.engine.price.calculator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.validator.ValidationIssue;
import it.eng.dome.billing.engine.validator.ValidationIssueSeverity;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductSpecificationCharacteristicValueUse;
import it.eng.dome.tmforum.tmf620.v4.model.Quantity;
import it.eng.dome.tmforum.tmf637.v4.model.Characteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import jakarta.validation.constraints.NotNull;

public class CharacteristicPriceCalculator extends AbstractPriceCalculator{
	
	private final Logger logger = LoggerFactory.getLogger(CharacteristicPriceCalculator.class);

	public CharacteristicPriceCalculator(ProductOfferingPrice pop, Product prod) {
		super(pop,prod);
	}


	@Override
	public Money calculatePrice() throws BillingEngineValidationException, ApiException {
		logger.info("Calculating charcteristic price for POP '{}'...", pop.getId());
		
		tmfEntityValidator.validateProdSpecCharValueUseList(pop);
		ProductSpecificationCharacteristicValueUse prodSpecCharValueUse= pop.getProdSpecCharValueUse().get(0);
		
		tmfEntityValidator.validateProductSpecificationCharacteristicValueUse(prodSpecCharValueUse, pop);
		
		Characteristic matchChar=null;
		
		for(Characteristic productCharacteristic : prod.getProductCharacteristic()) {
			if(prodSpecCharValueUse.getName().equalsIgnoreCase(productCharacteristic.getName())) {
				matchChar=productCharacteristic;
				break;
			}
		}
		if(matchChar==null) {
			String msg=String.format("Error! No matching Characteristic found for the ProductOfferingPrice '%s'", pop.getId());
			ValidationIssue issue=new ValidationIssue(msg,ValidationIssueSeverity.ERROR);
			throw new BillingEngineValidationException(issue);
		}
		
		// calculates the base price of the Characteristic 
		Money chAmount = calculatePriceForCharacteristic(pop, matchChar);
			
	    // applies price alterations
		if (ProductOfferingPriceUtils.hasRelationships(pop)) {
			Money alteratedPrice=priceAlterationCalculator.applyAlterations(chAmount,ProductOfferingPriceUtils.getProductOfferingPriceRelationships(pop.getPopRelationship(),productCatalogManagementApis));
			
			logger.info("Price of Characteristic '{}' '{}' after alterations: {} {}", 
			matchChar.getName(), matchChar.getValue(), alteratedPrice.getValue());
			
			return alteratedPrice;
		}
		
		return chAmount;
	}
	

	private Money calculatePriceForCharacteristic(@NotNull ProductOfferingPrice pop, @NotNull Characteristic ch) {
	
		logger.debug("Calculating price for Characteristic: '{}' value '{}'", ch.getName(), ch.getValue());
		final String chName = ch.getName();
		final Float chValue = Float.parseFloat(ch.getValue().toString());
		Float chAmount;
	
		if (ProductOfferingPriceUtils.isForfaitPrice(pop)) {
			chAmount = (pop.getPrice().getValue() * chValue);
			logger.info("Price of Characteristic '{}' [quantity: {}, price: '{}'] = {} {}", 
					chName, chValue, pop.getPrice().getValue(), chAmount,priceCurrency);
		} else {
			final Quantity unitOfMeasure = pop.getUnitOfMeasure();
			//chAmount = new EuroMoney(((pop.getPrice().getValue() * chValue) / unitOfMeasure.getAmount()) * chValue);
			chAmount = (pop.getPrice().getValue() * chValue) / unitOfMeasure.getAmount();
			logger.info("Price of Characteristic '{}' [quantity: {}, price: '{}' per '{} {}'] = {} {}", 
					chName, chValue,
					pop.getPrice().getValue(), unitOfMeasure.getAmount(), unitOfMeasure.getUnits(), chAmount,priceCurrency);
		}
		
		return new Money(priceCurrency, chAmount);
	}

}
