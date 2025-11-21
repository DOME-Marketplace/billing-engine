package it.eng.dome.billing.engine.price.calculator;

import java.util.List;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.validator.ValidationIssue;
import it.eng.dome.billing.engine.validator.ValidationIssueSeverity;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;

public class BundledPriceCalculator extends AbstractPriceCalculator{
	
	private TimePeriod billingPeriod;
	
	public BundledPriceCalculator(ProductOfferingPrice pop, Product prod, TimePeriod billingPeriod) {
		super(pop, prod);
		
		this.billingPeriod=billingPeriod;
	}

	@Override
	public Money calculatePrice() throws BillingEngineValidationException, ApiException {
		Float totalBundledPopsAmount=0f;
		
		List<ProductOfferingPrice> bundledPops = ProductOfferingPriceUtils.getBundledProductOfferingPrices(pop.getBundledPopRelationship(), productCatalogManagementApis);
		
		if (bundledPops == null || bundledPops.isEmpty()) {
			String msg=String.format("Error! Started calculation of bundled ProductOfferingPrice %s but the retrieved list of bundled POP is null or empty!", pop.getId());
			ValidationIssue issue=new ValidationIssue(msg, ValidationIssueSeverity.ERROR);
			throw new BillingEngineValidationException(issue);
		}
		
		for(ProductOfferingPrice bundledPop:bundledPops) {
			tmfEntityValidator.validateProductOfferingPrice(bundledPop);
			PriceCalculator pc= PriceCalculatorFactory.getPriceCalculatorFor(bundledPop, prod, billingPeriod);
			Money bundledPopMoney= pc.calculatePrice();
			totalBundledPopsAmount+=bundledPopMoney.getValue();
		}
		
		return new Money(priceCurrency,totalBundledPopsAmount);
	}

}
