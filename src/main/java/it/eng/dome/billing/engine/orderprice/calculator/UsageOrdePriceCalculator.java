package it.eng.dome.billing.engine.orderprice.calculator;

import java.util.ArrayList;
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
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

public class UsageOrdePriceCalculator extends AbstractBaseOrderPriceCalculator{
	
	private final Logger logger = LoggerFactory.getLogger(UsageOrdePriceCalculator.class);
	
    List<Usage> usages;
	// Map with key=usageCharacteristic.name and value=list of UsageCharacteritic
	private	Map<String, List<UsageCharacteristic>> usageData= null;
	                                                                                                                                                                                                                                                                                                                                                                                                                                                                       

	public UsageOrdePriceCalculator(ProductOfferingPrice pop, ProductOrderItem productOrderItem, List<Usage> usages) {
		super(pop,productOrderItem);

		this.usages = usages;
	}

	@Override
	public List<OrderPrice> calculateOrderPrice() throws BillingEngineValidationException, ApiException{
		
		logger.info("Calculating price preview for usage ProductOfferingPrice '{}' of ProductOrderItem", pop.getId(), productOrderItem.getId());
		
		List<OrderPrice> orderPriceList=new ArrayList<OrderPrice>();
		List<Price> usagePrices=new ArrayList<Price>();
		
		float totalAmount=0;
		Money totalAmountMoney=null;
		
		inizializeUsageData(usages);
		
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
			Money alteretedPrice=priceAlterationCalculator.applyAlterations(new Money(priceCurrency, totalAmount), ProductOfferingPriceUtils.getProductOfferingPriceRelationships(pop.getPopRelationship(), productCatalogManagementApis));
											
			logger.info("Price of ProductOfferingPrice '{}' after alterations = {} {}", 
					pop.getId(), alteretedPrice.getValue(), alteretedPrice.getUnit());	
		
			return alteretedPrice;
		}
	
		//return totalAmountMoney;
		return orderPriceList;
		
	}
	
	/*
	 * Initialize the HashMap of UsageCharacteristic retrieving via TMForum all the usageData associated with the specified product ID and belonging to the specified TimePeriod
	 */
	private Map<String, List<UsageCharacteristic>> inizializeUsageData(@NonNull List<Usage> usages) throws BillingEngineValidationException{
		
		logger.debug("Simulated usages: {}", usages.size());
		
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
