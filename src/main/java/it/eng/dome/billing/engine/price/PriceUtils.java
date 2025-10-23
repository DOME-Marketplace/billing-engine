package it.eng.dome.billing.engine.price;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import it.eng.dome.billing.engine.bill.BillUtils;
import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.brokerage.billing.utils.BillingPriceType;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductSpecificationCharacteristicValueUse;
import it.eng.dome.tmforum.tmf620.v4.model.Quantity;
import it.eng.dome.tmforum.tmf620.v4.model.TimePeriod;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOfferingPriceRef;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Characteristic;
import lombok.NonNull;

public final class PriceUtils {

	private final static Date BEGIN = (new GregorianCalendar(1900, Calendar.JANUARY, 1)).getTime();
	private final static Date END = (new GregorianCalendar(2100, Calendar.DECEMBER, 31)).getTime();
	
	private final static Logger logger = LoggerFactory.getLogger(PriceUtils.class);

	
	public static final boolean isValid(@NonNull Date when, TimePeriod period) {
		if (period == null)
			return true;
		
		final Date start;
		if (period.getStartDateTime() != null) {
			var tmp = period.getStartDateTime();
			start = (new GregorianCalendar(tmp.getYear(), tmp.getMonthValue() - 1, tmp.getDayOfMonth())).getTime();
		} else 
			start = BEGIN;
		
		final Date end;
		if (period.getEndDateTime() != null) {
			var tmp = period.getEndDateTime();
			end = (new GregorianCalendar(tmp.getYear(), tmp.getMonthValue() - 1, tmp.getDayOfMonth())).getTime();
		} else 
			end = END;
		
		return (when.compareTo(start) >= 0 && when.compareTo(end) <= 0);
	}
	
	
	public static final boolean  isActive (@NonNull ProductOfferingPrice pop) throws Exception{
		if(pop.getLifecycleStatus()==null || pop.getLifecycleStatus().isEmpty())
			throw new BillingBadRequestException(String.format("The lifecycleStatus element of the ProductOfferingPrice '%s' is not set!", pop.getId()));
		return ("active".equalsIgnoreCase(pop.getLifecycleStatus()) || "launched".equalsIgnoreCase(pop.getLifecycleStatus()));
	}
	
	
	public static boolean hasRelationships(@NonNull ProductOfferingPrice pop) {
		return !CollectionUtils.isEmpty(pop.getPopRelationship());
	}
	
	
	public static boolean hasBundledPops(@NonNull ProductOfferingPrice pop) {
		return !CollectionUtils.isEmpty(pop.getBundledPopRelationship());
	}
	
	
	public static boolean isForfaitPrice(@NonNull ProductOfferingPrice pop) {
		//return pop.getUnitOfMeasure() == null;
		return ((pop.getUnitOfMeasure()==null) || (pop.getUnitOfMeasure()!=null && "unit".equalsIgnoreCase(pop.getUnitOfMeasure().getUnits()) && pop.getUnitOfMeasure().getAmount()==1));
	}
	
	
	public static boolean hasAlterations(@NonNull OrderPrice orderPrice) {
		return !CollectionUtils.isEmpty(orderPrice.getPriceAlteration());
	}
	
	
	public static float getAlteredDutyFreePrice(@NonNull OrderPrice orderPrice) {
		if (PriceUtils.hasAlterations(orderPrice)) {
			PriceAlteration alteredPrice = orderPrice.getPriceAlteration().get(orderPrice.getPriceAlteration().size() - 1);
			return alteredPrice.getPrice().getDutyFreeAmount().getValue();
		}
		
		return 0;
	}
	
	
	public static float getDutyFreePrice(@NonNull OrderPrice orderPrice) {
		return orderPrice.getPrice().getDutyFreeAmount().getValue();
	}
	
