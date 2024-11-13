package it.eng.dome.billing.engine.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingTerm;
import it.eng.dome.tmforum.tmf622.v4.model.Money;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;

public class DiscountAlterationCalculator implements PriceAlterationCalculator{

	private ProductOfferingPrice alterationPOP;
	
	public DiscountAlterationCalculator(ProductOfferingPrice alterationPOP) {
		super();
		this.alterationPOP = alterationPOP;
	}

	@Override
	public PriceAlteration applyAlteration(float basePrice) {
		BigDecimal alteration = new BigDecimal(basePrice * (alterationPOP.getPercentage() / 100));
		alteration = alteration.setScale(2, RoundingMode.HALF_EVEN);
		basePrice -= alteration.floatValue();
		EuroMoney euro = new EuroMoney(basePrice < 0 ? 0 : basePrice);
		
		Price price = new Price();
		price.setDutyFreeAmount((new Money()).unit(euro.getCurrency()).value(euro.getAmount()));
		
		PriceAlteration priceAlteration = new PriceAlteration();
		priceAlteration
		.description(alterationPOP.getDescription())
		.name(alterationPOP.getName())
		.priceType(alterationPOP.getPriceType())
		.setPrice(price);
		
		if (alterationPOP.getProductOfferingTerm() != null && alterationPOP.getProductOfferingTerm().size() > 0) {
			ProductOfferingTerm term = alterationPOP.getProductOfferingTerm().get(0);
			
			priceAlteration
			.applicationDuration(term.getDuration().getAmount())
			.setUnitOfMeasure(term.getDuration().getUnits());
		}
		
		return priceAlteration;
	}

}
