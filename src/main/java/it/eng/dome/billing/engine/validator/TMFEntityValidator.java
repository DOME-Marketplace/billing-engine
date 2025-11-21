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
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.ProductPrice;
import jakarta.validation.constraints.NotNull;

/**
 * Class to validate the TMForum entities. If there are some unexpected/missing values needed for the BillingEngine processing a {@link BillingEngineValidationException} is raised.
 */
@Component
public class TMFEntityValidator {
	
	private final static Logger logger=LoggerFactory.getLogger(TMFEntityValidator.class);
	
	/**
	 * Validates the {@link ProductOfferingPrice}
	 * 
	 * @param pop {@link ProductOfferingPrice} to validate
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
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
		
		if(ProductOfferingPriceUtils.isPriceTypeInRecurringCategory(pop)) {
			if(pop.getRecurringChargePeriodLength()==null){
				String msg=String.format("The ProductOfferingPrice '%s' (recurring) must have 'recurringChargePeriodLength'", pop.getId());
				issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
			}
			if(pop.getRecurringChargePeriodType()==null || pop.getRecurringChargePeriodType().isEmpty()){
				String msg=String.format("The ProductOfferingPrice '%s' (recurring) must have 'recurringChargePeriodType'", pop.getId());
				issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
			}
		}
		
		if((pop.getIsBundle()!=null && !pop.getIsBundle()) & pop.getPrice()==null){
			String msg=String.format("The ProductOfferingPrice '%s' (not bundled) must have 'price'", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(ProductOfferingPriceUtils.isPriceTypeUsage(pop) && pop.getUnitOfMeasure()==null){
			String msg=String.format("The ProductOfferingPrice '%s' (usage) must have 'unitOfMeasure'", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		this.throwsErrorValidationIssuesIfAny(issues);
		
		logger.debug("Validation of ProductOfferingPrice {} successful", pop.getId());
		
	}
	
	/**
	 * Validates the {@link Product}
	 * 
	 * @param prod the {@link Product} to validate
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
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
		
		
		this.throwsErrorValidationIssuesIfAny(issues);
		
		logger.debug("Validation of Product {} successful", prod.getId());
		
	}	
	
	/**
	 * Validate a {@link ProductPrice}
	 * 
	 * @param prodPrice the {@link ProductPrice} to validate
	 * @param prodId the identifier of the {@link Product} to which the ProductPrice belongs to
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
	public void validateProductPrice(@NotNull ProductPrice prodPrice, @NotNull String prodId) throws BillingEngineValidationException{
		
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(prodPrice.getProductOfferingPrice()==null) {
			String msg=String.format("The ProductPrice of Product %s must have 'ProductOfferingPrice'", prodId);
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(prodPrice.getProductOfferingPrice().getId()==null || prodPrice.getProductOfferingPrice().getId().isEmpty()) {
			String msg=String.format("The ProductPrice f Product %s must have a 'ProductOfferingPrice' with a valorised 'id'", prodId);
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		this.throwsErrorValidationIssuesIfAny(issues);
		
		logger.debug("Validation of ProductPrice successful");
		
	}
	
	/**
	 * Validates a {@link Usage}
	 * @param usage the {@link Usage} to validate
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
	public void validateUsage(@NotNull Usage usage) throws BillingEngineValidationException{
		
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(usage.getUsageCharacteristic()==null || usage.getUsageCharacteristic().isEmpty()) {
			String msg=String.format("The UsageCharacteristic should not be null or empty for Usage: {}", usage.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.WARNING));
		}
		
		this.logWarningValidationIssuesIfAny(issues);
		
		logger.debug("Validation of Usage {} successful", usage.getId());
	}
	
	/**
	 * Validates a list of {@link Usage}
	 * @param usages the list of {@link Usage} to validate
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
	public void validateUsages(@NotNull List<Usage> usages) throws BillingEngineValidationException{
		
		for(Usage usage:usages) {
			this.validateUsage(usage);
		}
		
	} 
	
	/**
	 * Validate the price of a {@link ProductOfferingPrice}
	 * @param pop the {@link ProductOfferingPrice} which price must be validated
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
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
		
		this.throwsErrorValidationIssuesIfAny(issues);
		
		this.logWarningValidationIssuesIfAny(issues);
		
		logger.debug("Validation of Price for ProductOfferingPrice {} successful", pop.getId());
	}
	
	/**
	 * Validates a {@link Quantity} representing the unitOfMeasure of the specified {@link ProductOfferingPrice}
	 * @param unitOfMeasure the {@link Quantity} to validate
	 * @param pop the {@link ProductOfferingPrice} to which the unitOfMeasure belongs to
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
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
			
		this.throwsErrorValidationIssuesIfAny(issues);
		
		logger.debug("Validation of unitOdMeasure of ProductOfferingPrice {} successful", pop.getId());
	}
	
	/**
	 * In case of a simple {@link ProductOfferingPrice}, i.e., not bundled and without characteristics the unitOfMeasure  must be null or "1 unit"
	 * 
	 * @param pop the simple {@link ProductOfferingPrice} for which the unitOfMeasure must be checked
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
	public void validateUnitOfMeasureForSinglePrice(@NotNull ProductOfferingPrice pop) throws BillingEngineValidationException {
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(pop.getUnitOfMeasure()!=null && !"unit".equalsIgnoreCase(pop.getUnitOfMeasure().getUnits()) && pop.getUnitOfMeasure().getAmount()!=1){
			String msg=String.format("The UnitOfMeasure element of ProductOfferingPrice '%s' with single price  must be null or 1 unit!", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		logger.debug("Validation of unitOfMeasure of single ProductOfferingPrice {} successful", pop.getId());
		
		this.throwsErrorValidationIssuesIfAny(issues);
	}
	
	/**
	 * Validates the list of {@link ProductSpecificationCharacteristicValueUse} of the specified {@link ProductOfferingPrice}
	 * @param pop the {@link ProductOfferingPrice} for which the {@link ProductSpecificationCharacteristicValueUse} must be validated
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
	public void validateProdSpecCharValueUseList(@NotNull ProductOfferingPrice pop) throws BillingEngineValidationException{
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(pop.getProdSpecCharValueUse()!=null && pop.getProdSpecCharValueUse().size()>1) {
			String msg=String.format("The size of prodSpecCharValueUse in ProductOfferingPrice {} is greater than one ", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.WARNING));
		}
		
		this.logWarningValidationIssuesIfAny(issues);
	}
	
	/**
	 * Validates the {@link ProductSpecificationCharacteristicValueUse} of the specified  {@link ProductOfferingPrice}
	 * @param charValueUse the {@link ProductSpecificationCharacteristicValueUse} to validate
	 * @param pop the {@link ProductOfferingPrice} to which the {@link ProductSpecificationCharacteristicValueUse} belongs to
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
	public void validateProductSpecificationCharacteristicValueUse(@NotNull ProductSpecificationCharacteristicValueUse charValueUse, @NotNull ProductOfferingPrice pop) throws BillingEngineValidationException {
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(charValueUse.getName()==null || charValueUse.getName().isEmpty()) {
			String msg=String.format("The name of the Characteristic is missing in ProductOfferingPrice {}", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
	
		this.throwsErrorValidationIssuesIfAny(issues);
		
		logger.debug("Validation of productSpecificationCharacteristicValueUse of ProductOfferingPrice {} successful", pop.getId());
	}
	
	/**
	 * Checks if all the currencies in the specified list of {@link ProductOfferingPrice} are the same or not
	 * @param pops the list of {@link ProductOfferingPrice} that must be checked
	 * @param prod the Product to which the {@link ProductOfferingPrice} in the list refer to
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
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
	    
	    this.throwsErrorValidationIssuesIfAny(issues);
	    
	    logger.debug("Validation of ProductOfferingPrice's currencies of Product {} successful", prod.getId());
		
	}
	

	/**
	 * Checks if the specified {@link ProductOfferingPrice} that belongs to a popRelationship contains the minimum required attributes. 
	 * 
	 * @param pop The {@link ProductOfferingPrice} to validate
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
	public void validatePopRelationship(@NotNull ProductOfferingPrice pop) throws BillingEngineValidationException{
		
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(pop.getLifecycleStatus()==null || pop.getLifecycleStatus().isEmpty()) {
			String msg=String.format("The ProductOfferingPrice '%s' must have 'lifecycleStatus'", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(pop.getValidFor()==null){
			String msg=String.format("The ProductOfferingPrice '%s' must have 'validFor'", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(pop.getPriceType()==null || pop.getPriceType().isEmpty()) {
			String msg=String.format("The ProductOfferingPrice '%s' must have 'priceType'", pop.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		this.throwsErrorValidationIssuesIfAny(issues);
		
		logger.debug("Validation of ProductOfferingPrice in popRelationship {} successful", pop.getId());
		
	}
	
	/**
	 * Validates the {@link ProductOrder}
	 * 
	 * @param productOrder {@link ProductOrder} to validate
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
	public void validateProductOrder(@NotNull ProductOrder productOrder) throws BillingEngineValidationException{
		
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(productOrder.getProductOrderItem()==null||productOrder.getProductOrderItem().isEmpty()){
			String msg=String.format("The ProductOrder '%s' must have 'productOrderItem'", productOrder.getId());
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		this.throwsErrorValidationIssuesIfAny(issues);
		
		logger.debug("Validation of ProductOrder {} successful", productOrder.getId());
		
	}
	
	/**
	 * Validates the {@link ProductOrderItem}
	 * 
	 * @param productOrderItem {@link ProductOrderItem} to validate
	 * @param productOrderId The identifier of the {@link ProductOrder} to which the ProductOrderItem belongs to
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
	public void validateProductOrderItem(@NotNull ProductOrderItem productOrderItem, @NotNull String productOrderId) throws BillingEngineValidationException{
		
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(productOrderItem.getItemTotalPrice()==null||productOrderItem.getItemTotalPrice().isEmpty()){
			String msg=String.format("The ProductOrderItem %s of ProductOrder '%s' must have 'itemTotalPrice'", productOrderItem.getId(),productOrderId);
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(productOrderItem.getQuantity()==null||productOrderItem.getQuantity()<=0f){
			String msg=String.format("The ProductOrderItem %s of ProductOrder '%s' must have 'quantity' greater than zero", productOrderItem.getId(),productOrderId);
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		this.throwsErrorValidationIssuesIfAny(issues);
		
		logger.debug("Validation of ProductOrderItem {} for ProductOrder {} successful",productOrderItem.getId(),productOrderId);
		
	}
	
	/**
	 * Validates the {@link OrderPrice}
	 * 
	 * @param op The {@link OrderPrice} to validate
	 * @param productOrderItemId The identifier of the {@link ProductOrderItem} to which the {@link OrderPrice} belongs to
	 * @param productOrderId The identifier of the {@link ProductOrder} to which the {@link OrderPrice} belongs to
	 * @throws BillingEngineValidationException if some unexpected/missing values are find
	 */
	public void validateOrderPrice(@NotNull OrderPrice op, @NotNull String productOrderItemId, @NotNull String productOrderId) throws BillingEngineValidationException{
		
		List<ValidationIssue> issues=new ArrayList<ValidationIssue>();
		
		if(op.getProductOfferingPrice()==null){
			String msg=String.format("The OrderPrice of ProductOrderItem '%s' in ProducOrder '%s' must have 'productOfferingPrice'", productOrderItemId, productOrderId);
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		if(op.getProductOfferingPrice().getId()==null || op.getProductOfferingPrice().getId().isEmpty()) {
			String msg=String.format("The OrderPrice of ProductOrderItem '%s' in ProducOrder '%s' must have a 'ProductOfferingPrice' with a valorised 'id'", productOrderItemId, productOrderId);
			issues.add(new ValidationIssue(msg,ValidationIssueSeverity.ERROR));
		}
		
		this.throwsErrorValidationIssuesIfAny(issues);
		
		logger.debug("Validation of OrdePrice of ProductOrderItem {} in ProductOrder {} successful",productOrderItemId, productOrderId);
		
	}
	
	private void throwsErrorValidationIssuesIfAny(List<ValidationIssue> issues) throws BillingEngineValidationException {
		if (issues.stream().anyMatch(i -> i.getSeverity() == ValidationIssueSeverity.ERROR)) {
	           throw new BillingEngineValidationException(issues);
	        }
	}
	
	private void logWarningValidationIssuesIfAny(List<ValidationIssue> issues) {
		if (issues.stream().anyMatch(i -> i.getSeverity() == ValidationIssueSeverity.WARNING)) {
			BillingEngineValidationException ex=new BillingEngineValidationException(issues);
            logger.warn(ex.getMessage());
        }
	}
	

}
