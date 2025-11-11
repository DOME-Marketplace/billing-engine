package it.eng.dome.billing.engine.price.calculator;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.utils.UsageUtils;
import it.eng.dome.brokerage.api.UsageManagementApis;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.Quantity;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

public class UsagePriceCalculator extends AbstractPriceCalculator{
	
	private final Logger logger = LoggerFactory.getLogger(UsagePriceCalculator.class);
	
	private TimePeriod billiPeriod; 
	
	@Autowired
	private UsageManagementApis usageManagementApis;
	
	// Map with key=usageCharacteristic.name and value=list of UsageCharacteritic
	private	Map<String, List<UsageCharacteristic>> usageData= null;
	                                                                                                                                                                                                                                                                                                                                                                                                                                                                       

	public UsagePriceCalculator(ProductOfferingPrice pop, Product prod, TimePeriod billiPeriod) {
		super(pop,prod);

		this.billiPeriod = billiPeriod;
	}

	@Override
	public Money calculatePrice() throws BillingEngineValidationException, ApiException{
		
		logger.info("Calculating usage price for POP '{}' of Product '{}...", pop.getId(), prod.getId());
		
		float totalAmount=0;
		Money totalAmountMoney=null;
		
		inizializeUsageData(prod.getId(), billiPeriod);
		
		// Retrieve the metric from the unitOfMeasure of the POP and validate it
		tmfEntityValidator.validateUnitOfMeasure(pop.getUnitOfMeasure(), pop);
		
		String metric=pop.getUnitOfMeasure().getUnits();
		logger.debug("UnitOfMeasure of POP {}: units {}, value {}",pop.getId(), pop.getUnitOfMeasure().getUnits(), pop.getUnitOfMeasure().getAmount());
		
		List<UsageCharacteristic> usageChForMetric= UsageUtils.getUsageCharacteristicsForMetric(usageData, metric);
		
		if(usageChForMetric!=null && !usageChForMetric.isEmpty()) {
			logger.debug("Size of the list of UsageCharacteristic for metric {}: {}",metric,usageChForMetric.size());
			
			for(UsageCharacteristic usageCh:usageChForMetric) {
				Money usageChAmount= this.calculatePriceForUsageCharacteristic(usageCh);
				totalAmount+=usageChAmount.getValue();
			}
			totalAmountMoney=new Money(priceCurrency,totalAmount);
		}
		else {
			logger.warn("No usage data fount for the metric '{}' in the TimePeriod [{}-{}]",metric,billiPeriod.getStartDateTime(),billiPeriod.getEndDateTime());
		}
		
		logger.info("Price of ProductOfferingPrice '{}' = {} {}", pop.getId(), totalAmount, priceCurrency);
		
		// apply price alterations
		if (ProductOfferingPriceUtils.hasRelationships(pop)) {
			Money alteretedPrice=priceAlterationCalculator.applyAlterations(pop, new Money(priceCurrency, totalAmount));
											
			logger.info("Price of ProductOfferingPrice '{}' after alterations = {} {}", 
					pop.getId(), alteretedPrice.getValue(), alteretedPrice.getUnit());	
		
			return alteretedPrice;
		}
	
		return totalAmountMoney;
		
	}
	
	/*
	 * Initialize the HashMap of UsageCharacteristic retrieving via TMForum all the usageData associated with the specified product ID and belonging to the specified TimePeriod
	 */
	private Map<String, List<UsageCharacteristic>> inizializeUsageData(@NonNull String productId, @NotNull TimePeriod tp){
		
		List<Usage> usages=UsageUtils.getUsages(productId, tp, usageManagementApis);
		logger.info("Usage found: {}", usages.size());
		
		tmfEntityValidator.validateUsages(usages);
		
		usageData=UsageUtils.createUsageCharacteristicDataMap(usages);
		logger.info("Created UsageDataMap with keys "+usageData.keySet().toString());
		
		return usageData;
	}

	private Money calculatePriceForUsageCharacteristic(@NonNull UsageCharacteristic usageCh) {
		
		logger.debug("Calculating price for Usage Characteristic with [name:'{}' value: '{}']", usageCh.getName(), usageCh.getValue());

		final String usageChName = usageCh.getName();
		final Float usageChValue = Float.parseFloat(usageCh.getValue().toString());
		
		final Quantity unitOfMeasure = pop.getUnitOfMeasure();
		Float usageChAmount= (pop.getPrice().getValue() * usageChValue) / unitOfMeasure.getAmount();
		logger.info("Price of UsageCharacteristic '{}' with [quantity: '{}', price: '{}' per '{} {}'] = {} {}", 
					usageChName, usageChValue,
					pop.getPrice().getValue(), unitOfMeasure.getAmount(), unitOfMeasure.getUnits(), usageChAmount, priceCurrency);

		Money usageChMoney=new Money(pop.getPrice().getUnit(), usageChAmount);
		return usageChMoney;
	}

}