	public static OrderPrice calculateOrderPrice(@NonNull ProductOfferingPrice pop, @NonNull ProductOrderItem orderItem, @NonNull PriceAlterationCalculator pac) throws Exception{
		logger.info("Calculating Price for ProductOfferingPrice '{}' in ProductOrdeIem '{}'...,",pop.getId(), orderItem.getId());
		
		OrderPrice orderPrice=new OrderPrice();
	    final Price itemPrice = new Price();
	    EuroMoney euro = new EuroMoney(pop.getPrice().getValue() * orderItem.getQuantity());
		itemPrice.setDutyFreeAmount(euro.toMoney());
		itemPrice.setTaxIncludedAmount(null);
		orderPrice.setName(pop.getName());
		orderPrice.setDescription(pop.getDescription());
		String priceTypeNormalized=BillingPriceType.normalize(pop.getPriceType());
		orderPrice.setPriceType(priceTypeNormalized);
		//if(!("one time".equalsIgnoreCase(pop.getPriceType()))&& !("one-time".equalsIgnoreCase(pop.getPriceType()))) {
		if(!priceTypeNormalized.equalsIgnoreCase(BillingPriceType.ONE_TIME.getNormalizedKey())) {
			orderPrice.setRecurringChargePeriod(pop.getRecurringChargePeriodLength()+" "+pop.getRecurringChargePeriodType());
		}
		orderPrice.setPrice(itemPrice);
						
		logger.info("Price of ProductOfferingPrice '{}' in ProductOrdeIem '{}': [quantity: {}, price: '{}'] = {} euro", 
			pop.getId(), orderItem.getId(), orderItem.getQuantity(), pop.getPrice().getValue(), euro.getAmount());
						
		// apply price alterations
		if (PriceUtils.hasRelationships(pop)) {
			pac.applyAlterations(orderItem, pop, orderPrice);
							
			logger.info("Price of ProductOfferingPrice '{}' in ProductOrdeIem '{}' after alterations = {} euro", 
					pop.getId(), orderItem.getId(), PriceUtils.getAlteredDutyFreePrice(orderPrice));
		}			
		
		return orderPrice;
	}
	
	public static OrderPrice calculateOrderPriceForUsageCharacterisic(@NonNull ProductOfferingPrice pop, @NonNull ProductOrderItem orderItem, @NonNull PriceAlterationCalculator pac, List<UsageCharacteristic> usageChForMetric) throws Exception{
		OrderPrice orderPrice=new OrderPrice();
	    final Price itemPrice = new Price();
	    
	    List<Price> usagePrices=new ArrayList<Price>();
	    float amount = 0;
	    int quantity=1; 
	    
	    // If the quantity is set in the order item otherwise by default is set to 1 
	    if(orderItem.getQuantity()!=null && orderItem.getQuantity()!=0)
	    	quantity=orderItem.getQuantity();  	
		
		for(UsageCharacteristic usageCh:usageChForMetric) {
			Price usageChPrice= PriceUtils.calculatePriceForUsageCharacteristic(pop, usageCh);
			usagePrices.add(usageChPrice);
		}	
		
		for(Price price:usagePrices) {
			amount += price.getDutyFreeAmount().getValue();
		}
		
		logger.debug("Total order item amount: "+amount);
	    
	    EuroMoney euro = new EuroMoney(amount * quantity);
	    
	    logger.debug("Total order item amount for order quantity {}: {}", quantity,euro.getAmount()); 
	    
		itemPrice.setDutyFreeAmount(euro.toMoney());
		itemPrice.setTaxIncludedAmount(null);
		orderPrice.setName(pop.getName());
		orderPrice.setDescription(pop.getDescription());
		orderPrice.setPriceType(pop.getPriceType());
		
		orderPrice.setRecurringChargePeriod(pop.getRecurringChargePeriodLength()+" "+pop.getRecurringChargePeriodType());
		orderPrice.setPrice(itemPrice);
						
		logger.info("Price of item '{}': [quantity: {}, price: '{}'] = {} euro with {} simulated usage data", 
			orderItem.getId(), quantity, pop.getPrice().getValue(), euro.getAmount(), usageChForMetric.size());
						
		// apply price alterations
		if (PriceUtils.hasRelationships(pop)) {
			pac.applyAlterations(orderItem, pop, orderPrice);
							
			logger.info("Price of item '{}' after alterations = {} euro", 
					orderItem.getId(), PriceUtils.getAlteredDutyFreePrice(orderPrice));
		}			
		
		return orderPrice;
	}
	

