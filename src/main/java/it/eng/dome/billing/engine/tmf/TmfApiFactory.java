package it.eng.dome.billing.engine.tmf;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import it.eng.dome.tmforum.tmf620.v4.ApiClient;
import it.eng.dome.tmforum.tmf620.v4.Configuration;

@Component(value = "tmfApiFactory")
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public final class TmfApiFactory implements InitializingBean {
	@Value( "${tmforumapi.tmf620_catalog_base}" )
	private String tmf620ProductCatalogBase;
	
	@Value( "${tmforumapi.tmf620_catalog_path}" )
	private String tmf620ProductCatalogPath;
		
	@Value( "${tmforumapi.tmf622_ordering_base}" )
	private String tmf622ProductOrderingBase;

	@Value( "${tmforumapi.tmf622_ordering_path}" )
	private String tmf622ProductOrderingPath;

	
	public ApiClient getTMF620ProductCatalogApiClient() {
		final ApiClient apiClient = Configuration.getDefaultApiClient();
		apiClient.setBasePath(tmf620ProductCatalogBase + "/" + tmf620ProductCatalogPath);
		return apiClient;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(!StringUtils.isBlank(tmf620ProductCatalogBase), "Billing Engine not properly configured. tmf620_catalog_base property has no value.");
		Assert.state(!StringUtils.isBlank(tmf620ProductCatalogPath), "Billing Engine not properly configured. tmf620_catalog_path property has no value.");
		Assert.state(!StringUtils.isBlank(tmf622ProductOrderingBase), "Billing Engine not properly configured. tmf622_ordering_base property has no value.");
		Assert.state(!StringUtils.isBlank(tmf622ProductOrderingPath), "Billing Engine not properly configured. tmf622_ordering_path property has no value.");
			
		if (tmf620ProductCatalogBase.endsWith("/")) tmf620ProductCatalogBase = removeFinalSlash(tmf620ProductCatalogBase);
		if (tmf622ProductOrderingBase.endsWith("/")) tmf622ProductOrderingBase = removeFinalSlash(tmf622ProductOrderingBase);
		
		if (tmf620ProductCatalogPath.startsWith("/")) tmf620ProductCatalogPath = removeInitialSlash(tmf620ProductCatalogPath);
		if (tmf622ProductOrderingPath.startsWith("/")) tmf622ProductOrderingPath = removeInitialSlash(tmf622ProductOrderingPath);
	}

	public it.eng.dome.tmforum.tmf622.v4.ApiClient getTMF622ProductOrderingApiClient() {
		final it.eng.dome.tmforum.tmf622.v4.ApiClient apiClient = it.eng.dome.tmforum.tmf622.v4.Configuration.getDefaultApiClient();
		apiClient.setBasePath(tmf622ProductOrderingBase + "/" + tmf622ProductOrderingPath);
		return apiClient;
	}
	
	private String removeFinalSlash(String s) {
		while (s.endsWith("/"))
			s = s.substring(0, s.length() - 1);

		return s;
	}
	
	private String removeInitialSlash(String s) {
		while (s.startsWith("/"))
			s = s.substring(1);
		
		return s;
	}	
}
