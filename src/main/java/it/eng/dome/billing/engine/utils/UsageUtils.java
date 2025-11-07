package it.eng.dome.billing.engine.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.eng.dome.brokerage.api.UsageManagementApis;
import it.eng.dome.brokerage.api.fetch.FetchUtils;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import lombok.NonNull;

public class UsageUtils {
	
	/**
	 * 
	 * @param productId
	 * @param tp
	 * @param usageManagementApis
	 * @return
	 */
	public static List<Usage> getUsages(@NonNull String productId, @NonNull TimePeriod tp, UsageManagementApis usageManagementApis){
		
		// add filter for usages 
		// Get all Usage related to the product and within the TimePeriod
		Map<String, String> filter = new HashMap<String, String>();
		filter.put("ratedProductUsage.productRef", productId);
		filter.put("usageDate.lt", tp.getEndDateTime().toString());
		filter.put("usageDate.gt", tp.getStartDateTime().toString());
		
		List<Usage> usages = FetchUtils.streamAll(
				usageManagementApis::listUsages, // method reference
		        null,                       	// fields
		        filter, 				   		// filter
		        100                         	// pageSize
			) 
			.toList();	
		
		return usages;
	}
	
	/**
	 * Creates a {@link UsageCharacteristic} map with key the UsageCharacteristic's name. This map permits to retrieve from a list of {@link Usage} 
	 * all the {@link UsageCharacteristic} with a UsageCharacteristic's name
	 * 
	 * @param usages A list of {@link Usage}
	 * @return A Map with key the {@link UsageCharacteristic}'s name and with value the list of {@link UsageCharacteristic} with that name
	 */
	public static Map<String, List<UsageCharacteristic>> createUsageCharacteristicDataMap(@NonNull List<Usage> usages){
		Map<String, List<UsageCharacteristic>> usageData=new HashMap<String, List<UsageCharacteristic>>();
		
		for (Usage usage : usages) {
			if (usage.getUsageCharacteristic() != null && !usage.getUsageCharacteristic().isEmpty()) {
				
				List<UsageCharacteristic> usageCharacteristics = usage.getUsageCharacteristic();
				
				for (UsageCharacteristic usageCharacteristic : usageCharacteristics) {
					if (usageCharacteristic != null) {
						usageData.computeIfAbsent(usageCharacteristic.getName(), K -> new ArrayList<UsageCharacteristic>()).add(usageCharacteristic);
					}				
				}
			}
		}
		
		return usageData;
	}
	
	/**
	 * Returns the list of {@link UsageCharacteristic} with the specified metric's name
	 * 
	 * @param usageData A map with key the {@link UsageCharacteristic}'s name and value a list of {@link UsageCharacteristic}
	 * @param metric the metric's name
	 * @return The list of {@link UsageCharacteristic} with the specified metric's name
	 */
	public static List<UsageCharacteristic> getUsageCharacteristicsForMetric(@NonNull Map<String, List<UsageCharacteristic>> usageData, @NonNull String metric){
		return usageData.get(metric);
	}

}
