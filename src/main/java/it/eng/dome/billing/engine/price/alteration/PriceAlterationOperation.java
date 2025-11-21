package it.eng.dome.billing.engine.price.alteration;

import java.math.BigDecimal;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;

@FunctionalInterface
public interface PriceAlterationOperation {

	//PriceAlteration applyAlteration(float basicPrice, ProductOfferingPrice alterationPOP);
	
	/**
	 * Calculates the alteration amount that must be applied to the base price. The returned alteration could be a positive or a negative number, according to the type of the alteration.
	 * For instance, in case of a discount alteration, the returned alteration amount will be a negative number.
	 * 
	 * @param basicPrice {@link BigDecimal} representing the base price to which the alteration must be calculated
	 * @param alterationPOP The {@link ProductOfferingPrice} representing a price alteration that must be applied to the base price
	 * @return a {@link BigDecimal} representing the amount of the alteration that must be applied to the base price. 
	 */
	BigDecimal applyAlteration(BigDecimal basicPrice, ProductOfferingPrice alterationPOP) throws BillingEngineValidationException;
	
}
