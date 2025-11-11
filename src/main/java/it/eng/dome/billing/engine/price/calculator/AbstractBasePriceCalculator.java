package it.eng.dome.billing.engine.price.calculator;

import org.springframework.beans.factory.annotation.Autowired;

import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;

public abstract class AbstractBasePriceCalculator implements PriceCalculator{
	
	protected ProductOfferingPrice pop;
	
	protected final String priceCurrency;
	protected final String DEFAULT_CURRENCY = "EUR";
	
	@Autowired
	protected PriceAlterationCalculator priceAlterationCalculator;

	public AbstractBasePriceCalculator(ProductOfferingPrice pop) {
		super();
		this.pop = pop;
		
		if(this.pop.getPrice().getUnit()!=null && !this.pop.getPrice().getUnit().isEmpty())
			this.priceCurrency=this.pop.getPrice().getUnit();
		else
			this.priceCurrency=DEFAULT_CURRENCY;
	}

	public ProductOfferingPrice getPop() {
		return pop;
	}
	

}
