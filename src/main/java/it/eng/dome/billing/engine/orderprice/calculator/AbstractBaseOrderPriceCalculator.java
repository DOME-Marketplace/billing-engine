package it.eng.dome.billing.engine.orderprice.calculator;

import org.springframework.beans.factory.annotation.Autowired;

import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;

public abstract class AbstractBaseOrderPriceCalculator implements OrderPriceCalculator{
	
	protected ProductOfferingPrice pop;
	protected ProductOrderItem productOrderItem;
	
	protected final String priceCurrency;
	protected final String DEFAULT_CURRENCY = "EUR";
	
	@Autowired
	protected PriceAlterationCalculator priceAlterationCalculator;
	
	@Autowired
	protected ProductCatalogManagementApis productCatalogManagementApis; 
	
	@Autowired
	protected TMFEntityValidator tmfEntityValidator;

	public AbstractBaseOrderPriceCalculator(ProductOfferingPrice pop, ProductOrderItem productOrderItem) {
		super();
		this.pop = pop;
		this.productOrderItem=productOrderItem;
		
		if(this.pop.getPrice().getUnit()!=null && !this.pop.getPrice().getUnit().isEmpty())
			this.priceCurrency=this.pop.getPrice().getUnit();
		else
			this.priceCurrency=DEFAULT_CURRENCY;
	}

	public ProductOfferingPrice getPop() {
		return pop;
	}
	

}