	public static ProductOfferingPriceRef createProductOfferingPriceRef(@NonNull ProductOfferingPrice pop) {
		ProductOfferingPriceRef popRef= new ProductOfferingPriceRef();
	    popRef.setName(pop.getName());
	    popRef.setId(pop.getId());
		return popRef;
	}


	public static Price calculatePrice(@NonNull ProductOfferingPrice pop, List<Characteristic> prodCharacteristics,
			@NonNull PriceAlterationCalculator pac) throws Exception{
		Price itemPrice = new Price();
		
		if(pop.getProdSpecCharValueUse()==null || pop.getProdSpecCharValueUse().isEmpty()) {
			
			EuroMoney euro = new EuroMoney(pop.getPrice().getValue());
			itemPrice.setDutyFreeAmount(euro.toMoney());
			itemPrice.setTaxIncludedAmount(null);
			
			logger.info("Price of ProductOfferingPrice '{}' = {} euro", 
					pop.getId(), euro.getAmount());
								
			// apply price alterations
			if (PriceUtils.hasRelationships(pop)) {
				Price alteretedPrice=pac.applyAlterations(pop, itemPrice);
									
				logger.info("Price of ProductOfferingPrice '{}' after alterations = {} euro", 
						pop.getId(), alteretedPrice.getDutyFreeAmount());	
			
				return alteretedPrice;
			}
			
		}else {
			if(prodCharacteristics==null || prodCharacteristics.isEmpty())
				throw new BillingBadRequestException(String.format("Error! The Characteristics are missing in the Product to calculate the price for the ProductOfferingPrice '%s' ", pop.getId()));
			
			ProductSpecificationCharacteristicValueUse prodSpecCharValueUse= pop.getProdSpecCharValueUse().get(0);
			if(prodSpecCharValueUse.getName()==null) {
				throw new BillingBadRequestException(String.format("The name of the Characteristic is missing in the ProductOfferingPrice '%s'", pop.getId()));
			}
			Characteristic matchChar=null;
			
			for(Characteristic productCharacteristic : prodCharacteristics) {
				if(prodSpecCharValueUse.getName().equalsIgnoreCase(productCharacteristic.getName())) {
					matchChar=productCharacteristic;
					break;
				}
			}
			if(matchChar==null) {
				throw new BillingBadRequestException(String.format("Error! No matching Characteristic found for the ProductOfferingPrice '%s'", pop.getId()));
			}
			
			// calculates the base price of the Characteristic 
			itemPrice = calculatePriceForCharacteristic(pop, matchChar);
				
		    // applies price alterations
			if (PriceUtils.hasRelationships(pop)) {
				Price alteratedPrice=pac.applyAlterations(pop, itemPrice);
				
				logger.info("Price of Characteristic '{}' '{}' after alterations: {} euro", 
				matchChar.getName(), matchChar.getValue(), alteratedPrice.getDutyFreeAmount());
				
				return alteratedPrice;
			
			}
		}
		
		return itemPrice;
	}
	

	public static Price calculatePriceForPayPerUse(@NonNull ProductOfferingPrice pop, @NonNull Map<String, List<UsageCharacteristic>> usageData,
			@NonNull PriceAlterationCalculator pac) throws Exception{
		
		Price itemPrice = new Price();
		float amount=0;

		// Retrieve the metric from the unitOfMeasure of the POP
		if(pop.getUnitOfMeasure()==null)
			throw new BillingBadRequestException("The unitOfMeasure is missing in the ProductOfferingPrice but it is required for the pay-per-use price type");
		
		String metric=pop.getUnitOfMeasure().getUnits();
		logger.debug("UnitOfMeasure of POP {}: units {}, value {}",pop.getId(), pop.getUnitOfMeasure().getUnits(), pop.getUnitOfMeasure().getAmount());
		
		List<UsageCharacteristic> usageChForMetric= BillUtils.getUsageCharacteristicsForMetric(usageData, metric);
		
		if(usageChForMetric!=null && !usageChForMetric.isEmpty()) {
			logger.debug("Size of the list of UsageCharacteristic for metric {}: {}",metric,usageChForMetric.size());
			
			for(UsageCharacteristic usageCh:usageChForMetric) {
				Price usageChPrice= PriceUtils.calculatePriceForUsageCharacteristic(pop, usageCh);
				amount+=usageChPrice.getDutyFreeAmount().getValue();
			}
		}
		else {
			logger.warn("No usage data fount for the metric '{}' in the TimePeriod [{}-{}]",metric);
		}
		
		EuroMoney euro = new EuroMoney(amount);
		itemPrice.setDutyFreeAmount(euro.toMoney());
		itemPrice.setTaxIncludedAmount(null);
		
		logger.info("Price of ProductOfferingPrice '{}' = {} euro", pop.getId(), euro.getAmount());
		
		// apply price alterations
		if (PriceUtils.hasRelationships(pop)) {
			Price alteretedPrice=pac.applyAlterations(pop, itemPrice);
											
			logger.info("Price of ProductOfferingPrice '{}' after alterations = {} euro", 
					pop.getId(), alteretedPrice.getDutyFreeAmount());	
		
			return alteretedPrice;
		}
	
		return itemPrice;
	}
	
