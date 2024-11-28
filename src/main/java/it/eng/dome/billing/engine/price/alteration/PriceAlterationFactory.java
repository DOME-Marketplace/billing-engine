package it.eng.dome.billing.engine.price.alteration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;

@Component(value = "priceAlterationFactory")
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public final class PriceAlterationFactory {
	
	@Autowired
	private ApplicationContext applicationContext;
	
	public PriceAlterationOperation getPriceAlterationCalculator(ProductOfferingPrice alteration) {
		if ("discount".equalsIgnoreCase(alteration.getPriceType())) {
			return (PriceAlterationOperation)applicationContext.getBean("discountAlterationOperation");
		}
		
		throw new IllegalArgumentException("Alteration '" + alteration.getPriceType() + "' not supported.");
	}
	
}
