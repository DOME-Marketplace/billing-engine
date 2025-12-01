package it.eng.dome.billing.engine.price.calculator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;

@Component
public class PriceCalculatorFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(PriceCalculatorFactory.class);
	
	@Autowired
    private ApplicationContext ctx;
	
	public PriceCalculator<Product,Money> getPriceCalculatorForProduct(@NotNull ProductOfferingPrice pop, @NotNull TimePeriod billingPeriod) throws BillingEngineValidationException {
		logger.debug("*************** Price Calculator FACTORY for Product **************");
        PriceCalculator<Product, Money> pc=null;
        
        if(ProductOfferingPriceUtils.isBundled(pop)) {
        	pc=getBundledPriceCalculator(pop,billingPeriod);
        }else {
        	if(ProductOfferingPriceUtils.isPriceTypeUsage(pop)) {
    			pc=getUsagePriceCalculator(pop,billingPeriod);
    		}else {
    			if(ProductOfferingPriceUtils.hasProdSpecCharValueUses(pop)) {
    				pc=getCharacteristicPriceCalculator(pop);
    			}else {
    				pc=getBasePriceCalculator(pop);
    			}
    		}
        }
        
        pc.setProductOfferingPrice(pop);
		return pc;
	}
	
	public PriceCalculator<ProductOrderItem,List<OrderPrice>> getPriceCalculatorForProductOrderItem(@NotNull ProductOfferingPrice pop, @NotNull List<Usage> usages) throws BillingEngineValidationException {
		logger.debug("*************** Price Calculator FACTORY for ProductOrderItem **************");
        PriceCalculator<ProductOrderItem,List<OrderPrice>> pc=null;
        
        if(ProductOfferingPriceUtils.isBundled(pop)) {
        	pc=getBundledPriceCalculatorForProductOrderItem(pop,usages);
        }else {
        	if(ProductOfferingPriceUtils.isPriceTypeUsage(pop)) {
    			pc=getUsagePriceCalculatorForProductOrderItem(pop,usages);
    		}else {
    			if(ProductOfferingPriceUtils.hasProdSpecCharValueUses(pop)) {
    				pc=getCharacteristicPriceCalculatorForProductOrderItem(pop);
    			}else {
    				pc=getBasePriceCalculatorForProductOrderItem(pop);
    			}
    		}
        }
        pc.setProductOfferingPrice(pop);
		return pc;
	}

	private PriceCalculator<Product,Money> getUsagePriceCalculator(@NotNull ProductOfferingPrice pop, @NotNull TimePeriod billingPeriod) throws IllegalArgumentException{
		logger.debug("Creating UsagePriceCalculator for POP '{}'", pop.getId());
		if(billingPeriod==null)
			throw new IllegalArgumentException(String.format("Error getting UsagePriceCalculator: the POP '{}' with priceType Usage requires a not null billingPeriod to get Usage data", pop.getId()));
		UsagePriceCalculator priceCalculator=ctx.getBean(UsagePriceCalculator.class);
		priceCalculator.setBillingPeriod(billingPeriod);
		return priceCalculator;
	}
	
	private PriceCalculator<Product,Money> getBasePriceCalculator(@NotNull ProductOfferingPrice pop) {
		logger.debug("Creating BasePriceCalculator for POP '{}'", pop.getId()); 
		BasePriceCalculator priceCalculator=ctx.getBean(BasePriceCalculator.class);
		return priceCalculator;
	}
	
	private PriceCalculator<Product,Money> getCharacteristicPriceCalculator(@NotNull ProductOfferingPrice pop) throws BillingEngineValidationException {
		logger.debug("Creating CharacteristicPriceCalculator for POP '{}'", pop.getId());
		CharacteristicPriceCalculator priceCalculator=ctx.getBean(CharacteristicPriceCalculator.class);
		return priceCalculator;
	}
	
	private PriceCalculator<Product,Money> getBundledPriceCalculator(@NotNull ProductOfferingPrice pop, @NotNull TimePeriod billingPeriod) {
		logger.debug("Creating BundledPriceCalculator for POP '{}'", pop.getId());
		BundledPriceCalculator priceCalculator=ctx.getBean(BundledPriceCalculator.class);
		priceCalculator.setBillingPeriod(billingPeriod);
		return priceCalculator;
	}
	
	private PriceCalculator<ProductOrderItem,List<OrderPrice>> getUsagePriceCalculatorForProductOrderItem(@NotNull ProductOfferingPrice pop, @NotNull List<Usage> usages) throws IllegalArgumentException{
		logger.debug("Creating UsagePreviewPriceCalculator for POP '{}'", pop.getId());
		UsagePreviewPriceCalculator priceCalculator=ctx.getBean(UsagePreviewPriceCalculator.class);
		priceCalculator.setUsages(usages);
		return priceCalculator;
	}
	
	private PriceCalculator<ProductOrderItem,List<OrderPrice>>getBasePriceCalculatorForProductOrderItem(@NotNull ProductOfferingPrice pop) {
		logger.debug("Creating BasePreviewPriceCalculator for POP '{}'", pop.getId());
		BasePreviewPriceCalculator priceCalculator=ctx.getBean(BasePreviewPriceCalculator.class);
		return priceCalculator;
	}
	
	private PriceCalculator<ProductOrderItem,List<OrderPrice>> getCharacteristicPriceCalculatorForProductOrderItem(@NotNull ProductOfferingPrice pop) throws BillingEngineValidationException {
		logger.debug("Creating CharacteristicPreviewPriceCalculator for POP '{}'", pop.getId());
		CharacteristicPreviewPriceCalculator priceCalculator=ctx.getBean(CharacteristicPreviewPriceCalculator.class);
		return priceCalculator;
	}
	
	private PriceCalculator<ProductOrderItem,List<OrderPrice>> getBundledPriceCalculatorForProductOrderItem(@NotNull ProductOfferingPrice pop, @NotNull List<Usage> usages) {
		logger.debug("Creating BundledPreviewPriceCalculator for POP '{}'", pop.getId());
		BundledPreviewPriceCalculator priceCalculator=ctx.getBean(BundledPreviewPriceCalculator.class);
		priceCalculator.setUsages(usages);
		return priceCalculator;
	}


}
