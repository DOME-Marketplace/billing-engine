package it.eng.dome.billing.engine;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.api.ProductInventoryApis;
import it.eng.dome.brokerage.api.UsageManagementApis;
import it.eng.dome.brokerage.api.fetch.FetchUtils;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf635.v4.model.RelatedParty;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.ProductPrice;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;

public class TestUsageCompute {

	private static String TMF_SERVER = "https://dome-dev.eng.it";
	
	
	public static void main(String[] args) {
			
		// Product.ID && TimePeriod
		//TestIsfilteredUsage();
		
		// full test
		try {
			TestUsagePayPerUse();
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (it.eng.dome.tmforum.tmf637.v4.ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void TestIsfilteredUsage() {
		System.out.println("Test filtered Usages");
		
		it.eng.dome.tmforum.tmf635.v4.ApiClient clientTMF635 = it.eng.dome.tmforum.tmf635.v4.Configuration.getDefaultApiClient();
		clientTMF635.setBasePath(TMF_SERVER + "/tmf-api/usageManagement/v4");
		UsageManagementApis usageManagementApis = new UsageManagementApis(clientTMF635);
		
		TimePeriod tp = new TimePeriod();
		tp.setEndDateTime(OffsetDateTime.parse("2025-06-20T14:17:00.998612801Z"));
		tp.setStartDateTime(OffsetDateTime.parse("2025-06-20T14:17:00.928612799Z"));
		
		String productId = "urn:ngsi-ld:product:19402bf1-0889-46e5-9f6c-6fc458be024f";
		
		
		// filter usages
		Map<String, String> filter = new HashMap<String, String>();
		filter.put("ratedProductUsage.productRef", productId);
		filter.put("usageDate.lt", tp.getEndDateTime().toString());
		filter.put("usageDate.gt", tp.getStartDateTime().toString());

		
		// Get all Usage related to the product (Usage.ratedProductUsage.productRef.id=product.id) && Usage.usageDate is within the TimePeriod
//		List<Usage> usages = usageManagementApis.getAllUsages(null, filter);
		List<Usage> usages = FetchUtils.streamAll(
				usageManagementApis::listUsages, 				// method reference
		        null,                       	// fields
		        null, 				   		// filter
		        100                         	// pageSize
			).toList(); 
		
		int count = 0;
		
		for (Usage usage : usages) {
			System.out.println(++count + " => " + usage.getId() + " / " + usage.getUsageType() + " / " + usage.getUsageDate() );
		}
	}
	
	public static void TestUsagePayPerUse() throws ApiException, it.eng.dome.tmforum.tmf637.v4.ApiException {
		System.out.println("Test Usages for PayPerUse");
		
		it.eng.dome.tmforum.tmf637.v4.ApiClient clientTMF637 = it.eng.dome.tmforum.tmf637.v4.Configuration.getDefaultApiClient();
		clientTMF637.setBasePath(TMF_SERVER + "/tmf-api/productInventory/v4");
		
		//ProductApis productApis = new ProductApis(clientTMF637);
		ProductInventoryApis productApis = new ProductInventoryApis(clientTMF637);
		
		Product product = productApis.getProduct("urn:ngsi-ld:product:19402bf1-0889-46e5-9f6c-6fc458be024f", null);
		
		it.eng.dome.tmforum.tmf635.v4.ApiClient clientTMF635 = it.eng.dome.tmforum.tmf635.v4.Configuration.getDefaultApiClient();
		clientTMF635.setBasePath(TMF_SERVER + "/tmf-api/usageManagement/v4");
		UsageManagementApis usageManagementApis = new UsageManagementApis(clientTMF635);
		
		it.eng.dome.tmforum.tmf620.v4.ApiClient clientTMF620 = it.eng.dome.tmforum.tmf620.v4.Configuration.getDefaultApiClient();
		clientTMF620.setBasePath(TMF_SERVER + "/tmf-api/productCatalogManagement/v4");
		//ProductOfferingPriceApis productOfferingPriceApis = new ProductOfferingPriceApis(clientTMF620);
		ProductCatalogManagementApis productCatalogManagementApis = new ProductCatalogManagementApis(clientTMF620);


		// dummy values
		TimePeriod tp = new TimePeriod();
		tp.setEndDateTime(OffsetDateTime.parse("2025-06-20T14:17:00.998612801Z"));
		tp.setStartDateTime(OffsetDateTime.parse("2025-06-20T14:17:00.928612799Z"));
		
		String productId = "urn:ngsi-ld:product:19402bf1-0889-46e5-9f6c-6fc458be024f";
		
		
		// add filter for usages
		Map<String, String> filter = new HashMap<String, String>();
		filter.put("ratedProductUsage.productRef", productId);
		filter.put("usageDate.lt", tp.getEndDateTime().toString());
		filter.put("usageDate.gt", tp.getStartDateTime().toString());
		
		
//		List<Usage> usages = usageManagementApis.getAllUsages(null, filter);
		List<Usage> usages = FetchUtils.streamAll(
				usageManagementApis::listUsages, 				// method reference
		        null,                       	// fields
		        null, 				   		// filter
		        100                         	// pageSize
			).toList();
		
		// set the map keys/values from usage list
		Map<String, Object> usageData = new HashMap<String, Object>();		
				
		for (Usage usage : usages) {
			if (usage.getUsageCharacteristic() != null && !usage.getUsageCharacteristic().isEmpty()) {
				System.out.println(" -> Current usage: " + usage.getId());
				
				// get RelatedParty
				List<RelatedParty> relatedParty = usage.getRelatedParty();
				System.out.println("Size relatedParty: " + relatedParty.size());
				
				List<UsageCharacteristic> usageCharacteristics = usage.getUsageCharacteristic();
				for (UsageCharacteristic usageCharacteristic : usageCharacteristics) {
					if (usageCharacteristic != null) {
						System.out.println("Add key/value in Map: " + usageCharacteristic.getName() + " - " + usageCharacteristic.getValue());
						usageData.put(usageCharacteristic.getName(), usageCharacteristic.getValue());
					}
				}
			}
		}
		
		System.out.println("usageData Map() size: " + usageData.size());
		
		BigDecimal amount = BigDecimal.ZERO;
		Set<String> keys = usageData.keySet();

		System.out.println("Hashmap keys: " + keys.toString());
		
		if (product.getProductPrice() != null) {
			List<ProductPrice> pprices = product.getProductPrice();
			if (pprices != null && !pprices.isEmpty()) {
				
				for (ProductPrice pprice : pprices) {
					
					System.out.println("pop name: " + pprice.getName());
					
					// get popId
					if (pprice.getProductOfferingPrice() != null) {
						String popId = pprice.getProductOfferingPrice().getId();
						System.out.println("popId: " + popId);
						ProductOfferingPrice pop = productCatalogManagementApis.getProductOfferingPrice(popId, null);
						
						//TODO - retrieve usage amount
						String key = pop.getUnitOfMeasure().getUnits();	

						if (usageData.containsKey(key)) {
							System.out.println(">>> ----- Calculate");
							BigDecimal price = new BigDecimal(usageData.get(key).toString()); 
							
							//FIXME  calculate amount ...
							BigDecimal amt = new BigDecimal(pop.getUnitOfMeasure().getAmount().toString()); 
							
							BigDecimal temp = amt.multiply(price);
							System.out.println(price + " x " + amt + " = " +  temp);

							amount = amount.add(temp);
						}
						
					}else {
						System.out.println(">> Error: NO POP FOUND");
					}
					
				}

			}
		}
	
		System.out.println("Total amount: " + amount);
	}

}
