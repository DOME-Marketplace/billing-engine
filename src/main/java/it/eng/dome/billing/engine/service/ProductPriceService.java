package it.eng.dome.billing.engine.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.price.PriceUtils;
import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.billing.engine.tmf.EuroMoney;
import it.eng.dome.billing.engine.utils.TmfConverter;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.brokerage.model.BillCycle;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.Money;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductSpecificationCharacteristicValueUse;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf637.v4.model.Characteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.ProductPrice;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

@Service
public class ProductPriceService {
	
	private final static Logger logger=LoggerFactory.getLogger(ProductPriceService.class);
	
	@Autowired
	private TMFEntityValidator tmfEntityValidator;
	
	@Autowired
	private ProductCatalogManagementApis productCatalogManagementApis;
	
	@Autowired
	private BillCycleService billCycleService;
	
	@Autowired
	private PriceAlterationCalculator priceAlterationCalculator;
	
	private static final String DEFAULT_CURRENCY = "EUR";
	
	
	public List<ProductOfferingPrice> getProductOfferingPriceToBill(@NotNull Product prod, @NotNull TimePeriod billingPeriod) throws BillingEngineValidationException, IllegalArgumentException, ApiException, BillingBadRequestException{
		logger.info(String.format("Retrieving the ProductOfferingPrice(s) in the ProductPrice list of Product '%s', that must be billed in the billingPeriod ['%s'-'%s']", prod.getId(),billingPeriod.getStartDateTime().toString(), billingPeriod.getEndDateTime().toString()));
		
		List<ProductOfferingPrice> popsToBill= new ArrayList<ProductOfferingPrice>();
		
		List<ProductPrice> productPrices=prod.getProductPrice();
		
		for(ProductPrice pp:productPrices) {
			tmfEntityValidator.validateProductPrice(pp);
			
			ProductOfferingPrice pop=ProductOfferingPriceUtils.getProductOfferingPrice(pp, productCatalogManagementApis);
			
			if(pop!=null) {
				tmfEntityValidator.validateProductOfferingPrice(pop);
				if(pop.getIsBundle()) {
					List<ProductOfferingPrice> bundledPops= ProductOfferingPriceUtils.getProductOfferingPrices(pop.getBundledPopRelationship(), productCatalogManagementApis);
					for(ProductOfferingPrice bundledPop: bundledPops) {
						tmfEntityValidator.validateProductOfferingPrice(bundledPop);
						if(popToBill(bundledPop, prod, billingPeriod))
							popsToBill.add(bundledPop);
					}
				}else {
					if(popToBill(pop, prod, billingPeriod))
						popsToBill.add(pop);
				}
			}
			
		}
		
		logger.info("Number of ProductOfferingPrice(s) to bill: {}",popsToBill.size());
		
		return popsToBill;
	}
	
	private boolean popToBill(@NotNull ProductOfferingPrice pop, @NotNull Product prod, @NotNull TimePeriod billingPeriod) throws BillingBadRequestException {
		
		List<OffsetDateTime> billDates=billCycleService.calculateBillDates(pop, prod, billingPeriod.getEndDateTime());
		for(OffsetDateTime billDate: billDates) {
			if(billCycleService.isBillDateWithinBillingPeriod(billDate, billingPeriod))
				return true;
		}
		
		return false;
	}

	public Price calculatePrice(@NonNull ProductOfferingPrice pop, List<Characteristic> prodCharacteristics) {
		Price itemPrice = new Price();
		Money money=null;
		
		if(pop.getProdSpecCharValueUse()==null || pop.getProdSpecCharValueUse().isEmpty()) {
			
			money=getMoney(pop);
			logger.info("Price of ProductOfferingPrice '{}' = {} euro", 
						pop.getId(), money.getValue());
			
			itemPrice.setDutyFreeAmount(TmfConverter.convertMoneyTo622(money));
								
			// apply price alterations
			if (ProductOfferingPriceUtils.hasRelationships(pop)) {
				
				Price alteretedPrice=priceAlterationCalculator.applyAlterations(pop, itemPrice);
									
				logger.info("Price of ProductOfferingPrice '{}' after alterations = {} euro", 
						pop.getId(), alteretedPrice.getDutyFreeAmount());	
			
				return alteretedPrice;
			}
			
		}else {
			if(prodCharacteristics==null || prodCharacteristics.isEmpty())
				throw new BillingBadRequestException(String.format("Error! The Characteristics are missing in the Product to calculate the price for the ProductOfferingPrice '%s' ", pop.getId()));
			
			ProductSpecificationCharacteristicValueUse prodSpecCharValueUse= pop.getProdSpecCharValueUse().get(0);
			if(prodSpecCharValueUse.getName()==null) {
				throw new BillingBadRequestException(String.format("The name of the Characteristic is missing in the ProductOfferingPrice '%s'", pop.getId()));
			}
			Characteristic matchChar=null;
			
			for(Characteristic productCharacteristic : prodCharacteristics) {
				if(prodSpecCharValueUse.getName().equalsIgnoreCase(productCharacteristic.getName())) {
					matchChar=productCharacteristic;
					break;
				}
			}
			if(matchChar==null) {
				throw new BillingBadRequestException(String.format("Error! No matching Characteristic found for the ProductOfferingPrice '%s'", pop.getId()));
			}
			
			// calculates the base price of the Characteristic 
			itemPrice = calculatePriceForCharacteristic(pop, matchChar);
				
		    // applies price alterations
			if (PriceUtils.hasRelationships(pop)) {
				Price alteratedPrice=pac.applyAlterations(pop, itemPrice);
				
				logger.info("Price of Characteristic '{}' '{}' after alterations: {} euro", 
				matchChar.getName(), matchChar.getValue(), alteratedPrice.getDutyFreeAmount());
				
				return alteratedPrice;
			
			}
		}
		
		return itemPrice;
	}
	
	private Money getMoney(@NotNull ProductOfferingPrice pop) throws BillingEngineValidationException {
		tmfEntityValidator.validatePrice(pop);
		
		Money money=pop.getPrice();
		
		if(money.getUnit()==null || money.getUnit().isEmpty())
			money.setUnit(DEFAULT_CURRENCY);
		
		return money;
	}
	

}
