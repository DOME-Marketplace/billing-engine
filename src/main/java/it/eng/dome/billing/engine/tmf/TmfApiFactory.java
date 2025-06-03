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
	
	private it.eng.dome.tmforum.tmf620.v4.ApiClient apiClientTmf620;
	private it.eng.dome.tmforum.tmf622.v4.ApiClient apiClientTmf622;
	private it.eng.dome.tmforum.tmf678.v4.ApiClient apiClientTmf678;
	

	
	public it.eng.dome.tmforum.tmf620.v4.ApiClient getTMF620ProductCatalogApiClient() {
		if (apiClientTmf620 == null) {
			apiClientTmf620 = it.eng.dome.tmforum.tmf620.v4.Configuration.getDefaultApiClient();
			if (tmfEnvoy) {
				// usage of envoyProxy to access on TMForum APIs
				apiClientTmf620.setBasePath(tmfEndpoint + "/" + tmf620ProductCatalogPath);
			}else {
				// use direct access on specific TMForum APIs software	
				apiClientTmf620.setBasePath(tmfEndpoint + TMF_ENDPOINT_CONCAT_PATH + "product-catalog" + "." + tmfNamespace + "." + tmfPostfix + ":" + tmfPort + "/" + tmf620ProductCatalogPath);		
			}
			log.debug("Invoke Product Catalog API at endpoint: " + apiClientTmf620.getBasePath());
		}
		return apiClientTmf620;
	}	
	
	public it.eng.dome.tmforum.tmf622.v4.ApiClient getTMF622ProductOrderingApiClient() {
		if (apiClientTmf622 == null) {
			apiClientTmf622 = it.eng.dome.tmforum.tmf622.v4.Configuration.getDefaultApiClient();
			if (tmfEnvoy) {
				// usage of envoyProxy to access on TMForum APIs
				apiClientTmf622.setBasePath(tmfEndpoint + "/" + tmf622ProductOrderingPath);
			}else {
				// use direct access on specific TMForum APIs software	
				apiClientTmf622.setBasePath(tmfEndpoint + TMF_ENDPOINT_CONCAT_PATH + "product-ordering-management" + "." + tmfNamespace + "." + tmfPostfix + ":" + tmfPort + "/" + tmf622ProductOrderingPath);		
			}		
			log.debug("Invoke Product Ordering API at endpoint: " + apiClientTmf622.getBasePath());
		}
		return apiClientTmf622;
	}
	
	public it.eng.dome.tmforum.tmf678.v4.ApiClient getTMF678CustomerBillApiClient() {
		if (apiClientTmf678 == null) {
			apiClientTmf678 = it.eng.dome.tmforum.tmf678.v4.Configuration.getDefaultApiClient();
			if (tmfEnvoy) {
				// usage of envoyProxy to access on TMForum APIs
				apiClientTmf678.setBasePath(tmfEndpoint + "/" + tmf678CustomerBillPath);
			}else {
				// use direct access on specific TMForum APIs software	
				apiClientTmf678.setBasePath(tmfEndpoint + TMF_ENDPOINT_CONCAT_PATH + "customer-bill-management" + "." + tmfNamespace + "." + tmfPostfix + ":" + tmfPort + "/" + tmf678CustomerBillPath);		
			}		
			log.debug("Invoke Customer Billing API at endpoint: " + apiClientTmf678.getBasePath());
		}
		return apiClientTmf678;
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
			
		if (tmfEndpoint.endsWith("/")) {
			tmfEndpoint = removeFinalSlash(tmfEndpoint);		
		}
		
		if (tmf620ProductCatalogPath.startsWith("/")) {
			tmf620ProductCatalogPath = removeInitialSlash(tmf620ProductCatalogPath);
		}
		
		if (tmf622ProductOrderingPath.startsWith("/")) {
			tmf622ProductOrderingPath = removeInitialSlash(tmf622ProductOrderingPath);
		}
		
		if (tmf678CustomerBillPath.startsWith("/")) {
			tmf678CustomerBillPath = removeInitialSlash(tmf678CustomerBillPath);
		}
	}
	
	private String removeFinalSlash(String s) {
		String path = s;
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);

		return path;
	}
	
	private String removeInitialSlash(String s) {
		String path = s;
		while (path.startsWith("/")) {
			path = path.substring(1);
		}				
		return path;
	}	
}
