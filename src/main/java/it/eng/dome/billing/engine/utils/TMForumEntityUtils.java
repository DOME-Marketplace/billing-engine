package it.eng.dome.billing.engine.utils;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.List;

import it.eng.dome.brokerage.model.BillCycle;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.RelatedParty;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.Money;
import it.eng.dome.tmforum.tmf678.v4.model.ProductRef;
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

}
