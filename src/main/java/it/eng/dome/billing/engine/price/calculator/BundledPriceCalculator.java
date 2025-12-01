package it.eng.dome.billing.engine.price.calculator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.validator.ValidationIssue;
import it.eng.dome.billing.engine.validator.ValidationIssueSeverity;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;

@Component
public class BundledPriceCalculator extends AbstractPriceCalculator<Product,Money>{
	
	private TimePeriod billingPeriod;
	
	public BundledPriceCalculator() {
		super();
	}
	
	@Autowired
	PriceCalculatorFactory priceCalculatorFactory;

	@Override
	public Money calculatePrice(Product prod) throws BillingEngineValidationException, ApiException {
		Float totalBundledPopsAmount=0f;
		
		List<ProductOfferingPrice> bundledPops = ProductOfferingPriceUtils.getBundledProductOfferingPrices(pop.getBundledPopRelationship(), productCatalogManagementApis);
		
		if (bundledPops == null || bundledPops.isEmpty()) {
			String msg=String.format("Error! Started calculation of bundled ProductOfferingPrice %s but the retrieved list of bundled POP is null or empty!", pop.getId());
			ValidationIssue issue=new ValidationIssue(msg, ValidationIssueSeverity.ERROR);
			throw new BillingEngineValidationException(issue);
		}
		
		for(ProductOfferingPrice bundledPop:bundledPops) {
			tmfEntityValidator.validateProductOfferingPrice(bundledPop);
			PriceCalculator<Product,Money> pc= priceCalculatorFactory.getPriceCalculatorForProduct(bundledPop, billingPeriod);
			Money bundledPopMoney= pc.calculatePrice(prod);
			totalBundledPopsAmount+=bundledPopMoney.getValue();
		}
		
		return new Money(priceCurrency,totalBundledPopsAmount);
	}
	
	public void setBillingPeriod(@NotNull TimePeriod billingPeriod) {
		this.billingPeriod=billingPeriod;
	}

}
