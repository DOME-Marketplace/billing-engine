package it.eng.dome.billing.engine.price;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.Quantity;
import it.eng.dome.tmforum.tmf620.v4.model.TimePeriod;
import it.eng.dome.tmforum.tmf622.v4.model.Characteristic;

public class PriceMatcher {
	private final static Date BEGIN = (new GregorianCalendar(1900, Calendar.JANUARY, 1)).getTime();
	private final static Date END = (new GregorianCalendar(2100, Calendar.DECEMBER, 31)).getTime();
	
	private final List<CharacteristicPop> characteristicPopList = new ArrayList<CharacteristicPop>();
    private final Logger logger = LoggerFactory.getLogger(PriceMatcher.class);

	public void initialize(List<ProductOfferingPrice> pops) {
		for (var pop : pops) {
			addPrice(pop);
		}
	}
	
	public void addPrice(ProductOfferingPrice pop) {
		if (CollectionUtils.isEmpty(pop.getProdSpecCharValueUse())) {
			logger.warn("Skipping POP with id: {}, because it does not contains Characteristics.", pop.getId());
			return;
		}
		
		for (var popChar : pop.getProdSpecCharValueUse()) {
			for (var popCharValue : popChar.getProductSpecCharacteristicValue()) {
				CharacteristicPop cp = new CharacteristicPop(pop);
				
				cp.charName = popChar.getName();
				cp.charValueType = popChar.getValueType();
				cp.setCharValidPeriod(popChar.getValidFor());
				
				cp.charValueType = popCharValue.getValueType();
				cp.charValueUOM = popCharValue.getUnitOfMeasure();				
				cp.charValueValue = popCharValue.getValue();
				cp.charValueFrom = popCharValue.getValueFrom();
				cp.charValueTo = popCharValue.getValueTo();
				
				characteristicPopList.add(cp);
			}
		}
		
	}
	
	// TODO: must consider also the unit for fixed-price or dynamic-price
	public ProductOfferingPrice match (Characteristic productCharacteristic, Date when) {
		// Filters all valid pops having a price for the passed characteristic
		final String chName = productCharacteristic.getName();
				
		List<CharacteristicPop> filteredPopList = characteristicPopList.
				stream()
				.filter(cp -> (cp.charName.equalsIgnoreCase(chName) && cp.isActive() && cp.isValidOn(when)))
				.toList();
		
		logger.debug("Found {} candidate POP(s) for Characteristic: {}", filteredPopList.size(), chName);
		
		// Checks for a matching pop
		List<CharacteristicPop> matchingPopList = new ArrayList<CharacteristicPop>();
		for (CharacteristicPop cp : filteredPopList) {
			if (cp.perfectMatch(productCharacteristic)) {
				// perfect match is immediately returned
				return cp.pop;
			}
			
			if (cp.match(productCharacteristic)) {
				// normal match is added to result list
				matchingPopList.add(cp);
			}
		}

		if (matchingPopList.size() > 1) {
			throw new IllegalStateException(
					String.format("More than one POP match with Characteristic: '%s' with value: '%s' ", 
							chName, 
							productCharacteristic.getValue().toString()));
		}
		
		if (matchingPopList.size() == 1)
			return matchingPopList.get(0).pop;
		
		return null;
	}
	
	
	/*
	 * This class represents the price of a Characteristic in a specific period
	 * and for a specific value (or range of value) of the Characteristic.
	 * 
	 * Examples:
	 * 
	 */
	@SuppressWarnings("unused")
	private class CharacteristicPop {
		// Price
		ProductOfferingPrice pop;
		Quantity popUOM;
		Date popValidFrom;
		Date popValidTo;
		
		// Characteristic
		String charName;
		String charValueType;
		Date charValidFrom;
		Date charValidTo;
		
		// Characteristic Value
		Integer charValueFrom;
		Integer charValueTo;
		Object charValueValue;
		String charValueValueType;
		String charValueUOM;
		
