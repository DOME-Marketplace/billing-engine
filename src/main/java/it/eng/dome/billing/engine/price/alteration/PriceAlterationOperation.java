package it.eng.dome.billing.engine.price.alteration;

import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;

@FunctionalInterface
public interface PriceAlterationOperation {

	PriceAlteration applyAlteration(float basicPrice, ProductOfferingPrice alterationPOP);
	
}
