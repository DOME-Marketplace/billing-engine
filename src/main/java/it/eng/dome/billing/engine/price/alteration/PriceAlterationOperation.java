package it.eng.dome.billing.engine.price.alteration;

import java.math.BigDecimal;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;

@FunctionalInterface
public interface PriceAlterationOperation {
	
	/**
	 * Calculates the alteration amount that must be applied to the base price. The returned alteration could be a positive or a negative number, according to the type of the alteration.
	 * For instance, in case of a discount alteration, the returned alteration amount will be a negative number.
	 * If a not null and greater than zero quantity is specified the amount of alteration is multiplied for that quantity to manage the case of price preview where the productOrderItem contains a quantity that must be applied also to the alteration amounts.
	 * 
	 * @param basicPrice A {@link BigDecimal} representing the base price to which the alteration must be calculated
	 * @param alterationPOP The {@link ProductOfferingPrice} representing a price alteration that must be applied to the base price
	 * @param quantity A {@link Float} to indicate the quantity. Null if not present
	 * @return a {@link BigDecimal} representing the amount of the alteration that must be applied to the base price. 
	 */
	BigDecimal applyAlteration(BigDecimal basicPrice, ProductOfferingPrice alterationPOP, Float quantity) throws BillingEngineValidationException;
	
}
