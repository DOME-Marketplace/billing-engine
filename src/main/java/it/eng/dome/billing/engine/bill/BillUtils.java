package it.eng.dome.billing.engine.bill;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.api.UsageManagementApis;
import it.eng.dome.brokerage.api.fetch.FetchUtils;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.ProductOfferingPriceRef;
import it.eng.dome.tmforum.tmf637.v4.model.ProductPrice;
import it.eng.dome.tmforum.tmf637.v4.model.RelatedParty;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.Money;
import it.eng.dome.tmforum.tmf678.v4.model.ProductRef;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import lombok.NonNull;


public final class BillUtils {

	private static Logger logger = LoggerFactory.getLogger(BillUtils.class);
	
	public static List<ProductPrice> getProductPrices(@NonNull Product product) {
		return product.getProductPrice();
	}
	
	public static String getPriceTypeFromProductPrice(@NonNull ProductPrice productPrice, @NonNull ProductCatalogManagementApis productCatalogManagementApis) throws Exception{
		ProductOfferingPriceRef productOfferingPriceRef=productPrice.getProductOfferingPrice();
		//try {
			ProductOfferingPrice pop = productCatalogManagementApis.getProductOfferingPrice(productOfferingPriceRef.getId(), null);
			if (pop != null) {
				return pop.getPriceType();
			}else {
				throw (IllegalStateException)new IllegalStateException(String.format("ProductOfferingPrice with id %s not found on server!", productOfferingPriceRef.getId()));
			}			

		/*} catch (ApiException exc) {
			if (exc.getCode() == HttpStatus.NOT_FOUND.value()) {
				throw (IllegalStateException)new IllegalStateException(String.format("ProductOfferingPrice with id %s not found on server!", productOfferingPriceRef.getId())).initCause(exc);
			}
			throw exc;
		}*/
		
		
	}
	
	public static it.eng.dome.tmforum.tmf678.v4.model.BillingAccountRef createAppliedCustomerRateBillingAccount(@NonNull it.eng.dome.tmforum.tmf637.v4.model.BillingAccountRef productBillingAccountRef) {
		
		 it.eng.dome.tmforum.tmf678.v4.model.BillingAccountRef billingAccountRef = new it.eng.dome.tmforum.tmf678.v4.model.BillingAccountRef();
		 
		 billingAccountRef.setId(productBillingAccountRef.getId());
		 billingAccountRef.setName(productBillingAccountRef.getName());
		 billingAccountRef.setHref(productBillingAccountRef.getHref());
		 billingAccountRef.setAtType(productBillingAccountRef.getAtType());
		 billingAccountRef.setAtSchemaLocation(productBillingAccountRef.getAtSchemaLocation());
		 billingAccountRef.setAtReferredType(productBillingAccountRef.getAtReferredType());
		 billingAccountRef.setAtBaseType(productBillingAccountRef.getAtBaseType());
		 
		 return billingAccountRef;
	}
	
