package it.eng.dome.billing.engine.price.calculator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.validator.ValidationIssue;
import it.eng.dome.billing.engine.validator.ValidationIssueSeverity;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import jakarta.validation.constraints.NotNull;

@Component
public class BundledPreviewPriceCalculator extends AbstractPriceCalculator<ProductOrderItem,List<OrderPrice>>{

	private List<Usage> usages;
	
	public BundledPreviewPriceCalculator() {
		super();
	}
	
	@Autowired
	PriceCalculatorFactory priceCalculatorFactory;

	@Override
	public List<OrderPrice> calculatePrice(ProductOrderItem productOrderItem) throws BillingEngineValidationException, ApiException {
		
		List<OrderPrice> orderPrices=new ArrayList<OrderPrice>();
		List<ProductOfferingPrice> bundledPops = ProductOfferingPriceUtils.getBundledProductOfferingPrices(pop.getBundledPopRelationship(), productCatalogManagementApis);
		
		if (bundledPops == null || bundledPops.isEmpty()) {
			String msg=String.format("Error! Started calculation of bundled ProductOfferingPrice %s but the retrieved list of bundled POP is null or empty!", pop.getId());
			ValidationIssue issue=new ValidationIssue(msg, ValidationIssueSeverity.ERROR);
			throw new BillingEngineValidationException(issue);
		}
		
		for(ProductOfferingPrice bundledPop:bundledPops) {
			tmfEntityValidator.validateProductOfferingPrice(bundledPop);
			PriceCalculator<ProductOrderItem,List<OrderPrice>> pc= priceCalculatorFactory.getPriceCalculatorForProductOrderItem(bundledPop, usages);
			List<OrderPrice> bundledPopMoney= pc.calculatePrice(productOrderItem);
			orderPrices.addAll(bundledPopMoney);
		}
		
		return orderPrices;
	}
	
	public void setUsages(@NotNull List<Usage> usages) {
		this.usages=usages;
	}

}
