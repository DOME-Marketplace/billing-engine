package it.eng.dome.billing.engine.price;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.TimePeriod;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOfferingPriceRef;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
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
	
	public static OrderPrice calculatePrice(@NonNull ProductOfferingPrice pop, @NonNull ProductOrderItem orderItem, @NonNull PriceAlterationCalculator pac) throws Exception{
		OrderPrice orderPrice=new OrderPrice();
	    final Price itemPrice = new Price();
	    EuroMoney euro = new EuroMoney(pop.getPrice().getValue() * orderItem.getQuantity());
		itemPrice.setDutyFreeAmount(euro.toMoney());
		itemPrice.setTaxIncludedAmount(null);
		orderPrice.setName(pop.getName());
		orderPrice.setDescription(pop.getDescription());
		orderPrice.setPriceType(pop.getPriceType());
		if(!("one time".equalsIgnoreCase(pop.getPriceType()))&& !("one-time".equalsIgnoreCase(pop.getPriceType()))) {
			orderPrice.setRecurringChargePeriod(pop.getRecurringChargePeriodLength()+" "+pop.getRecurringChargePeriodType());
		}
		orderPrice.setPrice(itemPrice);
						
		logger.info("Price of item '{}': [quantity: {}, price: '{}'] = {} euro", 
			orderItem.getId(), orderItem.getQuantity(), pop.getPrice().getValue(), euro.getAmount());
						
			// 3) apply price alterations
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
}
