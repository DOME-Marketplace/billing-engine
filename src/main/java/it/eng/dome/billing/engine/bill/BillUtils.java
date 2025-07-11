package it.eng.dome.billing.engine.bill;

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.brokerage.api.ProductOfferingPriceApis;
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
	
	public static String getPriceTypeFromProductPrice(@NonNull ProductPrice productPrice, @NonNull ProductOfferingPriceApis popApis) throws Exception{
		ProductOfferingPriceRef productOfferingPriceRef=productPrice.getProductOfferingPrice();
		//try {
			ProductOfferingPrice pop = popApis.getProductOfferingPrice(productOfferingPriceRef.getId(), null);
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

		// Bugfix: need to set the Id for validation
		// appliedCustomerBillingRate.setId("bill" + product.getId().replace("urn:ngsi-ld:", "-"));

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
	public static List<ProductOfferingPrice> getBundledPops(ProductOfferingPrice pop, ProductOfferingPriceApis popApi) throws Exception {
		final List<ProductOfferingPrice> bundledPops = new ArrayList<ProductOfferingPrice>();
		
		for (var bundledPopRel : pop.getBundledPopRelationship()) {
			logger.debug("Retrieving remote ProductOfferingPrice with id: '{}'", bundledPopRel.getId());
			ProductOfferingPrice productOfferingPrice = popApi.getProductOfferingPrice(bundledPopRel.getId(), null);
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
	
	public static Map<String, List<UsageCharacteristic>> createUsageDataMap(@NonNull List<Usage> usages){
		Map<String, List<UsageCharacteristic>> usageData=new HashMap<String, List<UsageCharacteristic>>();
		
		for (Usage usage : usages) {
			if (usage.getUsageCharacteristic() != null && !usage.getUsageCharacteristic().isEmpty()) {
				logger.debug(" -> Current usage: " + usage.getId());
				
				List<UsageCharacteristic> usageCharacteristics = usage.getUsageCharacteristic();
				logger.debug(" -> UsageCharacterustic list size(): " + usageCharacteristics.size());
				
				for (UsageCharacteristic usageCharacteristic : usageCharacteristics) {
					if (usageCharacteristic != null) {
						usageData.computeIfAbsent(usageCharacteristic.getName(), K -> new ArrayList<UsageCharacteristic>()).add(usageCharacteristic);
					}
					else {
						logger.warn("UsageCharacteristic cannot be null for usage: {}", usage.getId());
					}
				}
			}
			else {
				logger.warn("UsageCharacteristic list cannot be null or empty for usage: {}", usage.getId());
			}
		}
		
		return usageData;
	}
	
	// TODO: REMOVE is only for test purposes
	public static Usage getUsageExampleA() {
		Usage usageA=null;
		
		String usageAStr="{'description':'Windows VM usage','usageType':'VM_USAGE','ratedProductUsage':[{'productRef':{'id':'urn:ngsi-ld:product:19402bf1-0889-46e5-9f6c-6fc458be024f','href':'urn:ngsi-ld:product:19402bf1-0889-46e5-9f6c-6fc458be024f','name':'Product for VDC Test'}}],'relatedParty':[{'id':'urn:ngsi-ld:organization:38063c78-fc9f-42ca-a39e-518107a2d403','href':'urn:ngsi-ld:organization:38063c78-fc9f-42ca-a39e-518107a2d403','name':'FICODES','role':'seller','@referredType':'organization'},{'id':'urn:ngsi-ld:organization:38817de3-8c3e-4141-a344-86ffd915cc3b','href':'urn:ngsi-ld:organization:38817de3-8c3e-4141-a344-86ffd915cc3b','name':'DHUB, Engineering D.HUB S.p.A.','role':'buyer','@referredType':'organization'}],'status':'received','usageCharacteristic':[{'id':'7bbc511e-b227-455c-8fd2-47377cebdca6','name':'CORE_hour','valueType':'float','value':2.45},{'id':'81e3b6fc-9be5-4680-b736-d7add7c72dec','name':'RAM_GB_hour','valueType':'float','value':45.37},{'id':'b1b2ecff-1586-4545-b6f4-384ce685070a','name':'DISK_GB_hour','valueType':'float','value':6122.59}]}";

		try {
			usageA=Usage.fromJson(usageAStr);
			usageA.setId("usageA");
			
		} catch (IOException e) {
			logger.error("Error: {}", e.getMessage());
		}
			
		return usageA;
	}
	
	// TODO: REMOVE is only for test purposes
	public static Usage getUsageExampleB() {
		Usage usageB=null;
		
		String usageBStr= "{'description':'Windows VM2 usage','usageType':'VM_USAGE','ratedProductUsage':[{'productRef':{'id':'urn:ngsi-ld:product:19402bf1-0889-46e5-9f6c-6fc458be024f','href':'urn:ngsi-ld:product:19402bf1-0889-46e5-9f6c-6fc458be024f','name':'Product for VDC Test'}}],'relatedParty':[{'id':'urn:ngsi-ld:organization:38063c78-fc9f-42ca-a39e-518107a2d403','href':'urn:ngsi-ld:organization:38063c78-fc9f-42ca-a39e-518107a2d403','name':'FICODES','role':'seller','@referredType':'organization'},{'id':'urn:ngsi-ld:organization:38817de3-8c3e-4141-a344-86ffd915cc3b','href':'urn:ngsi-ld:organization:38817de3-8c3e-4141-a344-86ffd915cc3b','name':'DHUB, Engineering D.HUB S.p.A.','role':'buyer','@referredType':'organization'}],'status':'received','usageCharacteristic':[{'id':'7bbc511e-b227-455c-8fd2-47377cebdca6','name':'CORE_hour','valueType':'float','value':3.0}]}";
		
		try {
			
			usageB=Usage.fromJson(usageBStr);
			usageB.setId("usageB");
			
		} catch (IOException e) {
			logger.error("Error: {}", e.getMessage());
		}
		
		return usageB;
	}
	
	

}
