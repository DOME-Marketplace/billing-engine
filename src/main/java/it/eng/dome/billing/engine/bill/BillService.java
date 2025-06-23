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
import it.eng.dome.brokerage.api.ProductApis;
import it.eng.dome.brokerage.api.ProductOfferingPriceApis;
import it.eng.dome.brokerage.api.UsageManagementApis;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf635.v4.model.RatedProductUsage;
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
	private ProductApis productApis;

	@Override
	public void afterPropertiesSet() throws Exception {
		productOfferingPriceApis = new ProductOfferingPriceApis(tmfApiFactory.getTMF620ProductCatalogApiClient());
		usageManagementApis = new UsageManagementApis(tmfApiFactory.getTMF635UsageManagementApiClient());
		productApis = new ProductApis(tmfApiFactory.getTMF637ProductInventoryApiClient());
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
		String appliedBillingRateType=null;

		for (ProductPrice productPrice : ppList) {
			
			// 1) retrieves the productOfferingPrice reference from the ProductPrice
			ProductOfferingPriceRef productOfferingPriceRef = productPrice.getProductOfferingPrice();
			if(productOfferingPriceRef==null)
				throw new BillingBadRequestException("The ProductOfferingPrice reference is missing in the ProductPrice " + productPrice.getName());
			
			// 2) retrieves from the server the ProductOfferingPrice
			ProductOfferingPrice pop = productOfferingPriceApis.getProductOfferingPrice(productOfferingPriceRef.getId(), null);
			logger.info("Calculate price for the price component with ProductOfferingPrice: "+pop.getId());
			
			// if POP is not bundle
			if(!pop.getIsBundle()) {
				prices.add(PriceUtils.calculatePrice(pop,productChars,priceAlterationCalculator));
				// Set the appliedBillingRateType if not set yet
				if(appliedBillingRateType == null) {
					appliedBillingRateType = pop.getPriceType();
				}
			} else {
				logger.info("ProductOfferingPrice: "+pop.getId()+" is bundled");
				
				List<ProductOfferingPrice> bundledPops=BillUtils.getBundledPops(pop, productOfferingPriceApis);
				if (bundledPops == null || bundledPops.isEmpty()) {
					throw new BillingBadRequestException(String.format("Error! Started calculation of bundled ProductOfferingPrice %s but the 'bundledPopRelationship' is empty!" + pop.getId()));
				}
				for(ProductOfferingPrice bundledPop: bundledPops) {
					prices.add(PriceUtils.calculatePrice(bundledPop,productChars, priceAlterationCalculator));
					// Set the appliedBillingRateType if not set yet
					if(appliedBillingRateType == null) {
						appliedBillingRateType=bundledPop.getPriceType();
					}
				}
			}
		}
			
		for(Price price:prices) {
			Float newTaxExcludedAmount = taxExcludedAmount.getValue() + price.getDutyFreeAmount().getValue();
			taxExcludedAmount.setValue(newTaxExcludedAmount);
		}
		
		logger.info("Bill total amount {} euro", taxExcludedAmount.getValue());
		
		appliedCustomerBillingRate=BillUtils.createAppliedCustomerBillingRate(product, tp, taxExcludedAmount, appliedBillingRateType,tmfApiFactory.getSchemaLocationRelatedParty());
		
		// Add the generate appliedCustomerBillingRate to the AppliedCustomerBillingRate list (at the moment only one element is present on the list)
		appliedCustomerBillRateList.add(appliedCustomerBillingRate);

		return appliedCustomerBillRateList;
	}
	

	public List<AppliedCustomerBillingRate> calculatePayPerUse(Product product, TimePeriod tp) throws Exception {
		List<AppliedCustomerBillingRate> appliedCustomerBillRateList = new ArrayList<AppliedCustomerBillingRate>();

		// Instance of the AppliedCustomerBillingRate generated from inputs parameters
		AppliedCustomerBillingRate appliedCustomerBillingRate;

		// Bill taxExcludedAmount
		Money taxExcludedAmount = new Money();
		taxExcludedAmount.setUnit("EUR");
		taxExcludedAmount.setValue(getPayPerUse(tp));

		logger.info("Bill pay-per-use total amount {} euro", taxExcludedAmount.getValue());
		
		String appliedBillingRateType = "pay-per-use";

		appliedCustomerBillingRate = BillUtils.createAppliedCustomerBillingRate(product, tp, taxExcludedAmount, appliedBillingRateType, tmfApiFactory.getSchemaLocationRelatedParty());
		
		// Add the generate appliedCustomerBillingRate to the AppliedCustomerBillingRate list (at the moment only one element is present on the list)
		appliedCustomerBillRateList.add(appliedCustomerBillingRate);

		return appliedCustomerBillRateList;
	}
	
	
	// TODO implement method to get PPU value
	private float getPayPerUse(TimePeriod tp) {
		
		Map<String, String> filter = new HashMap<String, String>();
		//filter.put("usageDate.gt", tp.getStartDateTime().toString());
		//filter.put("usageDate.lt", tp.getEndDateTime().toString());
		
		List<Usage> usages = usageManagementApis.getAllUsages(null, filter);
		logger.info("Usage found: {}", usages.size());
		
		float amount = 0;
		
		//TODO - must be implemented
		for (Usage usage : usages) {
			if (usage.getRatedProductUsage() != null && usage.getRatedProductUsage().size() > 0) {
				
				Map<String, Object> usageData = new HashMap<String, Object>();
				//usage.getRelatedParty();
				
				List<UsageCharacteristic> usageCharacteristics = usage.getUsageCharacteristic();
				for (UsageCharacteristic usageCharacteristic : usageCharacteristics) {
					if (usageCharacteristic != null) {
						usageData.put(usageCharacteristic.getName(), usageCharacteristic.getValue());
					}
				}
				
				List<RatedProductUsage> rates = usage.getRatedProductUsage();
				for (RatedProductUsage rate: rates) {
					String idProduct = rate.getProductRef().getId();
					Product product = productApis.getProduct(idProduct, null);
					if (product.getProductPrice() != null) {
						List<ProductPrice> pprices = product.getProductPrice();
						if (pprices != null && !pprices.isEmpty()) {
							
							for (ProductPrice pprice : pprices) {
								// retrieve price
								String key = pprice.getName();
								if (usageData.containsKey(key)) {
									Object value = usageData.get(key);
									if (value instanceof Number) {
										Float price = (float) pprice.getPrice().getTaxIncludedAmount().getValue();
										amount += price * (float)value;
									}
								}
								
							}
						}
					}
				}
			} else {
				logger.warn("RatedProductUsage cannot be null for usage: {}", usage.getId());
			}
		}
		
		return amount;
	}
}