	protected static Price calculatePriceForCharacteristic(ProductOfferingPrice pop, Characteristic ch) {
		
		logger.debug("Calculating price for Characteristic: '{}' value '{}'", ch.getName(), ch.getValue());
		
		final Price chPrice = new Price();
		EuroMoney chAmount;
		final String chName = ch.getName();
		final Double chValue = Double.parseDouble(ch.getValue().toString());

		if (PriceUtils.isForfaitPrice(pop)) {
			chAmount = new EuroMoney(pop.getPrice().getValue() * chValue);
			logger.info("Price of Characteristic '{}' [quantity: {}, price: '{}'] = {} euro", 
					chName, chValue, pop.getPrice().getValue(), chAmount.getAmount());
		} else {
			final Quantity unitOfMeasure = pop.getUnitOfMeasure();
			chAmount = new EuroMoney(((pop.getPrice().getValue() * chValue) / unitOfMeasure.getAmount()) * chValue);
			logger.info("Price of Characteristic '{}' [quantity: {}, price: '{}' per '{} {}'] = {} euro", 
					chName, chValue,
					pop.getPrice().getValue(), unitOfMeasure.getAmount(), unitOfMeasure.getUnits(), chAmount.getAmount());
		}
		
		chPrice.setDutyFreeAmount(chAmount.toMoney());
		chPrice.setTaxIncludedAmount(null);
		
		return chPrice;
	}
	
	public static Price calculatePriceForUsageCharacteristic(ProductOfferingPrice pop, UsageCharacteristic usageCh) {
		
		logger.debug("Calculating price for Usage Characteristic with [name:'{}' value: '{}']", usageCh.getName(), usageCh.getValue());
		
		final Price usageChPrice = new Price();
		EuroMoney usageChAmount;
		final String usageChName = usageCh.getName();
		final Double usageChValue = Double.parseDouble(usageCh.getValue().toString());
		
		final Quantity unitOfMeasure = pop.getUnitOfMeasure();
		usageChAmount = new EuroMoney(((pop.getPrice().getValue() * usageChValue) / unitOfMeasure.getAmount()));
		logger.info("Price of UsageCharacteristic '{}' with [quantity: '{}', price: '{}' per '{} {}'] = {} euro", 
					usageChName, usageChValue,
					pop.getPrice().getValue(), unitOfMeasure.getAmount(), unitOfMeasure.getUnits(), usageChAmount.getAmount());


		usageChPrice.setDutyFreeAmount(usageChAmount.toMoney());
		usageChPrice.setTaxIncludedAmount(null);
		
		return usageChPrice;
	}
	
	public static List<Price> calculatePriceForUsageCharacteristic(@NonNull ProductOfferingPrice pop, @NonNull List<UsageCharacteristic> usageChs){
		List<Price> prices=new ArrayList<Price>();
		
		for(UsageCharacteristic usageCh:usageChs) {
			Price usageChPrice= PriceUtils.calculatePriceForUsageCharacteristic(pop, usageCh);
			prices.add(usageChPrice);
		}
		return prices;
	}
	
}
