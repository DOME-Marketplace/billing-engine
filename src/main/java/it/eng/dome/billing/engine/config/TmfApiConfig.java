package it.eng.dome.billing.engine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.eng.dome.billing.engine.tmf.TmfApiFactory;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.api.ProductInventoryApis;
import it.eng.dome.brokerage.api.UsageManagementApis;


@Configuration
public class TmfApiConfig {
	
	private final Logger logger = LoggerFactory.getLogger(TmfApiConfig.class);
	
	@Autowired
	private TmfApiFactory tmfApiFactory;
	
	@Bean
    public ProductCatalogManagementApis productCatalogManagementApis() {
		logger.info("Initializing of ProductCatalogManagementApis");
		
		return new ProductCatalogManagementApis(tmfApiFactory.getTMF620ProductCatalogApiClient());
	}
	
	@Bean
    public UsageManagementApis usageManagementApis() {
		logger.info("Initializing of UsageManagementApis");
		
		return new UsageManagementApis(tmfApiFactory.getTMF635UsageManagementApiClient());
	}
	
	@Bean
    public ProductInventoryApis productInventoryApis() {
		logger.info("Initializing of ProductInventoryApis");
		
		return new ProductInventoryApis(tmfApiFactory.getTMF637ProductInventoryApiClient());
	}

}
