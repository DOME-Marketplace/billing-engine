package it.eng.dome.billing.engine.utils;

import java.util.Objects;

import it.eng.dome.brokerage.model.PriceType;
import it.eng.dome.brokerage.model.RecurringChargePeriod;

/**
 * Utility class to represent a key based on the priceType (e.g., one-time, recurring, recurring-prepaid, recurring-postpaid, pay per use) and recurringChargePeriod (e.g., 1 month, 1 week)
 * to aggregate the OrderPrice instances
 */ 
public class PriceTypeKey {

	private final PriceType priceType;
	private final RecurringChargePeriod recurringChargePeriod;

    public PriceTypeKey(PriceType priceType, RecurringChargePeriod recurringChargePeriod) {
		this.priceType = priceType;
		this.recurringChargePeriod= recurringChargePeriod;
	}
   
    public PriceType getPriceType() {
		return priceType;
	}
    
    public RecurringChargePeriod getRecurringChargePeriod() {
		return recurringChargePeriod;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PriceTypeKey other = (PriceTypeKey) obj;
		return priceType == other.priceType && Objects.equals(recurringChargePeriod, other.recurringChargePeriod);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(priceType, recurringChargePeriod);
	}
    
	@Override
	public String toString() {
		return "PriceTypeKey [priceType=" + priceType + ", recurringChargePeriod=" + recurringChargePeriod + "]";
	}

}
