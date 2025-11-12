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
import it.eng.dome.brokerage.billing.dto.BillingRequestDTO;
import it.eng.dome.brokerage.billing.dto.BillingResponseDTO;
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
    
	 /**
     * The POST /billing/bill REST API is invoked to calculate the bill of a Product (TMF637-v4) without taxes.
     * 
     * @param BillingRequestDTO The DTO contains information about the Product (TMF637-v4), the TimePeriod (TMF678-v4) and the list of ProductPrice (TMF637-v4) for which the bill must be calculated.
     * @return An AppliedCustomerBillingRate as a Json without taxes
     * @throws Throwable If an error occurs during the calculation of the bill for the Product
     */ 
    @RequestMapping(value = "/bill", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<List<Invoice>> calculateBill(@RequestBody BillingRequestDTO billRequestDTO){
		logger.info("Received request for calculating bill...");
		
		Product product;
		TimePeriod billingPeriod;
				
		try {
			
			// 1) retrieve the Product and the billingPeriod from the BillingRequestDTO
			product = billRequestDTO.getProduct();
			
			if (product == null) {
				throw new BillingBadRequestException("Missing the instance of Product in the BillingRequestDTO");
			}
			
			billingPeriod = billRequestDTO.getTimePeriod();
			if (billingPeriod == null) {
				throw new BillingBadRequestException("Missing the instance of billingPeriod in the BillingRequestDTO");
			}
			
			logger.info("Product with ID: {}", product.getId());
			logger.info("BillingPeriod with startDate: {} and endDate: {}", billingPeriod.getStartDateTime(), billingPeriod.getEndDateTime());
			
			//BillingResponseDTO billResponseDTO = billService.calculateBill(product, billingPeriod);
			List<Invoice> invoices=billService.calculateBill(product, billingPeriod);
			
			//return ResponseEntity.ok(billResponseDTO);
			return ResponseEntity.ok(invoices);

		} catch (Exception e) {
			logger.error("Error: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}
    
    /*private CustomerBill generateCustomerBill(List<AppliedCustomerBillingRate> acbrs) {
    	CustomerBill cb=new CustomerBill();
    	
    	//Money ammountDue=new Money();
    	Float totalTaxExcludedAmount=0f;
    	//TODO Add TAXITEM
    	for(AppliedCustomerBillingRate acbr:acbrs) {
    		totalTaxExcludedAmount += acbr.getTaxExcludedAmount().getValue();
		}
    	
    	Money totalTaxExcludedAmountMoney=new Money();
    	totalTaxExcludedAmountMoney.setUnit("EUR");
    	totalTaxExcludedAmountMoney.setValue(totalTaxExcludedAmount);
    	
    	cb.setTaxExcludedAmount(totalTaxExcludedAmountMoney);
    	cb.setAmountDue(totalTaxExcludedAmountMoney);
    	cb.setRemainingAmount(totalTaxExcludedAmountMoney);
    	
    	return cb;
    }*/

}
