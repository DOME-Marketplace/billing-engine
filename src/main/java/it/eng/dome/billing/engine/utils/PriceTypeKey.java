package it.eng.dome.billing.engine.utils;

import java.util.Objects;

/**
 * Utility class to represent a key based on the priceType (e.g., one-time, recurring, recurring-prepaid, recurring-postpaid, pay per use), recurringChargePeriodType (e.g., month, week), recurringChargePeriodLength (e-g-, 1,2..)
 * to aggregate the OrderPrice instances
 */ 
public class PriceTypeKey {

	private final String priceType;
    private final String recurringChargePeriodType;
    private final int recurringChargePeriodLength;
    
    private int hashCode;

	public PriceTypeKey(String priceType, String recurringChargePeriodType, int recurringChargePeriodLength) {
		this.priceType = priceType;
		this.recurringChargePeriodType = recurringChargePeriodType;
		this.recurringChargePeriodLength = recurringChargePeriodLength;
		this.hashCode = Objects.hash(priceType, priceType,recurringChargePeriodLength);
	}
   
    public String getPriceType() {
		return priceType;
	}

	public String getRecurringChargePeriodType() {
		return recurringChargePeriodType;
	}

	public int getRecurringChargePeriodLength() {
		return recurringChargePeriodLength;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PriceTypeKey that = (PriceTypeKey) o;
        return priceType.equals(that.priceType) && recurringChargePeriodType.equals(that.recurringChargePeriodType) && recurringChargePeriodLength==that.recurringChargePeriodLength;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
    
    @Override
	public String toString() {
		// TODO Auto-generated method stub
		return "[priceType: "+this.priceType+" recurringChargePeriodType: "+this.recurringChargePeriodType+" recurringChargePeriodLength: "+this.recurringChargePeriodLength+"]";
	}

}
