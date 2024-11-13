package it.eng.dome.billing.engine.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.billing.engine.model.BillingProductOrder;
import it.eng.dome.billing.engine.tmf.TmfApiServiceLocator;
import it.eng.dome.tmforum.tmf620.v4.ApiClient;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingPriceApi;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;

@Component(value = "PriceService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PriceService {
	
	@Autowired
	private TmfApiServiceLocator tmfApiServiceLocator;
	/**
	 * 
	 * @param pOrder: the order has been previously validated by the controller
	 * @return
	 */
	public OrderPrice calculateOrderPrice(ProductOrder order) throws Exception {
		final ApiClient apiClient = tmfApiServiceLocator.getTMF620ProductCatalogApiClient();
	    final var popApi = new ProductOfferingPriceApi(apiClient);

	    final BillingProductOrder billingProductOrder = new BillingProductOrder(order, popApi);
	    return billingProductOrder.calculateOrderPrice();
	}

}
