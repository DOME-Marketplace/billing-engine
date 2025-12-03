package it.eng.dome.billing.engine.service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.brokerage.model.BillCycle;
import it.eng.dome.brokerage.model.PriceType;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.ProductPrice;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;

@Service
public class ProductPriceService {
	
	private final static Logger logger=LoggerFactory.getLogger(ProductPriceService.class);
	
	@Autowired
	private TMFEntityValidator tmfEntityValidator;
	
	@Autowired
	private ProductCatalogManagementApis productCatalogManagementApis;
	
	@Autowired
	private BillCycleService billCycleService;
	
	public Map<ProductOfferingPrice, List<BillCycle>> getPOPBillCyclesInBillingPeriod(@NotNull Product prod, @NotNull TimePeriod billingPeriod) throws BillingEngineValidationException, IllegalArgumentException, ApiException, BillingBadRequestException{
		logger.info(String.format("Retrieving the ProductOfferingPrice(s) in the ProductPrice list of Product '%s' and the billCycles that must be billed in the billingPeriod ['%s'-'%s']", prod.getId(),billingPeriod.getStartDateTime().toString(), billingPeriod.getEndDateTime().toString()));
		
		Map<ProductOfferingPrice, List<BillCycle>> popBillCyclesMap=new HashMap<ProductOfferingPrice, List<BillCycle>>();
		
		List<ProductPrice> productPrices=prod.getProductPrice();
		
		for(ProductPrice pp:productPrices) {
			tmfEntityValidator.validateProductPrice(pp,prod.getId());
			
			ProductOfferingPrice pop=ProductOfferingPriceUtils.getProductOfferingPrice(pp.getProductOfferingPrice().getId(), productCatalogManagementApis);
			
			if(pop!=null) {
				tmfEntityValidator.validateProductOfferingPrice(pop);
				if(pop.getIsBundle()) {
					List<ProductOfferingPrice> bundledPops= ProductOfferingPriceUtils.getBundledProductOfferingPrices(pop.getBundledPopRelationship(), productCatalogManagementApis);
					for(ProductOfferingPrice bundledPop: bundledPops) {
						tmfEntityValidator.validateProductOfferingPrice(bundledPop);
						popBillCyclesMap.put(bundledPop, this.getPOPBillCyclesInBillingPeriod(bundledPop, prod.getStartDate(), billingPeriod));
					}
				}else {
					popBillCyclesMap.put(pop, this.getPOPBillCyclesInBillingPeriod(pop, prod.getStartDate(), billingPeriod));
				}
			}
			
		}
		
		long popToBill = popBillCyclesMap.entrySet().stream()
		        .filter(entry -> !entry.getValue().isEmpty())
		        .count();
		
		logger.debug("Number of ProductOfferingPrice(s) to bill: {}",popToBill);
		
		return popBillCyclesMap;
	}
	
	private List<BillCycle> getPOPBillCyclesInBillingPeriod(@NotNull ProductOfferingPrice pop, @NotNull OffsetDateTime activationDate, @NotNull TimePeriod billingPeriod) throws BillingBadRequestException{
		 
		logger.debug("Get billCycles in billingPeriod [{}-{}] for POP {} priceType {}",billingPeriod.getStartDateTime(),
				billingPeriod.getEndDateTime(), pop.getId(), PriceType.fromString(pop.getPriceType()));
		
		List<BillCycle> billCycles=billCycleService.getBillCycles(pop, activationDate, billingPeriod.getEndDateTime());
		List<BillCycle> billCyclesInBillingPeriod=billCycleService.getBillCyclesInBillingPeriod(billCycles, billingPeriod);
		
		logger.debug("BillDates in billingPeriod {}",billCycleService.getBillDates(billCyclesInBillingPeriod));

		return billCyclesInBillingPeriod;
	}
	
	
}
