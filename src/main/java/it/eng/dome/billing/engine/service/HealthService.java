package it.eng.dome.billing.engine.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.api.ProductInventoryApis;
import it.eng.dome.brokerage.api.UsageManagementApis;
import it.eng.dome.brokerage.api.fetch.FetchUtils;
import it.eng.dome.brokerage.observability.AbstractHealthService;
import it.eng.dome.brokerage.observability.health.Check;
import it.eng.dome.brokerage.observability.health.Health;
import it.eng.dome.brokerage.observability.health.HealthStatus;
import it.eng.dome.brokerage.observability.info.Info;


@Service
public class HealthService extends AbstractHealthService {

	private final Logger logger = LoggerFactory.getLogger(HealthService.class);
	private final static String SERVICE_NAME = "Billing Engine";
	
	private final ProductCatalogManagementApis productCatalogManagementApis;
	private final UsageManagementApis usageManagementApis;
	private final ProductInventoryApis productInventoryApis;

	
	public HealthService(ProductCatalogManagementApis productCatalogManagementApis, 
			UsageManagementApis usageManagementApis, ProductInventoryApis productInventoryApis) {
		
		this.productCatalogManagementApis = productCatalogManagementApis;
		this.usageManagementApis = usageManagementApis;
		this.productInventoryApis = productInventoryApis;
	}
	
	@Override
	public Info getInfo() {

		Info info = super.getInfo();
		logger.debug("Response: {}", toJson(info));

		return info;
	}
	
	@Override
	public Health getHealth() {
		Health health = new Health();
		health.setDescription("Health for the " + SERVICE_NAME);

		health.elevateStatus(HealthStatus.PASS);

		// 1: check of the TMForum APIs dependencies
		for (Check c : getTMFChecks()) {
			health.addCheck(c);
			health.elevateStatus(c.getStatus());
		}

		// 2: check dependencies: in case of FAIL or WARN set it to WARN
		boolean onlyDependenciesFailing = health.getChecks("self", null).stream()
				.allMatch(c -> c.getStatus() == HealthStatus.PASS);
		
		if (onlyDependenciesFailing && health.getStatus() == HealthStatus.FAIL) {
	        health.setStatus(HealthStatus.WARN);
	    }

		// 3: check self info
	    for(Check c: getChecksOnSelf()) {
	    	health.addCheck(c);
	    	health.elevateStatus(c.getStatus());
        }
	    
	    // 4: build human-readable notes
	    health.setNotes(buildNotes(health));
		
		logger.debug("Health response: {}", toJson(health));
		
		return health;
	}

	
	private List<Check> getChecksOnSelf() {
	    List<Check> out = new ArrayList<>();

	    // Check getInfo API
	    Info info = getInfo();
	    HealthStatus infoStatus = (info != null) ? HealthStatus.PASS : HealthStatus.FAIL;
	    String infoOutput = (info != null)
	            ? SERVICE_NAME + " version: " + info.getVersion()
	            : SERVICE_NAME + " getInfo returned unexpected response";
	    
	    Check infoCheck = createCheck("self", "get-info", "api", infoStatus, infoOutput);
	    out.add(infoCheck);

	    return out;
	}
	
	private List<Check> getTMFChecks() {

		List<Check> out = new ArrayList<>();

		// TMF620
		Check tmf620 = createCheck("tmf-api", "connectivity", "tmf620");

		try {
			FetchUtils.streamAll(productCatalogManagementApis::listProductOfferingPrices, null, null, 1).findAny();

			tmf620.setStatus(HealthStatus.PASS);

		} catch (Exception e) {
			tmf620.setStatus(HealthStatus.FAIL);
			tmf620.setOutput(e.toString());
		}

		out.add(tmf620);

		// TMF635
		Check tmf635 = createCheck("tmf-api", "connectivity", "tmf635");

		try {
			FetchUtils.streamAll(usageManagementApis::listUsages, null, null, 1).findAny();

			tmf635.setStatus(HealthStatus.PASS);

		} catch (Exception e) {
			tmf635.setStatus(HealthStatus.FAIL);
			tmf635.setOutput(e.toString());
		}

		out.add(tmf635);
		
		// TMF637
		Check tmf637 = createCheck("tmf-api", "connectivity", "tmf637");

		try {
			FetchUtils.streamAll(productInventoryApis::listProducts, null, null, 1).findAny();

			tmf637.setStatus(HealthStatus.PASS);

		} catch (Exception e) {
			tmf637.setStatus(HealthStatus.FAIL);
			tmf637.setOutput(e.toString());
		}

		out.add(tmf637);

		return out;
	}
}
