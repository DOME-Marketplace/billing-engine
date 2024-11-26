package it.eng.dome.billing.engine.controller;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.dome.billing.engine.price.PriceService;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;

@RestController
@RequestMapping("/price")
@Tag(name = "Price Preview Controller", description = "APIs to manage the price calculation for orders")
public class PricePreviewController {
	
	protected final Logger logger = LoggerFactory.getLogger(PricePreviewController.class);

	@Autowired
	protected PriceService priceService;
	
	/**
	 * Il calcolo va effettuato su un order prendendo in considerazioni i diversi 
	 * ProductOrderItem che lo compongono. Ogni ProductOrderItem genera un prezzo.
	 * 
	 * I casi possibili sono: 
	 * 1) ProductOrderItem singolo con isBundle = false. In questo caso è necessario recuperare il 
	 *    ProductOfferingPrice riferito da productOrderItem.itemPrice. Una volta recuperato bisogna
	 *    verificare se questo POP ha uno sconto, e successivamente bisogna moltiplicare per la 
	 *    quantità
	 * 2) ProductOrderItem multiplo con isBundle = true. In questo caso è necessario recuperare ogni
	 *    ProductOrderItem collegato e seguire i passi descritti al punto 1.
	 * 
	 * @param orderJson
	 * @return
	 * @throws Throwable 
	 */
    @RequestMapping(value = "/order", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> calculateOrderPrice(@RequestBody String orderJson) throws Throwable {
		logger.info("Received request for calculating order price...");
		Assert.state(!StringUtils.isBlank(orderJson), "Missing the instance of ProductOrder in the request body");
		try {
			// 1) parse request body to ProductOrder
			ProductOrder order = ProductOrder.fromJson(orderJson);
			// 2) calculate order price
			priceService.calculateOrderPrice(order);
			// 3) return updated ProductOrder
			return new ResponseEntity<String>(order.toJson(), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			// Java exception is converted into HTTP status code by the ControllerExceptionHandler
			throw (e.getCause() != null) ? e.getCause() : e;
		}
	}
	

}
