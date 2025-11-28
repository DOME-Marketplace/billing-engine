package it.eng.dome.billing.engine.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.eng.dome.brokerage.api.UsageManagementApis;
import it.eng.dome.brokerage.api.fetch.FetchUtils;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import lombok.NonNull;

public class UsageUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(UsageUtils.class);
	
	/**
	 * Gets all {@link Usage} related in to the {@link Product} with the product identifier in input and belonging to the specified {@link TimePeriod}
	 * @param productId the identifier of the {@link Product}
	 * @param tp the billingPeriod to take into consideration
	 * @param usageManagementApis An instance of {@link UsageManagementApis} to retrieve the Usage(s)
	 * @return A list of Usage for the specified product identifier and belonging to the specified {@link TimePeriod}
	 */
	public static List<Usage> getUsages(@NonNull String productId, @NonNull TimePeriod tp, UsageManagementApis usageManagementApis){
		
		AtomicInteger count = new AtomicInteger(0);
		
		List<Usage> usages=FetchUtils.streamAll(
			 usageManagementApis::listUsages,  // method TMF GET /usage
		        null,            // fields
		        null,            // date filter server-side
		        100              // page size
		)
		// Filter for usageDate
		 .filter(u ->
		        u.getUsageDate() != null &&
		        !u.getUsageDate().isBefore(tp.getStartDateTime()) &&
		        !u.getUsageDate().isAfter(tp.getEndDateTime())
		    )
		    // Filter for ratedProductUsage.productRef.id
		    .filter(u ->
		        u.getRatedProductUsage() != null &&
		        u.getRatedProductUsage().stream().anyMatch(rpu ->
		            rpu.getProductRef() != null &&
		            productId.equals(rpu.getProductRef().getId())
		        )
		    )
		    .peek(u -> {
	            int index = count.incrementAndGet();
	            logger.debug(index + " " +
	                    u.getId() + " → " +
	                    u.getUsageType() + " / " +
	                    u.getUsageDate());
	        })
	        .collect(Collectors.toList());
			// System.out.println("Usage found: " + usages.size());
		
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
