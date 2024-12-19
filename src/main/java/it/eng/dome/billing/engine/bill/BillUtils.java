package it.eng.dome.billing.engine.bill;

import java.util.List;

import it.eng.dome.tmforum.tmf637.v4.model.BillingAccountRef;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.ProductPrice;
import lombok.NonNull;


public final class BillUtils {

	
	public static List<ProductPrice> getProductPrices(@NonNull Product product) {
		return product.getProductPrice();
	}
	
	public static it.eng.dome.tmforum.tmf678.v4.model.BillingAccountRef createAppliedCustomerRateBillingAccount(@NonNull BillingAccountRef productBillingAccountRef) {
		
		 it.eng.dome.tmforum.tmf678.v4.model.BillingAccountRef billingAccountRef=new it.eng.dome.tmforum.tmf678.v4.model.BillingAccountRef();
		 
		 billingAccountRef.setId(productBillingAccountRef.getId());
		 billingAccountRef.setName(productBillingAccountRef.getName());
		 billingAccountRef.setHref(productBillingAccountRef.getHref());
		 billingAccountRef.setAtType(productBillingAccountRef.getAtType());
		 billingAccountRef.setAtSchemaLocation(productBillingAccountRef.getAtSchemaLocation());
		 billingAccountRef.setAtReferredType(productBillingAccountRef.getAtReferredType());
		 billingAccountRef.setAtBaseType(productBillingAccountRef.getAtBaseType());
		 
		 return billingAccountRef;
	}

}
