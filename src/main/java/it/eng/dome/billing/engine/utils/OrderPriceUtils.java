package it.eng.dome.billing.engine.utils;

import java.math.BigDecimal;

import org.springframework.util.CollectionUtils;

import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.brokerage.model.PriceType;
import it.eng.dome.brokerage.model.RecurringChargePeriod;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

public class OrderPriceUtils {
	
	/**
	 * Checks if the {@link OrderPrice} has {@link PriceAlteration}
	 * @param orderPrice the {@link OrderPrice} to check
	 * @return true if the OrderPrice has PriceAlteration, false otherwise
	 */
	public static boolean hasAlterations(@NonNull OrderPrice orderPrice) {
		return !CollectionUtils.isEmpty(orderPrice.getPriceAlteration());
	}
	
	/**
	 * Returns the dutyFreeAmount of the {@link OrderPrice} after the application of all the {@link PriceAlteration} of the OrderPrice 
	 * @param orderPrice the {@link OrderPrice} with price alterations
	 * @return the dutyFreeAmount of the {@link OrderPrice} after the application of all the {@link PriceAlteration} 
	 */
	public static float getAlteredDutyFreePrice(@NonNull OrderPrice orderPrice) {
		BigDecimal totalAlteratedPrice=new BigDecimal(String.valueOf(orderPrice.getPrice().getDutyFreeAmount().getValue()));
		
		if (OrderPriceUtils.hasAlterations(orderPrice)) {
			for(PriceAlteration pa:orderPrice.getPriceAlteration()) {
				BigDecimal alteratedPrice=new BigDecimal(String.valueOf(pa.getPrice().getDutyFreeAmount().getValue()));
				totalAlteratedPrice=totalAlteratedPrice.add(alteratedPrice);
			}
		}
		
		return totalAlteratedPrice.floatValue();
	}
	
	public static Money applyQuantity(@NonNull Money money, @NonNull Integer quantity) {
		
		if(quantity>0) {
			Float updatedValue=money.getValue()*quantity;
			money.setValue(updatedValue);
		}
		
		return money;
	}
	
	public static PriceType getPriceType(@NonNull OrderPrice orderPrice) {
		String priceType=orderPrice.getPriceType();
		
		if(priceType!=null && !priceType.isEmpty())
			return PriceType.fromString(priceType);
		else {
			throw new IllegalArgumentException(String.format("Error in OrderPrice: the priceType is null"));
		}
	}
	
	public static RecurringChargePeriod getRecurrigChargePeriod(@NotNull OrderPrice orderPrice) {
		String recurringChargePeriod=orderPrice.getRecurringChargePeriod();
		
		if(recurringChargePeriod!=null && !recurringChargePeriod.isEmpty()) {
			return RecurringChargePeriod.parse(recurringChargePeriod);
		}else {
			throw new IllegalArgumentException(String.format("Error in OrderPrice: the recurringChargePeriod is null"));
		}
	}
	
	public static float getDutyFreePrice(@NonNull OrderPrice orderPrice) {
		return orderPrice.getPrice().getDutyFreeAmount().getValue();
	}
	
	public static String getCurrency(@NonNull OrderPrice orderPrice) {
		return orderPrice.getPrice().getDutyFreeAmount().getUnit();
	}

}