		CharacteristicPop(ProductOfferingPrice pop) {
			this.pop = pop;
			this.popUOM = pop.getUnitOfMeasure();
			popValidFrom = BEGIN;
			popValidTo = END;
			
			final TimePeriod popValidFor = pop.getValidFor();
			if (popValidFor != null) {
				if (popValidFor.getStartDateTime() != null) {
					var start = popValidFor.getStartDateTime();
					popValidFrom = (new GregorianCalendar(start.getYear(), start.getMonthValue() - 1, start.getDayOfMonth())).getTime();
				}
				
				if (popValidFor.getEndDateTime() != null) {
					var end = popValidFor.getEndDateTime();
					popValidTo = (new GregorianCalendar(end.getYear(), end.getMonthValue() - 1, end.getDayOfMonth())).getTime();
				}
			}
		}
		
		boolean isValidOn(Date when) {
			return (when.compareTo(popValidFrom) >= 0 && when.compareTo(popValidTo) <= 0);
		}
		

		boolean isActive() {
			return PriceUtils.isActive(pop);
		}
		
		
		void setCharValidPeriod (TimePeriod timePeriod) {
			charValidFrom = BEGIN;
			charValidTo = END;
			
			if (timePeriod != null) {
				if (timePeriod.getStartDateTime() != null) {
					var start = timePeriod.getStartDateTime();
					charValidFrom = (new GregorianCalendar(start.getYear(), start.getMonthValue() - 1, start.getDayOfMonth())).getTime();
				}
				
				if (timePeriod.getEndDateTime() != null) {
					var end = timePeriod.getEndDateTime();
					charValidTo = (new GregorianCalendar(end.getYear(), end.getMonthValue() - 1, end.getDayOfMonth())).getTime();
				}
			} 
		}
		
		
		boolean hasRange() {
			return (charValueFrom != null && charValueTo != null);
		}
		
		
		boolean hasValue() {
			return charValueValue != null;
		}
	
		boolean valueMatch(Characteristic productCharacteristic) {
			// TODO: also consider type at char value level
			// TODO: must throw exception if no type is defined
			
			final String productCharactersiticValueType = productCharacteristic.getValueType();
			final Object productCharactersiticValue = productCharacteristic.getValue();

			if ("number".equalsIgnoreCase(productCharactersiticValueType)) {
				double productCharacteristicValueAsNumber = 0;
				if (productCharactersiticValue instanceof String) 
					productCharacteristicValueAsNumber = Double.parseDouble(productCharactersiticValue.toString());
				else if (productCharactersiticValue instanceof Double)
					productCharacteristicValueAsNumber = (Double)productCharactersiticValue;
				else if (productCharactersiticValue instanceof Integer)
					productCharacteristicValueAsNumber = (Integer)productCharactersiticValue;

				double priceCharacteristicValueAsNumber = 0;
				if (charValueValue instanceof String) 
					priceCharacteristicValueAsNumber = Double.parseDouble(charValueValue.toString());
				else if (charValueValue instanceof Double)
					priceCharacteristicValueAsNumber = (Double)charValueValue;
				else if (charValueValue instanceof Integer)
					priceCharacteristicValueAsNumber = (Integer)charValueValue;
				
				return productCharacteristicValueAsNumber == priceCharacteristicValueAsNumber;
			} else if ("string".equalsIgnoreCase(productCharactersiticValueType)) {
				return charValueValue.toString().equalsIgnoreCase(charValueValue.toString());
			}
			
			return false;
		}
		
		boolean perfectMatch(Characteristic productCharacteristic) {
			if (PriceUtils.isForfaitPrice(pop) && hasValue() && valueMatch(productCharacteristic)) {
				return true;
			}
			
			return false;
		}
		
		boolean match(Characteristic productCharacteristic) {
			if (!PriceUtils.isForfaitPrice(pop)) 
				return true;
			
			return false;
		}
	}

}
