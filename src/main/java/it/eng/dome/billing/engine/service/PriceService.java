package it.eng.dome.billing.engine.service;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.model.BillingProductOrder;
import it.eng.dome.tmforum.tmf620.v4.ApiClient;
import it.eng.dome.tmforum.tmf620.v4.Configuration;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingPriceApi;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;

@Component(value = "PriceService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PriceService {
	private static final String TMF_SERVER = "http://localhost";
	private static final String CATALOG_PORT = "8100";
	private static final String CATALOG_V4_PATH = "tmf-api/productCatalogManagement/v4";
	/**
	 * 
	 * @param pOrder: the order has been previously validated by the controller
	 * @return
	 */
	public void setOrderTotalPrice(ProductOrder order) throws Exception {
		final ApiClient apiClient = Configuration.getDefaultApiClient();
		apiClient.setBasePath(TMF_SERVER + ":" + CATALOG_PORT + "/" + CATALOG_V4_PATH);
	    final var popApi = new ProductOfferingPriceApi(apiClient);

	    final BillingProductOrder billingProductOrder = new BillingProductOrder(order, popApi);
	    billingProductOrder.setOrderPrice();
	}

}
