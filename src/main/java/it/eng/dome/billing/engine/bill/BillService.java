package it.eng.dome.billing.engine.bill;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import it.eng.dome.billing.engine.tmf.TmfApiFactory;
import it.eng.dome.tmforum.tmf620.v4.ApiClient;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingPriceApi;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.ProductOfferingPriceRef;
import it.eng.dome.tmforum.tmf637.v4.model.ProductPrice;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.BillingAccountRef;
import it.eng.dome.tmforum.tmf678.v4.model.Money;
import it.eng.dome.tmforum.tmf678.v4.model.ProductRef;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;

@Component(value = "billService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BillService implements InitializingBean{
	
	private final Logger logger = LoggerFactory.getLogger(BillService.class);
			
	@Autowired
	private TmfApiFactory tmfApiFactory;
	
	private ProductOfferingPriceApi popApi;

	@Override
	public void afterPropertiesSet() throws Exception {
		final ApiClient apiClient = tmfApiFactory.getTMF620ProductCatalogApiClient();
	    popApi = new ProductOfferingPriceApi(apiClient);
		
	}
	
	public ArrayList<AppliedCustomerBillingRate> calculateBill(Product product, TimePeriod tp, List<ProductPrice> ppList) throws Exception{
		ArrayList<AppliedCustomerBillingRate> appliedCustomerBillRateList=new ArrayList<AppliedCustomerBillingRate>();
		Assert.state(!CollectionUtils.isEmpty(ppList), "Cannot calculate bill for empty 'productPrice' list!");
		
		// Instance of the AppliedCustomerBillingRate generated from inputs parameters
		AppliedCustomerBillingRate appliedCustomerBillingRate=new AppliedCustomerBillingRate();
		
		// Bill taxExcludedAmount
		Money taxExcludedAmount=new Money();
		taxExcludedAmount.setUnit("EUR");
		taxExcludedAmount.setValue(0.0f);
		
		// Create billingAccountRef from Product
		Assert.state(!Objects.isNull(product.getBillingAccount()), "Billing Account is missing in the product with ID: "+product.getId());
		BillingAccountRef billingAccountRef= BillUtils.createAppliedCustomerRateBillingAccount(product.getBillingAccount());
		
		// Set appliedCustomerBillingRate.billingAccount
		appliedCustomerBillingRate.setBillingAccount(billingAccountRef);
		
		// Set appliedCustomerBillingRate.date
		appliedCustomerBillingRate.setDate(OffsetDateTime.now());
		
		// Set appliedCustomerBillingRate.description
		appliedCustomerBillingRate.setDescription("Bill for product with ID: "+product.getId()+" TimePeriod "+tp.getStartDateTime()+"/"+tp.getEndDateTime());
		
		// Set appliedCustomerBillingRate.isBilled to false because the billingAccount is valorised
		appliedCustomerBillingRate.setIsBilled(false);
		
		// Set appliedCustomerBillingRate.name 
		appliedCustomerBillingRate.setName("Bill for product with ID: "+product.getId());
		
		// Set appliedCustomerBillingRate.periodCoverage
		appliedCustomerBillingRate.setPeriodCoverage(tp);
		
		// Set appliedCustomerBillingRate.product reference
		ProductRef prodRef=new ProductRef();
		prodRef.setId(product.getId());
		appliedCustomerBillingRate.setProduct(prodRef);
		
		for(ProductPrice productPrice: ppList) {
			
			// 1) retrieves the productOfferingPrice reference from the ProductPrice
			ProductOfferingPriceRef productOfferingPriceRef=productPrice.getProductOfferingPrice();
			Assert.state(!Objects.isNull(productOfferingPriceRef), "The ProductOfferingPrice reference is missing in the ProductPrice "+productPrice.getName());
			
			
			// 2) retrieves from the server the ProductOfferingPrice
	    	ProductOfferingPrice pop = popApi.retrieveProductOfferingPrice(productOfferingPriceRef.getId(), null);
	    	
	    	logger.debug("Price for ProductOfferingPrice with id "+pop.getId()+": "+pop.getPrice().getValue()+pop.getPrice().getUnit());
	    	
	    	// set taxExcludedAmount
	    	Float newTaxExcludedAmount=taxExcludedAmount.getValue()+pop.getPrice().getValue();
	    	taxExcludedAmount.setValue(newTaxExcludedAmount);
			
		}
		
		logger.info("Total Price Amount Tax Excluted: "+taxExcludedAmount.getValue()+taxExcludedAmount.getUnit());
		appliedCustomerBillingRate.setTaxExcludedAmount(taxExcludedAmount);
		
		// Add the generate appliedCustomerBillingRate to the AppliedCustomerBillingRate list (at the moment only one element is present on the list)
		appliedCustomerBillRateList.add(appliedCustomerBillingRate);
		
		return appliedCustomerBillRateList;
	}

}
