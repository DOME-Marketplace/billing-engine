package it.eng.dome.billing.engine.bill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.price.PriceUtils;
import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.billing.engine.tmf.TmfApiFactory;
import it.eng.dome.brokerage.api.ProductOfferingPriceApis;
import it.eng.dome.brokerage.api.UsageManagementApis;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.ProductOfferingPriceRef;
import it.eng.dome.tmforum.tmf637.v4.model.ProductPrice;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.Money;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;

@Component(value = "billService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BillService implements InitializingBean {

	private final Logger logger = LoggerFactory.getLogger(BillService.class);

	@Autowired
	private TmfApiFactory tmfApiFactory;
	
	@Autowired
	private PriceAlterationCalculator priceAlterationCalculator; 

	private ProductOfferingPriceApis productOfferingPriceApis;
	private UsageManagementApis usageManagementApis;
	//private ProductApis productApis;

	@Override
	public void afterPropertiesSet() throws Exception {
		productOfferingPriceApis = new ProductOfferingPriceApis(tmfApiFactory.getTMF620ProductCatalogApiClient());
		usageManagementApis = new UsageManagementApis(tmfApiFactory.getTMF635UsageManagementApiClient());
		//productApis = new ProductApis(tmfApiFactory.getTMF637ProductInventoryApiClient());
	}

	public List<AppliedCustomerBillingRate> calculateBill(Product product, TimePeriod tp, List<ProductPrice> ppList) throws Exception {
		
		List<AppliedCustomerBillingRate> appliedCustomerBillRateList = new ArrayList<AppliedCustomerBillingRate>();

		// Instance of the AppliedCustomerBillingRate generated from inputs parameters
		AppliedCustomerBillingRate appliedCustomerBillingRate;
		
		// gets the characteristic chosen by the Customer
		final var productChars = product.getProductCharacteristic();
		
		// All calculated prices
		List<Price> prices = new ArrayList<Price>();

		// Bill taxExcludedAmount
		Money taxExcludedAmount = new Money();
		taxExcludedAmount.setUnit("EUR");
		taxExcludedAmount.setValue(0.0f);
		
		// Bill appliedBillingRateType of the productPrices belonging to the same group
		String appliedBillingRateType = null;

		for (ProductPrice productPrice : ppList) {
			
			// 1) retrieves the productOfferingPrice reference from the ProductPrice
			ProductOfferingPriceRef productOfferingPriceRef = productPrice.getProductOfferingPrice();
			if(productOfferingPriceRef == null) {
				throw new BillingBadRequestException("The ProductOfferingPrice reference is missing in the ProductPrice " + productPrice.getName());
			}
			
			// 2) retrieves from the server the ProductOfferingPrice
			ProductOfferingPrice pop = productOfferingPriceApis.getProductOfferingPrice(productOfferingPriceRef.getId(), null);
			logger.info("Calculate price for the price component with ProductOfferingPrice: "+pop.getId());
			
			// if POP is not bundle
			if(!pop.getIsBundle()) {
				prices.add(PriceUtils.calculatePrice(pop, productChars, priceAlterationCalculator));
				// Set the appliedBillingRateType if not set yet
				if(appliedBillingRateType == null) {
					appliedBillingRateType = pop.getPriceType();
				}
			} else {
				logger.info("ProductOfferingPrice: {}", pop.getId()+" is bundled");
				
				List<ProductOfferingPrice> bundledPops = BillUtils.getBundledPops(pop, productOfferingPriceApis);
				if (bundledPops == null || bundledPops.isEmpty()) {
					throw new BillingBadRequestException(String.format("Error! Started calculation of bundled ProductOfferingPrice %s but the 'bundledPopRelationship' is empty!" + pop.getId()));
				}
				for(ProductOfferingPrice bundledPop: bundledPops) {
					prices.add(PriceUtils.calculatePrice(bundledPop, productChars, priceAlterationCalculator));
					// Set the appliedBillingRateType if not set yet
					if(appliedBillingRateType == null) {
						appliedBillingRateType = bundledPop.getPriceType();
					}
				}
			}
		}
			
		for(Price price : prices) {
			Float newTaxExcludedAmount = taxExcludedAmount.getValue() + price.getDutyFreeAmount().getValue();
			taxExcludedAmount.setValue(newTaxExcludedAmount);
		}
		
		logger.info("Bill total amount for priceType {}: {} euro", appliedBillingRateType, taxExcludedAmount.getValue());
		
		appliedCustomerBillingRate = BillUtils.createAppliedCustomerBillingRate(product, tp, taxExcludedAmount, appliedBillingRateType, tmfApiFactory.getSchemaLocationRelatedParty());
		
		// Add the generate appliedCustomerBillingRate to the AppliedCustomerBillingRate list (at the moment only one element is present on the list)
		appliedCustomerBillRateList.add(appliedCustomerBillingRate);

		return appliedCustomerBillRateList;
	}
	

	public List<AppliedCustomerBillingRate> calculateBillForPayPerUse(Product product, TimePeriod tp, List<ProductPrice> ppList) throws Exception {
		List<AppliedCustomerBillingRate> appliedCustomerBillRateList = new ArrayList<AppliedCustomerBillingRate>();

		// Instance of the AppliedCustomerBillingRate generated from inputs parameters
		AppliedCustomerBillingRate appliedCustomerBillingRate;

		// Bill taxExcludedAmount
		Money taxExcludedAmount = new Money();
		taxExcludedAmount.setUnit("EUR");
		taxExcludedAmount.setValue(getPayPerUseAmount(product, tp, ppList));

		String appliedBillingRateType = "pay-per-use";
		logger.info("Bill total amount for priceType {}: {} euro", appliedBillingRateType, taxExcludedAmount.getValue());	

		appliedCustomerBillingRate = BillUtils.createAppliedCustomerBillingRate(product, tp, taxExcludedAmount, appliedBillingRateType, tmfApiFactory.getSchemaLocationRelatedParty());
		
		// Add the generate appliedCustomerBillingRate to the AppliedCustomerBillingRate list (at the moment only one element is present on the list)
		appliedCustomerBillRateList.add(appliedCustomerBillingRate);

		return appliedCustomerBillRateList;
	}
	
	
	// Method to get PPU amount to pay 
	private float getPayPerUseAmount(Product product, TimePeriod tp,  List<ProductPrice> ppList) {
		
		float amount = 0;
		List<Price> usagePrices=new ArrayList<Price>();
		
		// add filter for usages 
		// Get all Usage related to the product and within the TimePerios
		Map<String, String> filter = new HashMap<String, String>();
		filter.put("ratedProductUsage.productRef", product.getId());
		filter.put("usageDate.lt", tp.getEndDateTime().toString());
		filter.put("usageDate.gt", tp.getStartDateTime().toString());
	
		//TODO: TO FIX - due to the current bug in TMForum APIs
		List<Usage> usages = usageManagementApis.getAllUsages(null, filter);
		logger.info("Usage found: {}", usages.size());
		
		// TODO: TO REMOVE is only for test purposes
		//Usage usageA=BillUtils.getUsageExampleA();
		//Usage usageB=BillUtils.getUsageExampleB();
		//List<Usage> usages=new ArrayList<Usage>();
		//usages.add(usageA);
		//usages.add(usageB);
		
		// Create the Map with key the usageCharacteristic.name and value a list of UsageCharacteritic
		Map<String, List<UsageCharacteristic>> usageData= BillUtils.createUsageDataMap(usages);
		
		logger.info("Created UsageDataMap with keys "+usageData.keySet().toString());
		
		if (ppList != null && !ppList.isEmpty()) {	
			for (ProductPrice pprice : ppList) {
				// Retrieve ProductOfferingPrice
				if(pprice.getProductOfferingPrice()==null)
					throw new BillingBadRequestException(String.format("Error! Started calculation of the pay-per-use bill for the product %s but the ProductOfferingOPrice of a price component is null!",product.getId()));
				
				String popId=pprice.getProductOfferingPrice().getId();
				ProductOfferingPrice pop=productOfferingPriceApis.getProductOfferingPrice(popId, null);
				
				// Retrieve the metric from the unitOfMeasure of the POP
				String metric=pop.getUnitOfMeasure().getUnits();
				logger.debug("UnitOfMeasure of POP {}: units {}, value {}",popId, pop.getUnitOfMeasure().getUnits(), pop.getUnitOfMeasure().getAmount());
				
				List<UsageCharacteristic> usageChForMetric=usageData.get(metric);
				logger.debug("Size of the list of UsageCharacteristic for metric {}: {}",metric,usageChForMetric.size());
				
				for(UsageCharacteristic usageCh:usageChForMetric) {
					Price usageChPrice= PriceUtils.calculatePriceForUsageCharacteristic(pop, usageCh);
					usagePrices.add(usageChPrice);
				}	
			}
		}
		
		for(Price price:usagePrices) {
			amount += price.getDutyFreeAmount().getValue();
		}
		
		logger.info("Total amount of usageData for the Product '{}' in the TimePeriod '{}'-'{}': ", product.getId(), tp.getStartDateTime(), tp.getEndDateTime(), amount);
		
		return amount;
	}
}
