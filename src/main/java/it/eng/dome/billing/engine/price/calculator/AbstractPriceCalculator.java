package it.eng.dome.billing.engine.price.calculator;

import java.math.BigDecimal;
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
import it.eng.dome.tmforum.tmf620.v4.model.CharacteristicValueSpecification;
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
		
		logger.debug("Calculating price for Characteristic with name '{}' value '{}' and valueType '{}'", ch.getName(), ch.getValue(), ch.getValueType());
		final String chName = ch.getName();
		final String chValueType= ch.getValueType();
		Float chValue;
		Float chAmount;
		
		//final Float chValue = Float.parseFloat(ch.getValue().toString());
		//Float chAmount;
		
		// valueType of the characteristic is "string"
		if ("string".equalsIgnoreCase(chValueType)) {
			chAmount = (pop.getPrice().getValue());
			logger.info("Price of Characteristic '{}' [valueType: {}, price: '{}'] = {} {}", 
					chName, chValueType, pop.getPrice().getValue(), chAmount,priceCurrency);
		
		// valueType of the characteristic is "number"	
		}else if ("number".equalsIgnoreCase(chValueType)){
			chValue = Float.parseFloat(ch.getValue().toString());
		
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
		} else {
		    throw new IllegalArgumentException(
		            "Unsupported valueType: " + chValueType);
		}
		
		return new Money(priceCurrency, chAmount);
	}
	
	/*protected Characteristic findMachingCharacteristic(@NotNull List<Characteristic> characteristics) throws BillingEngineValidationException {
		
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
	}*/
	
	protected Characteristic findMachingCharacteristic(@NotNull List<Characteristic> characteristics) throws BillingEngineValidationException {
		logger.debug("Find matching characteristic...");
		List<ProductSpecificationCharacteristicValueUse> prodSpecCharValueUses=pop.getProdSpecCharValueUse();
		
		if(prodSpecCharValueUses==null || prodSpecCharValueUses.isEmpty()) {
			return null;
		}
		
		for(Characteristic characteristic : characteristics) {
			
			for (ProductSpecificationCharacteristicValueUse prodSpecCharValueUse : prodSpecCharValueUses) {
				
				// Match name (case insensitive)
	            if (!characteristic.getName().equalsIgnoreCase(prodSpecCharValueUse.getName())) {
	                continue;
	            }

				if ("number".equalsIgnoreCase(characteristic.getValueType())) {
					logger.debug("Matching characteristic with name '{}' and valueType '{}'",characteristic.getName(),characteristic.getValueType());
					return characteristic;
		        }
				
				// Characteristic.valueType = string → match also on value 
	            if ("string".equalsIgnoreCase(characteristic.getValueType()) && prodSpecCharValueUse.getProductSpecCharacteristicValue() != null) {
                    for (CharacteristicValueSpecification spec : prodSpecCharValueUse.getProductSpecCharacteristicValue()) {
                    	if (valuesMatch(spec.getValue(), characteristic.getValue())) {
                    		logger.debug("Matching characteristic with name '{}' valueType '{}' and value '{}'",characteristic.getName(),characteristic.getValueType(), characteristic.getValue().toString());
                    		return characteristic;
		                }
		            }
		        }
			}
		}
		
		return null;
	}
	
	private boolean valuesMatch(Object v1, Object v2) {

	    if (v1 == null || v2 == null) {
	        return false;
	    }

	    if (v1 instanceof Number && v2 instanceof Number) {
	        return new BigDecimal(v1.toString())
	                .compareTo(new BigDecimal(v2.toString())) == 0;
	    }

	    return v1.toString().equals(v2.toString());
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
