package it.eng.dome.billing.engine.bill;

import java.util.ArrayList;
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
import it.eng.dome.billing.engine.utils.BillingPriceType;
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
	
	// Map with key the usageCharacteristic.name and value a list of UsageCharacteritic
	private	Map<String, List<UsageCharacteristic>> usageData= null;

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
				String priceTypeNormalized=BillingPriceType.normalize(pop.getPriceType());
				// Not pay-per-use price plan
				if(!priceTypeNormalized.equalsIgnoreCase(BillingPriceType.PAY_PER_USE.getNormalizedKey())) {
					prices.add(PriceUtils.calculatePrice(pop, productChars, priceAlterationCalculator));
				}
				// Pay-per-use price plan
				else {
					// Initialize the usageData Map if not initialized yet
					if(usageData==null) {
						List<Usage> usages=BillUtils.getUsages(product.getId(), tp, usageManagementApis);
						//logger.info("Usage found: {}", usages.size());
						usageData=BillUtils.createUsageCharacteristicDataMap(usages);
						logger.info("Created UsageDataMap with keys "+usageData.keySet().toString());
					}
					prices.add(PriceUtils.calculatePriceForPayPerUse(pop, usageData, priceAlterationCalculator));
				}
				
				// Set the appliedBillingRateType if not set yet
				if(appliedBillingRateType == null) {
					appliedBillingRateType = priceTypeNormalized;
				}
				
			} else {
				logger.info("ProductOfferingPrice: {}", pop.getId()+" is bundled");
				
				List<ProductOfferingPrice> bundledPops = BillUtils.getBundledPops(pop, productOfferingPriceApis);
				if (bundledPops == null || bundledPops.isEmpty()) {
					throw new BillingBadRequestException(String.format("Error! Started calculation of bundled ProductOfferingPrice %s but the 'bundledPopRelationship' is empty!", pop.getId()));
				}
				for(ProductOfferingPrice bundledPop: bundledPops) {
					String priceTypeNormalized=BillingPriceType.normalize(pop.getPriceType());
					// Not pay-per-use price plan
					if(!priceTypeNormalized.equalsIgnoreCase(BillingPriceType.PAY_PER_USE.getNormalizedKey())) {
						prices.add(PriceUtils.calculatePrice(bundledPop, productChars, priceAlterationCalculator));
					}
					else {
						prices.add(PriceUtils.calculatePriceForPayPerUse(pop, usageData, priceAlterationCalculator));
					}
					
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
}
