package it.eng.dome.billing.engine;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import it.eng.dome.billing.engine.utils.TMForumEntityUtils;
import it.eng.dome.tmforum.tmf637.v4.model.BillingAccountRef;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.ProductOfferingPriceRef;
import it.eng.dome.tmforum.tmf637.v4.model.ProductPrice;
import it.eng.dome.tmforum.tmf637.v4.model.RelatedParty;
import it.eng.dome.tmforum.tmf678.v4.JSON;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.Money;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;

public class TestACBR_RelatedParty {
	
	public TestACBR_RelatedParty() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args) {
		TestACBR_RelatedParty test=new TestACBR_RelatedParty();
		
		Product product=new Product();
		product.setId("1234");
		product.setDescription("Test Product 1");
		
		BillingAccountRef billingAccountRef=new BillingAccountRef();
		billingAccountRef.setId("4567");
		billingAccountRef.setName("BillingAccountRef Test");
		
		product.setBillingAccount(billingAccountRef);
		product.setIsBundle(false);
		
		TimePeriod tp=new TimePeriod();
		tp.setStartDateTime(OffsetDateTime.now());
		tp.setEndDateTime(OffsetDateTime.now());
		
		Money amount=new Money();
		amount.setUnit("Eur");
		amount.setValue(Float.valueOf(10));
		
		ArrayList<ProductPrice> productPriceList=new ArrayList<ProductPrice>();
		
		ProductPrice pp=new ProductPrice();
		ProductOfferingPriceRef popr=new ProductOfferingPriceRef();
		popr.setId("urn:ngsi-ld:product-offering-price:38b293a6-92db-4ca3-8fe6-54a6e4a9e12c");
		pp.setPriceType("recurring");
		pp.setProductOfferingPrice(popr);
		
		ProductPrice pp2=new ProductPrice();
		ProductOfferingPriceRef popr2=new ProductOfferingPriceRef();
		popr2.setId("urn:ngsi-ld:product-offering-price:04a8cd8e-c88f-49be-8425-dca68e69708b");
		pp2.setPriceType("recurring");
		pp2.setProductOfferingPrice(popr2);
		
		productPriceList.add(pp);
		productPriceList.add(pp2);
		
		product.setProductPrice(productPriceList);
		
		RelatedParty rpBuyer=test.createRelatedParty("Buyer");
		RelatedParty rpSeller=test.createRelatedParty("Seller");
		RelatedParty rpBuyerOp=test.createRelatedParty("BuyerOperator");
		RelatedParty rpSellerOp=test.createRelatedParty("SellerOperator");
		
	    product.addRelatedPartyItem(rpBuyer);
	    product.addRelatedPartyItem(rpSeller);
	    product.addRelatedPartyItem(rpBuyerOp);
	    product.addRelatedPartyItem(rpSellerOp);
	    
		AppliedCustomerBillingRate acbr=null;
		try {
			//acbr = BillUtils.createAppliedCustomerBillingRate(product, tp, amount, "recurring", "testSchemaLocation");
			TMForumEntityUtils.createAppliedCustomerBillingRate(null, product, null, amount, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String str=JSON.getGson().toJson(acbr);
		System.out.print(str);
	}
	
	private RelatedParty createRelatedParty(String role) {
		RelatedParty rp=new RelatedParty();
		
		rp.setAtBaseType("");
		rp.setAtReferredType("Organization");
		rp.setAtSchemaLocation(URI.create(""));
		rp.setHref("urn:ngsi-ld:party:221f6434-ec82-4c6");
		rp.setId("urn:ngsi-ld:party:221f6434-ec82-4c6");
		rp.setName("did:elsi:VATES-11111111");
		rp.setRole(role);
		
		return rp;
	}

}
