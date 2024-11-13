package it.eng.dome.billing.engine.service;

import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;

public interface PriceAlterationCalculator {

	PriceAlteration applyAlteration(float basicPrice);
	
}
