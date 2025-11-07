package it.eng.dome.billing.engine.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.dome.billing.engine.bill.BillService;
import it.eng.dome.billing.engine.bill.BillUtils;
import it.eng.dome.billing.engine.utils.UsageUtils;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.api.UsageManagementApis;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

@Service
public class UsagePriceService {
	
	private final Logger logger = LoggerFactory.getLogger(UsagePriceService.class);
	
	@Autowired
	private UsageManagementApis usageManagementApis;
	
	@Autowired
	private TMFEntityValidator tmfEntityValidator;
	
	// Map with key=usageCharacteristic.name and value=list of UsageCharacteritic
	private	Map<String, List<UsageCharacteristic>> usageData= null;
		
	
	/*
	 * Initialize the HashMap of UsageCharacteristic retrieving via TMForum all the usageData associated with the specified product ID and belonging to the specified TimePeriod
	 */
	private Map<String, List<UsageCharacteristic>> inizializeUsageData(@NonNull String productId, @NotNull TimePeriod tp){
		
		List<Usage> usages=UsageUtils.getUsages(productId, tp, usageManagementApis);
		logger.info("Usage found: {}", usages.size());
		
		tmfEntityValidator.validateUsages(usages);
		
		usageData=UsageUtils.createUsageCharacteristicDataMap(usages);
		logger.info("Created UsageDataMap with keys "+usageData.keySet().toString());
		
		return usageData;
	}


}
