package it.eng.dome.billing.engine.utils;

import java.math.BigDecimal;

import org.springframework.util.CollectionUtils;

import it.eng.dome.billing.engine.price.PriceUtils;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
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
		BigDecimal totalAlteratedPrice=new BigDecimal(String.valueOf(orderPrice.getPrice().getDutyFreeAmount()));
		
		if (PriceUtils.hasAlterations(orderPrice)) {
			for(PriceAlteration pa:orderPrice.getPriceAlteration()) {
				BigDecimal alteratedPrice=new BigDecimal(String.valueOf(pa.getPrice().getDutyFreeAmount()));
				totalAlteratedPrice.add(alteratedPrice);
			}
		}
		
		return totalAlteratedPrice.floatValue();
	}

}
