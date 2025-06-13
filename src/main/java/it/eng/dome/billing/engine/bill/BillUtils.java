package it.eng.dome.billing.engine.bill;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.brokerage.api.ProductOfferingPriceApis;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
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
		
		 it.eng.dome.tmforum.tmf678.v4.model.BillingAccountRef billingAccountRef=new it.eng.dome.tmforum.tmf678.v4.model.BillingAccountRef();
		 
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
		appliedCustomerBillingRate.setId("bill" + product.getId().replace("urn:ngsi-ld:", "-"));

		// Set appliedCustomerBillingRate.billingAccount
		if(product.getBillingAccount()==null)
			throw new BillingBadRequestException("Billing Account is missing in the product with ID: " + product.getId());
		
		appliedCustomerBillingRate.setBillingAccount(BillUtils.createAppliedCustomerRateBillingAccount(product.getBillingAccount()));

		// Set appliedCustomerBillingRate.date
		appliedCustomerBillingRate.setDate(OffsetDateTime.now());

		// Set appliedCustomerBillingRate.description
		appliedCustomerBillingRate.setDescription("Bill for product with ID: " + product.getId() + " TimePeriod " + tp.getStartDateTime() + "/" + tp.getEndDateTime());

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
		List<RelatedParty> prodRelatedParty=product.getRelatedParty();
		
		if(prodRelatedParty!=null) {
			logger.debug("List of relatedParty from Product - size= "+prodRelatedParty.size());
			
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
		rpTMF678.setAtType(rpTMF637.getAtType());
		rpTMF678.setHref(URI.create(rpTMF637.getHref()));
		rpTMF678.setId(rpTMF637.getId());
		rpTMF678.setName(rpTMF637.getName());
		rpTMF678.setRole(rpTMF637.getRole());
		
		return rpTMF678;
	}
	
	

}
