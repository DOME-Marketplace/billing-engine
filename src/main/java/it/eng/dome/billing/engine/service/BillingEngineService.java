package it.eng.dome.billing.engine.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.dome.billing.engine.config.AppProperties;
import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.price.calculator.PriceCalculator;
import it.eng.dome.billing.engine.price.calculator.PriceCalculatorFactory;
import it.eng.dome.billing.engine.utils.TMForumEntityUtils;
import it.eng.dome.billing.engine.utils.TmfConverter;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.model.BillCycle;
import it.eng.dome.brokerage.model.BillCycleSpecification;
import it.eng.dome.brokerage.model.Invoice;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;

@Service
public class BillingEngineService {
	
private final static Logger logger=LoggerFactory.getLogger(BillingEngineService.class);
	
	@Autowired
	private TMFEntityValidator tmfEntityValidator;
	
	@Autowired
	private ProductPriceService productPriceService;
	
	@Autowired
	private BillCycleService billCycleService;
	
	@Autowired
	private PriceCalculatorFactory priceCalculatorFactory;
	
	private final AppProperties appProperties;
	
	public BillingEngineService(AppProperties appProperties) {
		this.appProperties = appProperties;
	}
	
	public List<Invoice> calculateBill(@NotNull Product product, @NotNull TimePeriod billingPeriod) throws BillingEngineValidationException, ApiException, IllegalArgumentException, BillingBadRequestException {
		
		logger.info("Starting calculation of the bill for Product '{}' and billingPeriod '{}'-'{}'...", product.getId(), billingPeriod.getStartDateTime(), billingPeriod.getEndDateTime());
		
		List<Invoice> invoices=new ArrayList<Invoice>();
		
		List<AppliedCustomerBillingRate> acbrs=new ArrayList<AppliedCustomerBillingRate>();
		
		tmfEntityValidator.validateProduct(product);
		
		List<ProductOfferingPrice> popsToBill=productPriceService.getProductOfferingPriceToBill(product, billingPeriod);
		
		tmfEntityValidator.validatePOPsCurrency(popsToBill, product);
		
		for(ProductOfferingPrice pop: popsToBill) {
			acbrs.addAll(generateACBR(pop,product,billingPeriod));
		}
		
		if(!acbrs.isEmpty()) {
			CustomerBill cb=generateCB(acbrs, product, billingPeriod);
		
			Invoice invoice=new Invoice(cb, acbrs);
			invoices.add(invoice);
		}
		
		return invoices;
	}
	
	


	private List<AppliedCustomerBillingRate> generateACBR(@NotNull ProductOfferingPrice pop, @NotNull Product product,
			@NotNull TimePeriod billingPeriod) throws BillingBadRequestException, BillingEngineValidationException, ApiException {
		logger.info("Generation of ACBR(s) for POP '{}' in Product '{}' and billingPeriod '{}'-'{}'",pop.getId(),product.getId(),billingPeriod.getStartDateTime(), billingPeriod.getEndDateTime());
		
		List<AppliedCustomerBillingRate> acbrs=new ArrayList<AppliedCustomerBillingRate>();
		
		List<BillCycle> billCycles=billCycleService.getBillCycles(pop, product.getStartDate(), billingPeriod.getEndDateTime());
		List<BillCycle> billCycleInBillingPeriod=billCycleService.getBillCyclesInBillingPeriod(billCycles, billingPeriod);
		
		
		for(BillCycle billCycle:billCycleInBillingPeriod) {
			tmfEntityValidator.validatePrice(pop);
			
			PriceCalculator<Product,it.eng.dome.billing.engine.model.Money> pc=priceCalculatorFactory.getPriceCalculatorForProduct(pop,billCycle.getBillingPeriod());
			
			it.eng.dome.billing.engine.model.Money taxExclutedAmount=pc.calculatePrice(product);
			
			AppliedCustomerBillingRate acbr= TMForumEntityUtils.createAppliedCustomerBillingRate
					(pop, product, billCycle, TmfConverter.convertMoneyTo678(taxExclutedAmount), appProperties.getSchema().getSchemaLocationRelatedParty());
			acbrs.add(acbr);
		}

		return acbrs;
	}
	
	private CustomerBill generateCB(@NotNull List<AppliedCustomerBillingRate> acbrs, @NotNull Product prod, @NotNull TimePeriod billingPeriod) {
		logger.info("Generation of CB for billingPeriod '{}'-'{}' with {} ACBRs",billingPeriod.getStartDateTime(), billingPeriod.getEndDateTime(), acbrs.size());
		
		BillCycleSpecification bcSpec=null;
		
		if(appProperties.getBillCycle().isBillCycleSpecEnabled())
			bcSpec=getBillCycleSpecification();
		
		CustomerBill cb=TMForumEntityUtils.createCustomerBill(acbrs, prod, billingPeriod, bcSpec); 
		
		return cb;
		
	}
	
	/*
	 * TODO: Method not implemented yet. BillCycleSpecification not supported yet.
	 */
	private static BillCycleSpecification getBillCycleSpecification() {
		throw new UnsupportedOperationException("Method not supported yet!");
	}
	
	public List<Invoice> calculateBill(@NotNull Product product, @NotNull OffsetDateTime date) throws BillingEngineValidationException, ApiException, IllegalArgumentException, BillingBadRequestException {
		
		TimePeriod billingPeriod=new TimePeriod();
		billingPeriod.setStartDateTime(date);
		billingPeriod.endDateTime(date);
		
		return this.calculateBill(product, billingPeriod);
	}
	

}
