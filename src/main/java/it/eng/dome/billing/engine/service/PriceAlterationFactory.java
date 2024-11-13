package it.eng.dome.billing.engine.service;

import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;

public final class PriceAlterationFactory {
	
	public static PriceAlterationCalculator getPriceAlterationCalculator(ProductOfferingPrice alteration) {
		if ("discount".equalsIgnoreCase(alteration.getPriceType())) {
			return new DiscountAlterationCalculator(alteration);
		}
		
		return null;
	}
	
}
