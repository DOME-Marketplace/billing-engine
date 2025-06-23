package it.eng.dome.billing.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.eng.dome.brokerage.api.ProductApis;
import it.eng.dome.brokerage.api.UsageManagementApis;
import it.eng.dome.tmforum.tmf635.v4.model.RelatedParty;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.ProductPrice;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;

public class TestUsageCompute {

	private static String TMF_SERVER = "https://dome-dev.eng.it";
	
	
	public static void main(String[] args) {
			
		it.eng.dome.tmforum.tmf637.v4.ApiClient clientTMF637 = it.eng.dome.tmforum.tmf637.v4.Configuration.getDefaultApiClient();
		clientTMF637.setBasePath(TMF_SERVER + "/tmf-api/productInventory/v4");
		
		ProductApis productApis = new ProductApis(clientTMF637);
		
		Product product = productApis.getProduct("urn:ngsi-ld:product:19402bf1-0889-46e5-9f6c-6fc458be024f", null);
		float amount = getPayPerUse(product, null);
		System.out.println("Amount: " + amount);
		
		
	}
	
	private static float getPayPerUse(Product product, TimePeriod tp) {
		
		//
		it.eng.dome.tmforum.tmf635.v4.ApiClient clientTMF635 = it.eng.dome.tmforum.tmf635.v4.Configuration.getDefaultApiClient();
		clientTMF635.setBasePath(TMF_SERVER + "/tmf-api/usageManagement/v4");
		UsageManagementApis usageManagementApis = new UsageManagementApis(clientTMF635);
		
		// filter with TimePeriod
		List<Usage> usages = usageManagementApis.getAllUsages(null, null);
		
		Map<String, Object> usageData = new HashMap<String, Object>();
		
		
		float amount = 0;
		for (Usage usage : usages) {
			if (usage.getRatedProductUsage() != null && usage.getRatedProductUsage().size() > 0) {
				System.out.println(" -> Current usage: " + usage.getId());
				
				// get RelatedParty
				List<RelatedParty> relatedParty = usage.getRelatedParty();
				System.out.println("Size relatedParty: " + relatedParty.size());
				
				List<UsageCharacteristic> usageCharacteristics = usage.getUsageCharacteristic();
				for (UsageCharacteristic usageCharacteristic : usageCharacteristics) {
					if (usageCharacteristic != null) {
						System.out.println("add key: " + usageCharacteristic.getName() + " with value: " + usageCharacteristic.getValue());
						usageData.put(usageCharacteristic.getName(), usageCharacteristic.getValue());
					}
				}
			}
		}
		
		Set<String> keys = usageData.keySet();
		System.out.println("dim usageData: " + usageData.size());
		System.out.println(keys.toString());
		
		if (product.getProductPrice() != null) {
			List<ProductPrice> pprices = product.getProductPrice();
			if (pprices != null && !pprices.isEmpty()) {
				
				for (ProductPrice pprice : pprices) {
					// retrieve price
					String key = pprice.getName();
					
					if (usageData.containsKey(key)) {
						Object value = usageData.get(key);
						
						//FIXME - DutyFreeAmount NULL
						/*if (value instanceof Number) {
							Float price = (float) pprice.getPrice().getTaxIncludedAmount().getValue();
							amount += price * (float)value;
						}*/
						Float dutyFreeAmount = (float) pprice.getPrice().getTaxRate();
						System.out.println("DutyFreeAmount: " + dutyFreeAmount);
						
						float price = Float.valueOf(value.toString()) ;
						System.out.println("Price: " + price);

						amount +=  dutyFreeAmount * price;
					}
					
				}
			}
		}
	
		return amount;
	}

}