	public static AppliedCustomerBillingRate createAppliedCustomerBillingRate(@NonNull Product product, @NonNull TimePeriod tp, @NonNull Money taxExcludedAmount, @NonNull String appliedBillingRateType, @NonNull String schemaLocation) throws Exception{
		
		AppliedCustomerBillingRate appliedCustomerBillingRate = new AppliedCustomerBillingRate();

		// Set appliedCustomerBillingRate.billingAccount
		if(product.getBillingAccount() == null) {
			throw new BillingBadRequestException("Billing Account is missing in the product with ID: " + product.getId());
		}
		
		appliedCustomerBillingRate.setBillingAccount(BillUtils.createAppliedCustomerRateBillingAccount(product.getBillingAccount()));

		// Set appliedCustomerBillingRate.date
		appliedCustomerBillingRate.setDate(OffsetDateTime.now());

		// Set appliedCustomerBillingRate.description
		appliedCustomerBillingRate.setDescription("Generated bill for product " + product.getId() + " with TimePeriod " + tp.getStartDateTime() + "/" + tp.getEndDateTime());

		// Set appliedCustomerBillingRate.isBilled to false because the billingAccount is valorised
		appliedCustomerBillingRate.setIsBilled(false);

		// Set appliedCustomerBillingRate.name
		appliedCustomerBillingRate.setName("Bill for product with ID: " + product.getId());

		// Set appliedCustomerBillingRate.periodCoverage
		appliedCustomerBillingRate.setPeriodCoverage(tp);

		// Set appliedCustomerBillingRate.product reference
		ProductRef prodRef = new ProductRef();
		prodRef.setId(product.getId());
		appliedCustomerBillingRate.setProduct(prodRef);
		
		// Set appliedCustomerBillinhRate.appliedBillingRateType
		appliedCustomerBillingRate.setType(appliedBillingRateType);
		
		// Set appliedCustomerBillingRate.taxExcludedAmount
		appliedCustomerBillingRate.setTaxExcludedAmount(taxExcludedAmount);
		
		// Set appliedCustomerBillingRate.schemaLocation
		appliedCustomerBillingRate.setAtSchemaLocation(URI.create(schemaLocation));
		
		// Set appliedCustomerBillingRate.relatedParty (if present in the Product)
		List<RelatedParty> prodRelatedParty = product.getRelatedParty();
		
		if (prodRelatedParty != null) {
			logger.debug("List of relatedParty from Product - size {} ", prodRelatedParty.size());
			
			for(RelatedParty rp: prodRelatedParty) {
				it.eng.dome.tmforum.tmf678.v4.model.RelatedParty rpTMF678=createRelatedParty_TMF678(rp);
				appliedCustomerBillingRate.addRelatedPartyItem(rpTMF678);
			}
		}
		
		return appliedCustomerBillingRate;
	}
	
	/*
     * Returns the list of ProductOfferingPrice referenced in the bundle ProductOfferingPrice (i.e., stored in the bundledPopRelationship element of the ProductOfferingPrice)
     */
	public static List<ProductOfferingPrice> getBundledPops(ProductOfferingPrice pop, ProductCatalogManagementApis productCatalogManagementApis) throws Exception {
		final List<ProductOfferingPrice> bundledPops = new ArrayList<ProductOfferingPrice>();
		
		for (var bundledPopRel : pop.getBundledPopRelationship()) {
			logger.debug("Retrieving remote ProductOfferingPrice with id: '{}'", bundledPopRel.getId());
			ProductOfferingPrice productOfferingPrice = productCatalogManagementApis.getProductOfferingPrice(bundledPopRel.getId(), null);
			if (productOfferingPrice != null) {
				bundledPops.add(productOfferingPrice);
			}else {
				throw (IllegalStateException)new IllegalStateException(String.format("ProductOfferingPrice with id %s not found on server!", bundledPopRel.getId()));
			}
			
			/*try {
				bundledPops.add(popApi.getProductOfferingPrice(bundledPopRel.getId(), null));
			} catch (ApiException exc) {
				if (exc.getCode() == HttpStatus.NOT_FOUND.value()) {
					throw (IllegalStateException)new IllegalStateException(String.format("ProductOfferingPrice with id %s not found on server!", bundledPopRel.getId())).initCause(exc);
				}
				throw exc;
			}*/
		}
		
		return bundledPops;
	}
	
