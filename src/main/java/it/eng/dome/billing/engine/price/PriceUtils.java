package it.eng.dome.billing.engine.price;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.springframework.util.CollectionUtils;

import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.TimePeriod;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
import lombok.NonNull;

public final class PriceUtils {

	private final static Date BEGIN = (new GregorianCalendar(1900, Calendar.JANUARY, 1)).getTime();
	private final static Date END = (new GregorianCalendar(2100, Calendar.DECEMBER, 31)).getTime();

	
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
	
	
	public static final boolean  isActive (@NonNull ProductOfferingPrice pop) {
		return ("active".equalsIgnoreCase(pop.getLifecycleStatus()) || "launched".equalsIgnoreCase(pop.getLifecycleStatus()));
	}
	
	
	public static boolean hasRelationships(@NonNull ProductOfferingPrice pop) {
		return !CollectionUtils.isEmpty(pop.getPopRelationship());
	}
	
	
	public static boolean hasBundledPops(@NonNull ProductOfferingPrice pop) {
		return !CollectionUtils.isEmpty(pop.getBundledPopRelationship());
	}
	
	
	public static boolean isForfaitPrice(@NonNull ProductOfferingPrice pop) {
		return pop.getUnitOfMeasure() == null;
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
}
