package it.eng.dome.billing.engine.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import it.eng.dome.billing.engine.bill.BillService;
import it.eng.dome.brokerage.billing.dto.BillingRequestDTO;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf637.v4.model.ProductPrice;
import it.eng.dome.tmforum.tmf678.v4.JSON;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;

@RestController
@RequestMapping("/billing")
@Tag(name = "Billing Controller", description = "APIs to manage the calculation og the bills")
public class BillController {
	
	protected final Logger logger = LoggerFactory.getLogger(BillController.class);
	
	@Autowired
	protected BillService billService;
    
    @RequestMapping(value = "/bill", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> calculateBill(@RequestBody BillingRequestDTO billRequestDTO) throws Throwable {
		logger.info("Received request for calculating bill...");
		
		List<AppliedCustomerBillingRate> appliedCustomerBillingRateList;
		Product product;
		TimePeriod tp;
		List<ProductPrice> ppList;
		
		try {
			
			// 1) retrieve the Product, TimePeriod and ProductPrice list from the BillingRequestDTO
			product=billRequestDTO.getProduct();
			Assert.state(!Objects.isNull(product),  "Missing the instance of Product in the BillingRequestDTO");
			
			tp=billRequestDTO.getTimePeriod();
			Assert.state(!Objects.isNull(tp),  "Missing the instance of TimePeriod in the BillingRequestDTO");
			
			ppList=billRequestDTO.getProductPrice();
			Assert.state(!Objects.isNull(ppList),  "Missing the instance of ProductPrice list in the BillingRequestDTO");
			
			logger.info("for Product with ID: "+product.getId());
			logger.info("for TimePeriod with startDate: "+tp.getStartDateTime()+" and endDate: "+tp.getEndDateTime());
			logger.info("for ProductPrice list with "+ppList.size()+" element(s)");
			
			// 2) calculate the list of the AppliedCustomerBillingRates for the Product, TimePeriod and ProductPrice List
			appliedCustomerBillingRateList=billService.calculateBill(product,tp, ppList);
			
			// 3) return the AppliedCustomerBillingRate List
			return new ResponseEntity<String>(JSON.getGson().toJson(appliedCustomerBillingRateList), HttpStatus.OK);
			 

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			// Java exception is converted into HTTP status code by the ControllerExceptionHandler
			throw new Exception(e); //throw (e.getCause() != null) ? e.getCause() : e;
		}
	}
    
 

}
