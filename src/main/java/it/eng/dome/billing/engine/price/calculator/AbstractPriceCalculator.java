package it.eng.dome.billing.engine.price.calculator;

import org.springframework.beans.factory.annotation.Autowired;

import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf637.v4.model.Product;

public abstract class AbstractPriceCalculator extends AbstractBasePriceCalculator{

	protected Product prod;
		
	@Autowired
	protected TMFEntityValidator tmfEntityValidator;
	
	public AbstractPriceCalculator(ProductOfferingPrice pop, Product prod) {
		super(pop);
		this.prod=prod;
	}

	public Product getProd() {
		return prod;
	}
	
	

}
