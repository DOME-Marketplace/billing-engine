package it.eng.dome.billing.engine;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import it.eng.dome.billing.engine.dto.BillingRequestDTO;
import it.eng.dome.tmforum.tmf637.v4.model.BillingAccountRef;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.ProductOfferingPriceRef;
import it.eng.dome.tmforum.tmf637.v4.model.ProductPrice;
import it.eng.dome.tmforum.tmf678.v4.JSON;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;

public class CreateBillingRequestDTO {
	
	public static void main(String[] args) {
		
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
		
		BillingRequestDTO brDTO=new BillingRequestDTO(product, tp, productPriceList);
		
		String str=JSON.getGson().toJson(brDTO);
		System.out.print(str);
	}

}
