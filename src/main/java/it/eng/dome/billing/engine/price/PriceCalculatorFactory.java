package it.eng.dome.billing.engine.price;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import lombok.NonNull;

@Component(value = "priceCalculatorFactory")
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class PriceCalculatorFactory {
	
	@Autowired
    private ApplicationContext applicationContext;
	
	public PriceCalculator getPriceCalculator(@NonNull ProductOfferingPrice pop) {
		if (pop.getIsBundle() == false)
			return (PriceCalculator)applicationContext.getBean("singlePriceCalculator");
		else 
			throw new NotImplementedException("Bundled pop");
	}

}
