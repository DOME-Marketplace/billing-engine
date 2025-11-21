package it.eng.dome.billing.engine.utils;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.util.CollectionUtils;

import it.eng.dome.brokerage.billing.utils.BillingPriceType;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.brokerage.model.BillCycle;
import it.eng.dome.brokerage.model.BillCycleSpecification;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingTerm;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.RelatedParty;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;
import it.eng.dome.tmforum.tmf678.v4.model.Money;
import it.eng.dome.tmforum.tmf678.v4.model.ProductRef;
import it.eng.dome.tmforum.tmf678.v4.model.StateValue;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

public class TMForumEntityUtils {
	
	
	public static AppliedCustomerBillingRate createAppliedCustomerBillingRate(@NotNull ProductOfferingPrice pop, @NonNull Product product, @NonNull BillCycle billCycle, @NonNull Money taxExcludedAmount, String relatedPartySchemaLocation){
		
		AppliedCustomerBillingRate appliedCustomerBillingRate = new AppliedCustomerBillingRate();

		// Set appliedCustomerBillingRate.billingAccount
		appliedCustomerBillingRate.setBillingAccount(TmfConverter.convertBillingAccountRefTo678(product.getBillingAccount()));

		// Set appliedCustomerBillingRate.date
		appliedCustomerBillingRate.setDate(billCycle.getBillDate());

		// Set appliedCustomerBillingRate.description
		appliedCustomerBillingRate.setDescription(String.format("Bill for Product '%s' in billingPeriod [{%s}-{%s}]", product.getId(),
				billCycle.getBillingPeriod().getStartDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
				billCycle.getBillingPeriod().getEndDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
		
		// Set appliedCustomerBillingRate.isBilled
		appliedCustomerBillingRate.setIsBilled(true);

		// Set appliedCustomerBillingRate.name
		appliedCustomerBillingRate.setName(String.format("%s Bill", pop.getPriceType()));

		// Set appliedCustomerBillingRate.periodCoverage
		appliedCustomerBillingRate.setPeriodCoverage(billCycle.getBillingPeriod());

		// Set appliedCustomerBillingRate.product reference
		appliedCustomerBillingRate.setProduct(TMForumEntityUtils.createProductRef(product.getId()));
		
		// Set appliedCustomerBillinhRate.appliedBillingRateType
		appliedCustomerBillingRate.setType(pop.getPriceType());
		
		// Set appliedCustomerBillingRate.taxExcludedAmount
		appliedCustomerBillingRate.setTaxExcludedAmount(taxExcludedAmount);
		
		// Set appliedCustomerBillingRate.schemaLocation
		appliedCustomerBillingRate.setAtSchemaLocation(URI.create(relatedPartySchemaLocation));
		
		// Set appliedCustomerBillingRate.relatedParty (if present in the Product)
		List<RelatedParty> prodRelatedParty = product.getRelatedParty();
		appliedCustomerBillingRate.setRelatedParty(TmfConverter.convertRpTo678(prodRelatedParty));
		
		return appliedCustomerBillingRate;
	}
	
	public static ProductRef createProductRef(@NotNull String prodId) {
		ProductRef prodRef = new ProductRef();
		prodRef.setId(prodId);
		
		return prodRef;
	}
	
	public static TimePeriod createTimePeriod678(@NotNull OffsetDateTime start, @NotNull OffsetDateTime end) {
		TimePeriod tp=new TimePeriod();
		tp.setStartDateTime(start);
		tp.setEndDateTime(end);
		
		return tp;
	}
	
	public static CustomerBill createCustomerBill(@NotNull List<AppliedCustomerBillingRate> acbrs, @NotNull Product prod, @NotNull TimePeriod billingPeriod, BillCycleSpecification billCycleSpec) {
		CustomerBill cb=new CustomerBill();
		
		OffsetDateTime currrentDate=OffsetDateTime.now();
		
		Float totalAmount=0f;
		String currency=acbrs.get(0).getTaxExcludedAmount().getUnit();
		
		for(AppliedCustomerBillingRate acbr:acbrs) {
			totalAmount+=acbr.getTaxExcludedAmount().getValue();
		}
		
		//Set customerBill.amountDue
		Money totalAmountMoney=new Money();
		totalAmountMoney.setValue(totalAmount);
		totalAmountMoney.setUnit(currency);
		cb.setAmountDue(totalAmountMoney);
		
		//Set customerBill.billDate and customerBill.paymentDueDate
		if(billCycleSpec==null) {
			cb.setBillDate(billingPeriod.getEndDateTime());
		    cb.setPaymentDueDate(billingPeriod.getEndDateTime());
		}	
		else{
			OffsetDateTime billDate=billingPeriod.getEndDateTime().plusDays(billCycleSpec.getBillingDateShift());
			cb.setBillDate(billDate);
			cb.setPaymentDueDate(billDate.plusDays(billCycleSpec.getPaymentDueDateOffset()));
		}
		
		//Set customerBill.billNo
		cb.setBillNo("BILL-" + System.currentTimeMillis());
		
		//Set customerBill.billingAccount
		cb.setBillingAccount(TmfConverter.convertBillingAccountRefTo678(prod.getBillingAccount()));
		
		//Set customerBill.billingPeriod
		cb.setBillingPeriod(billingPeriod);
		
		//Set customerBill.lastUpdate
		cb.setLastUpdate(currrentDate);
		
		// Set customerBill.relatedParty (if present in the Product)
		List<RelatedParty> prodRelatedParty = prod.getRelatedParty();
		cb.setRelatedParty(TmfConverter.convertRpTo678(prodRelatedParty));
		
		// Set customerBill.remainingAmount
		cb.setRemainingAmount(totalAmountMoney);
		                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       
		// Set customerBill.state
		cb.setState(StateValue.NEW);
		
		// Set customerBill.taxExcludedAmount
		cb.setTaxExcludedAmount(totalAmountMoney);
		
		return cb;
	}
	
	public static Price createPriceTMF622(@NotNull it.eng.dome.billing.engine.model.Money money) {
		Price price=new Price();
		price.setDutyFreeAmount(TmfConverter.convertMoneyTo622(money));
		price.setTaxIncludedAmount(null);
		return price;
	}
	
	public static OrderPrice createOrderPriceTMF622(@NotNull Price price, @NotNull ProductOfferingPrice pop) {
		OrderPrice op=new OrderPrice();
		
		op.setName(pop.getName());
		op.setDescription(pop.getDescription());
		op.setPriceType(ProductOfferingPriceUtils.getPriceType(pop).toString());
		if(ProductOfferingPriceUtils.isPriceTypeInRecurringCategory(pop)) {
			op.setRecurringChargePeriod(ProductOfferingPriceUtils.getRecurringChargePeriod(pop).toString());
		}
		op.setPrice(price);
		
		return op;
	}
	
	public static PriceAlteration createPriceAlteration(@NotNull Price price, @NotNull ProductOfferingPrice alterationPop) {
		PriceAlteration priceAlteration=new PriceAlteration();
		
		priceAlteration.description(alterationPop.getDescription())
		.name(alterationPop.getName())
		.priceType(ProductOfferingPriceUtils.getPriceType(alterationPop).toString())
		.setPrice(price);
		
		if (!CollectionUtils.isEmpty(alterationPop.getProductOfferingTerm())) {
			ProductOfferingTerm term = alterationPop.getProductOfferingTerm().get(0);
			
			priceAlteration
			.applicationDuration(term.getDuration().getAmount())
			.setUnitOfMeasure(term.getDuration().getUnits());
		}
		
		//logger.info("Applied {}% discount to price of {} euro. Discounted price: {} {}", 
		//		alterationPOP.getPercentage(), basePrice, discounteMoney.getValue(), discounteMoney.getUnit());
		
		return priceAlteration;
	}
}
