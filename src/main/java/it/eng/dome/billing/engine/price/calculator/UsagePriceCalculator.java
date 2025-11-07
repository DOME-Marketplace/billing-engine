package it.eng.dome.billing.engine.price.calculator;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.eng.dome.billing.engine.bill.BillUtils;
import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.price.PriceUtils;
import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.billing.engine.utils.UsageUtils;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.api.UsageManagementApis;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.Quantity;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

public class UsagePriceCalculator implements PriceCalculator{
	
	private final Logger logger = LoggerFactory.getLogger(UsagePriceCalculator.class);
	
	private ProductOfferingPrice pop;
	private Product prod;
	private TimePeriod billiPeriod; 
	
	//private final String currency;
	//private static final String DEFAULT_CURRENCY = "EUR";
	
	@Autowired
	private UsageManagementApis usageManagementApis;
	
	@Autowired
	private TMFEntityValidator tmfEntityValidator;
	
	@Autowired
	private PriceAlterationCalculator priceAlterationCalculator; 
	
	// Map with key=usageCharacteristic.name and value=list of UsageCharacteritic
	private	Map<String, List<UsageCharacteristic>> usageData= null;
	                                                                                                                                                                                                                                                                                                                                                                                                                                                                       

	public UsagePriceCalculator(ProductOfferingPrice pop, Product prod, TimePeriod billiPeriod) {
		super();
		this.pop = pop;
		this.prod = prod;
		this.billiPeriod = billiPeriod;
		
		/*if(pop.getPrice().getUnit()==null || pop.getPrice().getUnit().isEmpty())
			this.currency=DEFAULT_CURRENCY;
		else
			this.currency=pop.getPrice().getUnit();*/
	}

	@Override
	public Money calculatePrice() throws BillingBadRequestException{
		
		float totalAmount=0;
		Money totalAmountMoney=new Money();
		
		inizializeUsageData(prod.getId(), billiPeriod);

		// Retrieve the metric from the unitOfMeasure of the POP
		if(pop.getUnitOfMeasure()==null)
			throw new BillingBadRequestException("The unitOfMeasure is missing in the ProductOfferingPrice but it is required for the Usage price type");
		
		String metric=pop.getUnitOfMeasure().getUnits();
		logger.debug("UnitOfMeasure of POP {}: units {}, value {}",pop.getId(), pop.getUnitOfMeasure().getUnits(), pop.getUnitOfMeasure().getAmount());
		
		List<UsageCharacteristic> usageChForMetric= UsageUtils.getUsageCharacteristicsForMetric(usageData, metric);
		
		if(usageChForMetric!=null && !usageChForMetric.isEmpty()) {
			logger.debug("Size of the list of UsageCharacteristic for metric {}: {}",metric,usageChForMetric.size());
			
			for(UsageCharacteristic usageCh:usageChForMetric) {
				Money usageChAmount= this.calculatePriceForUsageCharacteristic(pop, usageCh);
				totalAmount+=usageChAmount.getValue();
			}
		}
		else {
			logger.warn("No usage data fount for the metric '{}' in the TimePeriod [{}-{}]",metric,billiPeriod.getStartDateTime(),billiPeriod.getEndDateTime());
		}
		
		logger.info("Price of ProductOfferingPrice '{}' = {} {}", pop.getId(), totalAmount, pop.getPrice().getUnit());
		
		// apply price alterations
		if (ProductOfferingPriceUtils.hasRelationships(pop)) {
			Money alteretedPrice=priceAlterationCalculator.applyAlterations(pop, new Money(pop.getPrice().getUnit(), totalAmount));
											
			logger.info("Price of ProductOfferingPrice '{}' after alterations = {} {}", 
					pop.getId(), alteretedPrice.getValue(), alteretedPrice.getUnit());	
		
			return alteretedPrice;
		}
	
		return itemPrice;
		
		return null;
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

	private Money calculatePriceForUsageCharacteristic(@NonNull ProductOfferingPrice pop, @NonNull UsageCharacteristic usageCh) {
		
		logger.debug("Calculating price for Usage Characteristic with [name:'{}' value: '{}']", usageCh.getName(), usageCh.getValue());
		
		//final Price usageChPrice = new Price();
		//EuroMoney usageChAmount;
		final String usageChName = usageCh.getName();
		//final Double usageChValue = Double.parseDouble(usageCh.getValue().toString());
		final Float usageChValue = Float.parseFloat(usageCh.getValue().toString());
		
		final Quantity unitOfMeasure = pop.getUnitOfMeasure();
		//usageChAmount = new EuroMoney(((pop.getPrice().getValue() * usageChValue) / unitOfMeasure.getAmount()));
		Float usageChAmount= (pop.getPrice().getValue() * usageChValue) / unitOfMeasure.getAmount();
		//Money usageChAmount = new EuroMoney(((pop.getPrice().getValue() * usageChValue) / unitOfMeasure.getAmount()));
		logger.info("Price of UsageCharacteristic '{}' with [quantity: '{}', price: '{}' per '{} {}'] = {} euro", 
					usageChName, usageChValue,
					pop.getPrice().getValue(), unitOfMeasure.getAmount(), unitOfMeasure.getUnits(), usageChAmount);

		Money usageChMoney=new Money(pop.getPrice().getUnit(), usageChAmount);
		//usageChPrice.setDutyFreeAmount(usageChAmount.toMoney());
		//usageChPrice.setTaxIncludedAmount(null);
		
		//return usageChPrice;
		return usageChMoney;
	}

}
