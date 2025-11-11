package it.eng.dome.billing.engine.validator;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductSpecificationCharacteristicValueUse;
import it.eng.dome.tmforum.tmf620.v4.model.Quantity;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.ProductPrice;
import jakarta.validation.constraints.NotNull;

@Component
public class TMFEntityValidator {
	
	private final static Logger logger=LoggerFactory.getLogger(TMFEntityValidator.class);
	
	public void validateProductOfferingPrice(@NotNull ProductOfferingPrice pop) throws BillingEngineValidationException{
		
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(pop.getLifecycleStatus()==null || pop.getLifecycleStatus().isEmpty()) {
			String msg=String.format("The ProductOfferingPrice '%s' must have 'lifecycleStatus'", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(pop.getIsBundle()==null){
			String msg=String.format("The ProductOfferingPrice '%s' must have 'isBundle'", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if((pop.getIsBundle()!=null && pop.getIsBundle()) && (pop.getBundledPopRelationship()==null || pop.getBundledPopRelationship().isEmpty())){
			String msg=String.format("The ProductOfferingPrice '%s' is bundled but the BundledPopRelationship are missing", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if((pop.getIsBundle()!=null && !pop.getIsBundle()) && (pop.getPriceType()==null || pop.getPriceType().isEmpty())) {
			String msg=String.format("The ProductOfferingPrice '%s' (not bundled) must have 'priceType'", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(ProductOfferingPriceUtils.isPriceTypeRecurring(pop)) {
			if(pop.getRecurringChargePeriodLength()==null){
				String msg=String.format("The ProductOfferingPrice '%s' (recurring) must have 'recurringChargePeriodLength'", pop.getId());
				issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
			}
			if(pop.getRecurringChargePeriodType()==null || pop.getRecurringChargePeriodType().isEmpty()){
				String msg=String.format("The ProductOfferingPrice '%s' (recurring) must have 'recurringChargePeriodType'", pop.getId());
				issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
			}
		}
		
		if(pop.getPrice()==null){
			String msg=String.format("The ProductOfferingPrice '%s' must have 'price'", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(ProductOfferingPriceUtils.isPriceTypeUsage(pop) && pop.getUnitOfMeasure()==null){
			String msg=String.format("The ProductOfferingPrice '%s' (usage) must have 'unitOfMeasure'", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if (issues.stream().anyMatch(i -> i.getSeverity() == ValidationIssueSeverity.ERROR)) {
           throw new BillingEngineValidationException(issues);
        }
		
		logger.debug("Validation of ProductOfferingPrice {} successful", pop.getId());
		
	}
	
	public void validateProduct(@NotNull Product prod) throws BillingEngineValidationException{
		
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(prod.getProductPrice()==null || prod.getProductPrice().isEmpty()) {
			String msg=String.format("The Product '%s' must have 'ProductPrice'", prod.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(prod.getStartDate()==null){
			String msg=String.format("The Product '%s' must have 'startDate'", prod.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(prod.getStatus()==null){
			String msg=String.format("The Product '%s' must have 'status'", prod.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(prod.getBillingAccount()==null) {
			String msg=String.format("The Product '%s' must have 'billingAccount'", prod.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(prod.getRelatedParty()==null || prod.getRelatedParty().isEmpty()) {
			String msg=String.format("The Product '%s' must have 'relatedParty'", prod.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		
		if (issues.stream().anyMatch(i -> i.getSeverity() == ValidationIssueSeverity.ERROR)) {
            throw new BillingEngineValidationException(issues);
        }
		
		logger.debug("Validation of Product {} successful", prod.getId());
		
	}	
	
	public void validateProductPrice(@NotNull ProductPrice prodPrice) throws BillingEngineValidationException{
		
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(prodPrice.getProductOfferingPrice()==null) {
			String msg=String.format("The ProductPrice must have 'ProductOfferingPrice'");
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if (issues.stream().anyMatch(i -> i.getSeverity() == ValidationIssueSeverity.ERROR)) {
            throw new BillingEngineValidationException(issues);
        }
		
		logger.debug("Validation of ProductPrice successful");
		
	}
	
	public void validateUsage(@NotNull Usage usage){
		
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(usage.getUsageCharacteristic()==null || usage.getUsageCharacteristic().isEmpty()) {
			String msg=String.format("The UsageCharacteristic should not be null or empty for Usage: {}", usage.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.WARNING));
		}
		
		if (issues.stream().anyMatch(i -> i.getSeverity() == ValidationIssueSeverity.WARNING)) {
			BillingEngineValidationException ex=new BillingEngineValidationException(issues);
            logger.warn(ex.getMessage());
        }else {
        	logger.debug("Validation of Usage successful");
        }
		
	}
	
	public void validateUsages(@NotNull List<Usage> usages){
		
		for(Usage usage:usages) {
			this.validateUsage(usage);
		}
		
	} 
	
	public void validatePrice(@NotNull ProductOfferingPrice pop) throws BillingEngineValidationException {
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(pop.getPrice().getUnit()==null || (pop.getPrice().getUnit().isEmpty())) {
			String msg=String.format("The currency is missing in the price of ProductOfferingPrice {}. By default 'EUR' is used", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.WARNING));
		}
		
		if(pop.getPrice().getValue()==null) {
			String msg=String.format("The value is missing in the price of ProductOfferingPrice {}", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if (issues.stream().anyMatch(i -> i.getSeverity() == ValidationIssueSeverity.ERROR)) {
            throw new BillingEngineValidationException(issues);
        }
		
		if (issues.stream().anyMatch(i -> i.getSeverity() == ValidationIssueSeverity.WARNING)) {
			BillingEngineValidationException ex=new BillingEngineValidationException(issues);
            logger.warn(ex.getMessage());
        }
		else {
        	logger.debug("Validation of Price for ProductOfferingPrice {} successful", pop.getId());
        }
	}
	
	public void validateUnitOfMeasure (@NotNull Quantity unitOfMeasure, @NotNull ProductOfferingPrice pop) throws BillingEngineValidationException {
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(unitOfMeasure.getUnits()==null || unitOfMeasure.getUnits().isEmpty()){
			String msg=String.format("The units is missing in unitOfMeasure of ProductOfferingPrice {}", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(unitOfMeasure.getAmount()==null){
			String msg=String.format("The amount is missing in unitOfMeasure of ProductOfferingPrice {}", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
			
		if (issues.stream().anyMatch(i -> i.getSeverity() == ValidationIssueSeverity.ERROR)) {
            throw new BillingEngineValidationException(issues);
        }
	}
	
	public void validateProdSpecCharValueUseList(@NotNull ProductOfferingPrice pop) {
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(pop.getProdSpecCharValueUse()!=null && pop.getProdSpecCharValueUse().size()>1) {
			String msg=String.format("The size of prodSpecCharValueUse in ProductOfferingPrice {} is greater than one ", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.WARNING));
		}
		
		if (issues.stream().anyMatch(i -> i.getSeverity() == ValidationIssueSeverity.WARNING)) {
			BillingEngineValidationException ex=new BillingEngineValidationException(issues);
            logger.warn(ex.getMessage());
        }
	}
	
	public void validateProductSpecificationCharacteristicValueUse(@NotNull ProductSpecificationCharacteristicValueUse charValueUse, @NotNull ProductOfferingPrice pop) throws BillingEngineValidationException {
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(charValueUse.getName()==null || charValueUse.getName().isEmpty()) {
			String msg=String.format("The name of the Characteristic is missing in ProductOfferingPrice {}", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if (issues.stream().anyMatch(i -> i.getSeverity() == ValidationIssueSeverity.ERROR)) {
            throw new BillingEngineValidationException(issues);
        }
	}
	
	public void validatePOPsCurrency(@NotNull List<ProductOfferingPrice> pops, @NotNull Product prod) throws BillingEngineValidationException {
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		String firstCurrency = pops.get(0).getPrice().getUnit();
		
		boolean allSame = pops.stream()
	            .map(pop -> pop.getPrice().getUnit())
	            .allMatch(currency -> currency.equals(firstCurrency));

	    if (!allSame) {
	    	String msg=String.format("The price components of the Product {} have different currencies", prod.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
	    }
	    
	    if (issues.stream().anyMatch(i -> i.getSeverity() == ValidationIssueSeverity.ERROR)) {
            throw new BillingEngineValidationException(issues);
        }
		
	}
	

}
