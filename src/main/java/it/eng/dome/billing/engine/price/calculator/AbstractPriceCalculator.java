package it.eng.dome.billing.engine.price.calculator;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Characteristic;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.billing.engine.utils.UsageUtils;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductSpecificationCharacteristicValueUse;
import it.eng.dome.tmforum.tmf620.v4.model.Quantity;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

public abstract class AbstractPriceCalculator<T, R> implements PriceCalculator<T, R> {
	
	private final Logger logger = LoggerFactory.getLogger(AbstractPriceCalculator.class);
	
	protected ProductOfferingPrice pop;
	
	protected String priceCurrency;
	protected final String DEFAULT_CURRENCY = "EUR";
	
	@Autowired
	protected PriceAlterationCalculator priceAlterationCalculator;
	
	@Autowired
	protected ProductCatalogManagementApis productCatalogManagementApis;
	
	@Autowired
	protected TMFEntityValidator tmfEntityValidator;
	
	// Map with key=usageCharacteristic.name and value=list of UsageCharacteritic
	protected Map<String, List<UsageCharacteristic>> usageData;
	                                                               
	
	protected AbstractPriceCalculator() {
        super();
    }
	
	protected Money calculatePriceForUsageCharacteristic(@NonNull UsageCharacteristic usageCh) {
		
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
	
	/*
	 * Initialize the HashMap of UsageCharacteristic retrieving via TMForum all the usageData associated with the specified product ID and belonging to the specified TimePeriod
	 */
	protected Map<String, List<UsageCharacteristic>> inizializeUsageData(@NonNull List<Usage> usages) throws BillingEngineValidationException{

		tmfEntityValidator.validateUsages(usages);
		
		Map<String, List<UsageCharacteristic>> usageData=UsageUtils.createUsageCharacteristicDataMap(usages);
		logger.info("Created UsageDataMap with keys "+usageData.keySet().toString());
		
		return usageData;
	}
	
	protected Money calculatePriceForCharacteristic(@NotNull Characteristic ch) {
		
		logger.debug("Calculating price for Characteristic: '{}' value '{}'", ch.getName(), ch.getValue());
		final String chName = ch.getName();
		final Float chValue = Float.parseFloat(ch.getValue().toString());
		Float chAmount;
	
		if (ProductOfferingPriceUtils.isForfaitPrice(pop)) {
			chAmount = (pop.getPrice().getValue() * chValue);
			logger.info("Price of Characteristic '{}' [quantity: {}, price: '{}'] = {} {}", 
					chName, chValue, pop.getPrice().getValue(), chAmount,priceCurrency);
		} else {
			final Quantity unitOfMeasure = pop.getUnitOfMeasure();
			//chAmount = new EuroMoney(((pop.getPrice().getValue() * chValue) / unitOfMeasure.getAmount()) * chValue);
			chAmount = (pop.getPrice().getValue() * chValue) / unitOfMeasure.getAmount();
			logger.info("Price of Characteristic '{}' [quantity: {}, price: '{}' per '{} {}'] = {} {}", 
					chName, chValue,
					pop.getPrice().getValue(), unitOfMeasure.getAmount(), unitOfMeasure.getUnits(), chAmount,priceCurrency);
		}
		
		return new Money(priceCurrency, chAmount);
	}
	
	protected Characteristic findMachingCharacteristic(@NotNull List<Characteristic> characteristics) throws BillingEngineValidationException {
		
		tmfEntityValidator.validateProdSpecCharValueUseList(pop);
		ProductSpecificationCharacteristicValueUse prodSpecCharValueUse= pop.getProdSpecCharValueUse().get(0);
		
		tmfEntityValidator.validateProductSpecificationCharacteristicValueUse(prodSpecCharValueUse, pop);
		
		Characteristic matchChar=null;
		
		for(Characteristic characteristic : characteristics) {
			if(prodSpecCharValueUse.getName().equalsIgnoreCase(characteristic.getName())) {
				matchChar=new Characteristic(characteristic.getName(),characteristic.getValueType(),characteristic.getValue());
				break;
			}
		}
		
		return matchChar;
	}
	
	protected Money calculatePriceforUsageCharacteristics() throws BillingEngineValidationException {
		
		float totalAmount=0f;
		Money totalAmountMoney=new Money(priceCurrency,totalAmount);
		
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
			totalAmountMoney.setValue(totalAmount);
			
		}
		else {
			logger.warn("No usage data fount for the metric '{}'",metric);
		}

		return totalAmountMoney;
	}
	
	
	@Override
	public void setProductOfferingPrice(ProductOfferingPrice pop) {
		this.pop=pop;
		
		if(!ProductOfferingPriceUtils.isBundled(pop)) {
			if(this.pop.getPrice().getUnit()!=null && !this.pop.getPrice().getUnit().isEmpty())
				this.priceCurrency=this.pop.getPrice().getUnit();
			else
				this.priceCurrency=DEFAULT_CURRENCY;
		}
	}

}
