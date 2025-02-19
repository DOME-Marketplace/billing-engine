package it.eng.dome.billing.engine.utils;

import java.util.Objects;

/**
 * Utility class to represent a key based on the priceType (e.g., one-time, recurring, recurring-prepaid, recurring-postpaid, pay per use) and recurringChargePeriod (e.g., 1 month, 1 week)
 * to aggregate the OrderPrice instances
 */ 
public class PriceTypeKey {

	private final String priceType;
	private final String recurringChargePeriod;
    
    
	private int hashCode;

    public PriceTypeKey(String priceType, String recurringChargePeriod) {
		this.priceType = priceType;
		this.recurringChargePeriod= recurringChargePeriod;
		this.hashCode = Objects.hash(priceType, recurringChargePeriod);
	}
   
   
    public String getPriceType() {
		return priceType;
	}
    
    public String getRecurringChargePeriod() {
		return recurringChargePeriod;
	}
    
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PriceTypeKey that = (PriceTypeKey) o;
        return priceType.equals(that.priceType) && recurringChargePeriod.equals(that.recurringChargePeriod);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
    
    @Override
	public String toString() {
		// TODO Auto-generated method stub
		return "[priceType: "+this.priceType+" recurringChargePeriod: "+this.recurringChargePeriod+"]";
	}

}
