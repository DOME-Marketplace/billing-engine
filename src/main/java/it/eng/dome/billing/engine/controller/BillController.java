package it.eng.dome.billing.engine.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.service.BillingEngineService;
import it.eng.dome.brokerage.api.ProductInventoryApis;
import it.eng.dome.brokerage.billing.dto.BillingRequestDTO;
import it.eng.dome.brokerage.model.Invoice;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;

@RestController
@RequestMapping("/billing")
@Tag(name = "Billing Controller", description = "APIs to manage the calculation og the bills")
public class BillController {
	
	protected final Logger logger = LoggerFactory.getLogger(BillController.class);
	
	@Autowired
	protected BillingEngineService billService;
	
	@Autowired
	private ProductInventoryApis productInventoryApis;
    
	 /**
     * The POST /billing/bill REST API is invoked to calculate the bill of a {@link Product} without taxes.
     * 
     * @param billRequestDTO A {@link BillingRequestDTO} containing information about the identifier of the {@link Product} and of a {@link TimePeriod} representing the billingPeriod for which the bill must be must be calculated.
     * @return  A list of {@link Invoice} 
     */ 
    @RequestMapping(value = "/bill", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<List<Invoice>> calculateBill(@RequestBody BillingRequestDTO billRequestDTO){
		logger.info("Received request for calculating bill...");
		
		Product product;
		TimePeriod billingPeriod;
				
		try {
			
			// 1) retrieve the Product and the billingPeriod from the BillingRequestDTO
			product=productInventoryApis.getProduct(billRequestDTO.getProductId(), null);
			
			if (product == null) {
				throw new BillingBadRequestException("Missing the instance of Product in the BillingRequestDTO");
			}
			
			billingPeriod = billRequestDTO.getBillingPeriod();
			if (billingPeriod == null) {
				throw new BillingBadRequestException("Missing the instance of billingPeriod in the BillingRequestDTO");
			}
			
			logger.info("Product with ID: {}", product.getId());
			logger.info("BillingPeriod with startDate: {} and endDate: {}", billingPeriod.getStartDateTime(), billingPeriod.getEndDateTime());
			
			List<Invoice> invoices=billService.calculateBill(product, billingPeriod);
			
			return ResponseEntity.ok(invoices);

		} catch (Exception e) {
			logger.error("Error: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

}
