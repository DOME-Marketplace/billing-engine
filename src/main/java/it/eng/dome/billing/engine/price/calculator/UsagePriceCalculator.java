package it.eng.dome.billing.engine.price.calculator;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.utils.UsageUtils;
import it.eng.dome.brokerage.api.UsageManagementApis;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

@Component
public class UsagePriceCalculator extends AbstractPriceCalculator<Product, Money>{
	
	private final Logger logger = LoggerFactory.getLogger(UsagePriceCalculator.class);
	
	private TimePeriod billingPeriod;      
	
	@Autowired
	private UsageManagementApis usageManagementApis;
	
	public UsagePriceCalculator() {
		super();
	}

	@Override
	public Money calculatePrice(Product prod) throws BillingEngineValidationException, ApiException{
		
		logger.info("Calculating price for POP '{}' USAGE of Product '{}'", pop.getId(), prod.getId());
		
		List<Usage> usages=getUsages(prod.getId(), billingPeriod);
		usageData=inizializeUsageData(usages);
		
		Money totalAmountMoney=this.calculatePriceforUsageCharacteristics();
		logger.info("Price of ProductOfferingPrice '{}' = {} {}", pop.getId(), totalAmountMoney.getValue(), priceCurrency);
		
		// apply price alterations
		if (ProductOfferingPriceUtils.hasRelationships(pop)) {
			Money alteretedPrice=priceAlterationCalculator.applyAlterations(totalAmountMoney, ProductOfferingPriceUtils.getProductOfferingPriceRelationships(pop.getPopRelationship(), productCatalogManagementApis));
											
			logger.info("Price of ProductOfferingPrice '{}' after alterations = {} {}", 
					pop.getId(), alteretedPrice.getValue(), alteretedPrice.getUnit());	
		
			return alteretedPrice;
		}
	
		return totalAmountMoney;
		
	}
	
	private List<Usage> getUsages(@NonNull String productId, @NotNull TimePeriod billingPeriod){
		List<Usage> usages=UsageUtils.getUsages(productId, billingPeriod, usageManagementApis);
		
		if (usages == null) {
		    usages = new ArrayList<>();
		} else {
		    logger.debug("Usage found: {}", usages.size());
		}

		return usages;
	}
	
	public void setBillingPeriod(@NotNull TimePeriod billingPeriod) {
		this.billingPeriod=billingPeriod;
	}

}
