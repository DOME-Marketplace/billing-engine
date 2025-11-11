package it.eng.dome.billing.engine.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.dome.billing.engine.config.AppProperties;
import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.price.PriceUtils;
import it.eng.dome.billing.engine.price.calculator.PriceCalculator;
import it.eng.dome.billing.engine.price.calculator.PriceCalculatorFactory;
import it.eng.dome.billing.engine.utils.TMForumEntityUtils;
import it.eng.dome.billing.engine.utils.TmfConverter;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.billing.dto.BillingResponseDTO;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.brokerage.model.BillCycle;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOffering;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;
import it.eng.dome.tmforum.tmf678.v4.model.Money;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.NonFinal;

@Service
public class BillingEngineService {
	
private final static Logger logger=LoggerFactory.getLogger(BillingEngineService.class);
	
	@Autowired
	private TMFEntityValidator tmfEntityValidator;
	
	@Autowired
	private ProductPriceService productPriceService;
	
	//@Autowired
	//private ProductPriceService usagePriceService;
	
	@Autowired
	private BillCycleService billCycleService;
	
	private final AppProperties appProperties;
	
	
	public BillingEngineService(AppProperties appProperties) {
		this.appProperties = appProperties;
	}


	public BillingResponseDTO calculateBill(@NotNull Product product, @NotNull TimePeriod billingPeriod) throws BillingEngineValidationException, ApiException, IllegalArgumentException, BillingBadRequestException {
		
		logger.info("Starting calculation of the bill for Product '{}' and billingPeriod '{}'-'{}'...", product.getId(), billingPeriod.getStartDateTime(), billingPeriod.getEndDateTime());
		
		List<AppliedCustomerBillingRate> acbrs=new ArrayList<AppliedCustomerBillingRate>();
		
		tmfEntityValidator.validateProduct(product);
		
		List<ProductOfferingPrice> popsToBill=productPriceService.getProductOfferingPriceToBill(product, billingPeriod);
		
		for(ProductOfferingPrice pop: popsToBill) {
			acbrs=generateACBR(pop,product,billingPeriod);
		}
		
		CustomerBill cb= this.generateCB(acbrs, prod, billingPeriod);
		BillingResponseDTO billingResponseDTO=new BillingResponseDTO(cb,acbrs);
		
		return billingResponseDTO;
		
	}


	private List<AppliedCustomerBillingRate> generateACBR(@NotNull ProductOfferingPrice pop, @NotNull Product product,
			@NotNull TimePeriod billingPeriod) throws BillingBadRequestException, BillingEngineValidationException, ApiException {
		logger.info("Generation of ACBR(s) for POP '{}' in Product '{}' and billingPeriod '{}'-'{}'",pop.getId(),product.getId(),billingPeriod.getStartDateTime(), billingPeriod.getEndDateTime());
		
		List<AppliedCustomerBillingRate> acbrs=new ArrayList<AppliedCustomerBillingRate>();
		
		List<BillCycle> billCycles=billCycleService.getBillCycles(pop, product.getStartDate(), billingPeriod.getEndDateTime());
		List<BillCycle> billCycleInBillingPeriod=billCycleService.getBillCyclesInBillingPeriod(billCycles, billingPeriod);
		
		
		for(BillCycle billCycle:billCycleInBillingPeriod) {
			tmfEntityValidator.validatePrice(pop);
			
			PriceCalculator pc=PriceCalculatorFactory.getPriceCalculatorFor(pop, product, billingPeriod);
			
			it.eng.dome.billing.engine.model.Money taxExclutedAmount=pc.calculatePrice();
			
			AppliedCustomerBillingRate acbr= TMForumEntityUtils.createAppliedCustomerBillingRate
					(pop, product, billCycle, TmfConverter.convertMoneyTo678(taxExclutedAmount), appProperties.getSchemaLocationRelatedParty());
			acbrs.add(acbr);
		}

		return acbrs;
	}
	
	private CustomerBill generateCB(@NotNull List<AppliedCustomerBillingRate> acbrs, @NotNull Product prod, @NotNull TimePeriod billingPeriod) {
		logger.info("Generation of CB for billingPeriod '{}'-'{}' with {} ACBRs",billingPeriod.getStartDateTime(), billingPeriod.getEndDateTime(), acbrs.size());
		
		CustomerBill cb=new CustomerBill();
		
		ProductOffering po=new ProductOffering(); 
		
		return cb;
		
	}

}
