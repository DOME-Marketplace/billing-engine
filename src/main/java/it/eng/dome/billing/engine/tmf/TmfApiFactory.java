package it.eng.dome.billing.engine.tmf;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import it.eng.dome.brokerage.billing.utils.UrlPathUtils;


@Component(value = "tmfApiFactory")
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public final class TmfApiFactory implements InitializingBean {
	
	private static final Logger log = LoggerFactory.getLogger(TmfApiFactory.class);
	private static final String TMF_ENDPOINT_CONCAT_PATH = "-";
	
    @Value("${tmforumapi.tmf_endpoint}")
    public String tmfEndpoint;
    
    @Value("${tmforumapi.tmf_envoy}")
    public boolean tmfEnvoy;
    
    @Value("${tmforumapi.tmf_namespace}")
    public String tmfNamespace;
    
    @Value("${tmforumapi.tmf_postfix}")
    public String tmfPostfix;    
    
    @Value("${tmforumapi.tmf_port}")
    public String tmfPort;
	
	@Value( "${tmforumapi.tmf620_catalog_path}" )
	private String tmf620ProductCatalogPath;

	@Value( "${tmforumapi.tmf622_ordering_path}" )
	private String tmf622ProductOrderingPath;

	@Value( "${tmforumapi.tmf678_billing_path}" )
	private String tmf678CustomerBillPath;
	
	@Value( "${tmforumapi.tmf637_billing_path}" )
	private String tmf637ProductInventoryPath;
	
	@Value( "${schema.schemaLocation_relatedParty}" )
	private String schemaLocationRelatedParty;
	
	private it.eng.dome.tmforum.tmf620.v4.ApiClient apiClientTmf620;
	private it.eng.dome.tmforum.tmf622.v4.ApiClient apiClientTmf622;
	private it.eng.dome.tmforum.tmf678.v4.ApiClient apiClientTmf678;
	private it.eng.dome.tmforum.tmf637.v4.ApiClient apiClientTmf637;
	
	
	public it.eng.dome.tmforum.tmf620.v4.ApiClient getTMF620ProductCatalogApiClient() {
		if (apiClientTmf620 == null) {
			apiClientTmf620 = it.eng.dome.tmforum.tmf620.v4.Configuration.getDefaultApiClient();
			
			String basePath = tmfEndpoint;
			if (!tmfEnvoy) { // no envoy specific path
				basePath += TMF_ENDPOINT_CONCAT_PATH + "product-catalog" + "." + tmfNamespace + "." + tmfPostfix + ":" + tmfPort;
			}
			
			apiClientTmf620.setBasePath(basePath + "/" + tmf620ProductCatalogPath);
			log.debug("Invoke Catalog API at endpoint: " + apiClientTmf620.getBasePath());			
		}
		
		return apiClientTmf620;
	}	
	
	public it.eng.dome.tmforum.tmf622.v4.ApiClient getTMF622ProductOrderingApiClient() {	
		if (apiClientTmf622 == null) {
			apiClientTmf622 = it.eng.dome.tmforum.tmf622.v4.Configuration.getDefaultApiClient(); 
			
			String basePath = tmfEndpoint;
			if (!tmfEnvoy) { // no envoy specific path
				basePath += TMF_ENDPOINT_CONCAT_PATH + "product-ordering-management" + "." + tmfNamespace + "." + tmfPostfix + ":" + tmfPort;
			}
			
			apiClientTmf622.setBasePath(basePath + "/" + tmf637ProductInventoryPath);
			log.debug("Invoke Product Ordering API at endpoint: " + apiClientTmf622.getBasePath());
		}
		
		return apiClientTmf622;
	}
	
	public it.eng.dome.tmforum.tmf678.v4.ApiClient getTMF678CustomerBillApiClient() {
		if (apiClientTmf678 == null) { 
			apiClientTmf678 = it.eng.dome.tmforum.tmf678.v4.Configuration.getDefaultApiClient();
			
			String basePath = tmfEndpoint;
			if (!tmfEnvoy) { // no envoy specific path
				basePath += TMF_ENDPOINT_CONCAT_PATH + "customer-bill-management" + "." + tmfNamespace + "." + tmfPostfix + ":" + tmfPort;
			}
			
			apiClientTmf678.setBasePath(basePath + "/" + tmf678CustomerBillPath);
			log.debug("Invoke Customer Billing API at endpoint: " + apiClientTmf678.getBasePath());
		}
		return apiClientTmf678;
	}
	
	public it.eng.dome.tmforum.tmf637.v4.ApiClient getTMF637ProductInventoryApiClient() {
		if (apiClientTmf637 == null) {
			apiClientTmf637 = it.eng.dome.tmforum.tmf637.v4.Configuration.getDefaultApiClient(); 
			
			String basePath = tmfEndpoint;
			if (!tmfEnvoy) { // no envoy specific path
				basePath += TMF_ENDPOINT_CONCAT_PATH + "product-inventory" + "." + tmfNamespace + "." + tmfPostfix + ":" + tmfPort;
			}
			
			apiClientTmf637.setBasePath(basePath + "/" + tmf637ProductInventoryPath);
			log.debug("Invoke Product Inventory API at endpoint: " + apiClientTmf637.getBasePath());
		}
		
		return apiClientTmf637;
	}
	
	public String getSchemaLocationRelatedParty() {
		return schemaLocationRelatedParty;
	}

	
	@Override
	public void afterPropertiesSet() throws Exception {
		
		log.info("Billing Engine is using the following TMForum endpoint prefix: " + tmfEndpoint);	
		if (tmfEnvoy) {
			log.info("You set the apiProxy for TMForum endpoint. No tmf_port {} can be applied", tmfPort);	
		} else {
			log.info("No apiProxy set for TMForum APIs. You have to access on specific software via paths at tmf_port {}", tmfPort);	
		}
		
		Assert.state(!StringUtils.isBlank(tmfEndpoint), "Billing Engine not properly configured. tmf620_catalog_base property has no value.");
		Assert.state(!StringUtils.isBlank(tmf620ProductCatalogPath), "Billing Engine not properly configured. tmf620_catalog_path property has no value.");
		Assert.state(!StringUtils.isBlank(tmf622ProductOrderingPath), "Billing Engine not properly configured. tmf622_ordering_path property has no value.");
		Assert.state(!StringUtils.isBlank(tmf678CustomerBillPath), "Billing Engine not properly configured. tmf632PartyManagementPath property has no value.");
		Assert.state(!StringUtils.isBlank(tmf637ProductInventoryPath), "Billing Engine not properly configured. tmf637ProductInventoryPath property has no value.");
		Assert.state(!StringUtils.isBlank(tmf637ProductInventoryPath), "Billing Engine not properly configured. schemaLocation_relatedParty property has no value");
		
		if (tmfEndpoint.endsWith("/")) {
			tmfEndpoint = UrlPathUtils.removeFinalSlash(tmfEndpoint);		
		}
		
		if (tmf620ProductCatalogPath.startsWith("/")) {
			tmf620ProductCatalogPath = UrlPathUtils.removeInitialSlash(tmf620ProductCatalogPath);
		}
		
		if (tmf622ProductOrderingPath.startsWith("/")) {
			tmf622ProductOrderingPath = UrlPathUtils.removeInitialSlash(tmf622ProductOrderingPath);
		}
		
		if (tmf678CustomerBillPath.startsWith("/")) {
			tmf678CustomerBillPath = UrlPathUtils.removeInitialSlash(tmf678CustomerBillPath);
		}
		if (tmf637ProductInventoryPath.startsWith("/")) {
			tmf637ProductInventoryPath = UrlPathUtils.removeInitialSlash(tmf637ProductInventoryPath);
		}
	}

}
