package it.eng.dome.billing.engine.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.price.calculator.PriceCalculator;
import it.eng.dome.billing.engine.price.calculator.PriceCalculatorFactory;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import jakarta.validation.constraints.NotNull;

@Service
public class ProductOrderPriceService {
	
private final static Logger logger=LoggerFactory.getLogger(ProductOrderPriceService.class);
	
	@Autowired
	private TMFEntityValidator tmfEntityValidator;
	
	@Autowired
	private ProductCatalogManagementApis productCatalogManagementApis;
	
	public List<OrderPrice> calculatePricesForProductOrderItem(@NotNull ProductOrderItem productOrderItem, @NotNull String productOrderId,  @NotNull List<Usage> usageData) throws BillingEngineValidationException, ApiException, IllegalArgumentException{
		List<OrderPrice> productOrderItemPrices=new ArrayList<OrderPrice>();
		
		tmfEntityValidator.validateProductOrderItem(productOrderItem, productOrderId);
		
		for(OrderPrice op:productOrderItem.getItemTotalPrice()) {
			tmfEntityValidator.validateOrderPrice(op, productOrderItem.getId(), productOrderId);
			
			ProductOfferingPrice orderPricePop= ProductOfferingPriceUtils.getProductOfferingPrice(op.getProductOfferingPrice().getId(), productCatalogManagementApis);
			
			tmfEntityValidator.validateProductOfferingPrice(orderPricePop); 
			
			Product orderProduct=new Product();
			orderProduct.setId(null);
			orderProduct.setProductCharacteristic(productOrderItem.getProduct().getProductCharacteristic());
			
		}
		
		return productOrderItemPrices;
		
	}

}