	/*
	 * This method creates a relatedParty TMF678 and sets its attributes with the same values of the relatedParty in input (TMF637).
	 *
	 * @param rpTMF637 The relatedParty entity of TMF637 specification
	 * @return the relatedParty entity of TMF678 specification with the same value of the relatedParty in input
	 */
	public static it.eng.dome.tmforum.tmf678.v4.model.RelatedParty createRelatedParty_TMF678(RelatedParty rpTMF637){
		it.eng.dome.tmforum.tmf678.v4.model.RelatedParty rpTMF678=new it.eng.dome.tmforum.tmf678.v4.model.RelatedParty();
		
		rpTMF678.setAtBaseType(rpTMF637.getAtBaseType());
		rpTMF678.setAtReferredType(rpTMF637.getAtReferredType());
		rpTMF678.setAtSchemaLocation(rpTMF637.getAtSchemaLocation());
		//rpTMF678.setAtType(rpTMF637.getAtType()); // Bad Request => The request contained invalid data - Did not receive a valid entity: Client 'ngsi'.
		rpTMF678.setHref(URI.create(rpTMF637.getHref()));
		rpTMF678.setId(rpTMF637.getId());
		rpTMF678.setName(rpTMF637.getName());
		rpTMF678.setRole(rpTMF637.getRole());
		
		return rpTMF678;
	}
	
	public static Map<String, List<UsageCharacteristic>> createUsageCharacteristicDataMap(@NonNull List<Usage> usages){
		Map<String, List<UsageCharacteristic>> usageData=new HashMap<String, List<UsageCharacteristic>>();
		
		for (Usage usage : usages) {
			if (usage.getUsageCharacteristic() != null && !usage.getUsageCharacteristic().isEmpty()) {
				
				List<UsageCharacteristic> usageCharacteristics = usage.getUsageCharacteristic();
				
				for (UsageCharacteristic usageCharacteristic : usageCharacteristics) {
					if (usageCharacteristic != null) {
						usageData.computeIfAbsent(usageCharacteristic.getName(), K -> new ArrayList<UsageCharacteristic>()).add(usageCharacteristic);
					}
					else {
						logger.warn("UsageCharacteristic cannot be null for usage with usageSpecification: {}", usage.getUsageSpecification());
					}
				}
			}
			else {
				logger.warn("UsageCharacteristic list cannot be null or empty for usage: {}", usage.getId());
			}
		}
		
		return usageData;
	}
	
	public static String getPOPUnitOfMeasure_Units(@NonNull ProductOfferingPrice pop) {
		
		String units = pop.getUnitOfMeasure().getUnits();
		logger.info("UnitOfMeasure of POP {} with units {}",pop.getId(), pop.getUnitOfMeasure().getUnits());
		
		return units;
		
	}
	
	public static float getPOPUnitOfMeasure_Amount(@NonNull ProductOfferingPrice pop) {
		float amount;
		
		amount=pop.getUnitOfMeasure().getAmount();
		logger.info("UnitOfMeasure of POP {} with amount {}",pop.getId(), pop.getUnitOfMeasure().getAmount());
		
		return amount;
		
	}
	
	/*
	 * Retrieves the list of Usage for the specified Product id and related to the specified TimePeriod 
	 */
	public static List<Usage> getUsages(@NonNull String productId, @NonNull TimePeriod tp, UsageManagementApis usageManagementApis){
		
		// add filter for usages 
		// Get all Usage related to the product and within the TimePeriod
		Map<String, String> filter = new HashMap<String, String>();
		filter.put("ratedProductUsage.productRef", productId);
		filter.put("usageDate.lt", tp.getEndDateTime().toString());
		filter.put("usageDate.gt", tp.getStartDateTime().toString());
			
		//List<Usage> usages = usageManagementApis.getAllUsages(null, filter);
		
		//TODO check
		List<Usage> usages = FetchUtils.streamAll(
				usageManagementApis::listUsages, 				// method reference
		        null,                       	// fields
		        filter, 				   		// filter
		        100                         	// pageSize
			) 
			.toList();	
		
		return usages;
	}
	
	public static List<UsageCharacteristic> getUsageCharacteristicsForMetric(@NonNull Map<String, List<UsageCharacteristic>> usageData, @NonNull String metric){
		return usageData.get(metric);
	}
	
	
	
	

}
