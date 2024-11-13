package it.eng.dome.billing.engine.tmf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.tmforum.tmf620.v4.ApiClient;
import it.eng.dome.tmforum.tmf620.v4.Configuration;

@Component(value = "TmfApiServiceLocator")
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public final class TmfApiServiceLocator {
		
	@Value( "${tmforumapi.tmf620_catalog_url}" )
	private String tmf620ProductCatalogUrl;
	
	@Value( "${tmforumapi.tmf622_ordering_url}" )
	private String tmf622ProductOrderingUrl;
	
	public ApiClient getTMF620ProductCatalogApiClient() {
		final ApiClient apiClient = Configuration.getDefaultApiClient();
		apiClient.setBasePath(tmf620ProductCatalogUrl);
		return apiClient;
	}

	public it.eng.dome.tmforum.tmf622.v4.ApiClient getTMF622ProductOrderingApiClient() {
		final it.eng.dome.tmforum.tmf622.v4.ApiClient apiClient = it.eng.dome.tmforum.tmf622.v4.Configuration.getDefaultApiClient();
		apiClient.setBasePath(tmf622ProductOrderingUrl);
		return apiClient;
	}
}
