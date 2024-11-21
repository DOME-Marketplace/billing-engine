package it.eng.dome.billing.engine.price;

import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.Quantity;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;

public final class PriceUtils {

	public static final boolean  isActive (ProductOfferingPrice pop) {
		// "launched"
		return "active".equalsIgnoreCase(pop.getLifecycleStatus());
	}
	
	
	public static boolean hasRelationships(ProductOfferingPrice pop) {
		return (pop.getPopRelationship() != null && pop.getPopRelationship().size() > 0);
	}
	
	
	public static boolean hasBundledPops(ProductOfferingPrice pop) {
		return (pop.getBundledPopRelationship() != null && pop.getBundledPopRelationship().size() > 0);
	}
	
	public static boolean asFixedPrice(ProductOfferingPrice pop) {
		final Quantity unit = pop.getUnitOfMeasure();
		return (unit == null || (unit.getAmount() == 1F && "unit".equalsIgnoreCase(unit.getUnits())));
	}
	
	
	public static boolean hasAlterations(OrderPrice orderPrice) {
		return (orderPrice.getPriceAlteration() != null && orderPrice.getPriceAlteration().size() > 0);
	}
	
	
	public static float getAlteredDutyFreePrice(OrderPrice orderPrice) {
		if (PriceUtils.hasAlterations(orderPrice)) {
			PriceAlteration alteredPrice = orderPrice.getPriceAlteration().get(orderPrice.getPriceAlteration().size() - 1);
			return alteredPrice.getPrice().getDutyFreeAmount().getValue();
		}
		
		return 0;
	}
	
	
	public static float getDutyFreePrice(OrderPrice orderPrice) {
		return orderPrice.getPrice().getDutyFreeAmount().getValue();
	}
}
