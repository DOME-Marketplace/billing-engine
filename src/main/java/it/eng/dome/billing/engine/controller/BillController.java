package it.eng.dome.billing.engine.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.service.BillingEngineService;
import it.eng.dome.billing.engine.validator.TMFEntityValidator;
import it.eng.dome.brokerage.api.ProductInventoryApis;
import it.eng.dome.brokerage.billing.dto.BillingRequestDTO;
import it.eng.dome.brokerage.billing.dto.InstantBillingRequestDTO;
import it.eng.dome.brokerage.model.Invoice;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
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
	
	@Autowired
	private TMFEntityValidator tmfEntityValidator;
    
	 /**
     * The REST API POST /billing/bill REST API is invoked to calculate the bill of a {@link Product} without taxes.
     * 
     * @param billRequestDTO A {@link BillingRequestDTO} containing information about the identifier of the {@link Product} and of a {@link TimePeriod} representing the billingPeriod for which the bill must be must be calculated.
     * @return  A list of {@link Invoice} 
	 * @throws BillingBadRequestException if the {@link BillingRequestDTO} is not well formed
	 * @throws ApiException if some error occurs retrieving the TMF620 entities
	 * @throws {@link BillingEngineValidationException} if some error occurs during the validation of TMForum entities
	 * @throws IllegalArgumentException  if some illegal argument is provided in input
	 * @throws it.eng.dome.tmforum.tmf637.v4.ApiException if some error occurs retrieving the TMF637 entities
     */ 
    @RequestMapping(value = "/bill", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<List<Invoice>> calculateBill(@RequestBody BillingRequestDTO billRequestDTO) throws IllegalArgumentException, BillingEngineValidationException, ApiException, BillingBadRequestException, it.eng.dome.tmforum.tmf637.v4.ApiException{
		logger.info("Received request for calculating bill...");
		
		Product product;
		TimePeriod billingPeriod;
				

		// 1) retrieve the Product and the billingPeriod from the BillingRequestDTO
		product=productInventoryApis.getProduct(billRequestDTO.getProductId(), null);
		
		if (product == null) {
			throw new BillingBadRequestException("Missing the instance of Product in the BillingRequestDTO");
		}
		
		billingPeriod = billRequestDTO.getBillingPeriod();
		if (billingPeriod == null) {
			throw new BillingBadRequestException("Missing the instance of billingPeriod in the BillingRequestDTO");
		}
		
		tmfEntityValidator.validateBillingPeriod(billingPeriod);
		
		logger.info("Product with ID: {}", product.getId());
		logger.info("BillingPeriod with startDate: {} and endDate: {}", billingPeriod.getStartDateTime(), billingPeriod.getEndDateTime());
		
		List<Invoice> invoices=billService.calculateBill(product, billingPeriod);
		
		return ResponseEntity.ok(invoices);
	}
    
    /**
     * The REST API POST /billing/instantBill REST API is invoked to calculate the bill of a {@link Product} without taxes in a specified date.
     * This REST API is used to manage the scenario of one-time and recurring-prepaid bills required during the ordering phase (i.e., the Product is not yet in the Product Inventory).
     * 
     * @param instantBillRequestDTO A {@link InstantBillingRequestDTO} containing information about the {@link Product} and the date for which the bill must be must be calculated.
     * @return  A list of {@link Invoice} 
	 * @throws BillingBadRequestException if the {@link InstantBillingRequestDTO} is not well formed
	 * @throws ApiException if some error occurs retrieving the TMF620 entities
	 * @throws {@link BillingEngineValidationException} if some error occurs during the validation of TMForum entities
	 * @throws IllegalArgumentException  if some illegal argument is provided in input
	 * @throws it.eng.dome.tmforum.tmf637.v4.ApiException if some error occurs retrieving the TMF637 entities
     */ 
    @RequestMapping(value = "/instantBill", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<List<Invoice>> calculateBill(@RequestBody InstantBillingRequestDTO instantBillRequestDTO) throws IllegalArgumentException, BillingEngineValidationException, ApiException, BillingBadRequestException, it.eng.dome.tmforum.tmf637.v4.ApiException{
		logger.info("Received request for calculating instant bill...");
		
		if (instantBillRequestDTO.getProduct() == null) {
			throw new BillingBadRequestException("Missing the instance of Product in the InstantBillingRequestDTO");
		}
		if(instantBillRequestDTO.getDate()==null){
			throw new BillingBadRequestException("Missing the date in the InstantBillingRequestDTO");
		}

		logger.info("Product with ID: {}", instantBillRequestDTO.getProduct().getId());
		logger.info("Bill date: {}", instantBillRequestDTO.getDate());
		
		List<Invoice> invoices=billService.calculateBill(instantBillRequestDTO.getProduct(), instantBillRequestDTO.getDate());
		
		return ResponseEntity.ok(invoices);
	}

}
