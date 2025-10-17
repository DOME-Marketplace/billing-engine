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
import it.eng.dome.billing.engine.price.PriceService;
import it.eng.dome.brokerage.billing.dto.BillingPreviewRequestDTO;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;

@RestController
//@RequestMapping("/price")
@Tag(name = "Price Preview Controller", description = "APIs to manage the price calculation for orders")
public class PricePreviewController {
	
	protected final Logger logger = LoggerFactory.getLogger(PricePreviewController.class);

	@Autowired
	protected PriceService priceService;
    
    /**
     * The POST /billing/previewPrice REST API is invoked to calculate the price preview of a ProcuctOrder (TMF622-v4).
     * 
     * @param billPreviewRequestDTO The DTO contains information about the ProductOrder (TMF622-v4) and, in case of pay per use scenario, the list of simulate Usage (TMF635-v4) for which the price preview must be calculated.
     * @return The ProductOrder as a Json string with prices
     * @throws Exception If an error occurs during the calculation of the ProductOrder price preview
     */ 
	@RequestMapping(value = "/billing/previewPrice", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> calculateOrderPrice(@RequestBody BillingPreviewRequestDTO billPreviewRequestDTO) throws Exception {
		logger.info("Received request for calculating price preview for the order...");
		
		ProductOrder order;
		List<Usage> usageData;

		try {
			// 1) retrieve the ProductOrder and the Usage from the BillingPreviewRequestDTO
			order = billPreviewRequestDTO.getProductOrder();
			
			if (order == null)
				throw new BillingBadRequestException("Missing the instance of ProductOrder in the BillingPreviewRequestDTO");
			
			usageData = billPreviewRequestDTO.getUsage();
			
			if(usageData == null || (usageData !=null & usageData.isEmpty())) {
				logger.info("Usage data in the BillingPreviewRequestDTO is null or empty");
				usageData =null;
				
			}else {
				logger.info("found {} Usage data in the BillingPreviewRequestDTO", usageData.size());		
			}
			
			// 2) calculate order price
			priceService.calculateOrderPrice(order,usageData);
			
			// 3) return updated ProductOrder
			return new ResponseEntity<String>(order.toJson(), HttpStatus.OK);
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
			// Java exception is converted into HTTP status code by the ControllerExceptionHandler
			throw new Exception(e); //throw (e.getCause() != null) ? e.getCause() : e;
		}
	}
	
    /*@RequestMapping(value = "/billing/previewPrice", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> calculateOrderPrice(@RequestBody String orderJson) throws Exception {
		logger.info("Received request for calculating order price...");
		Assert.state(!StringUtils.isBlank(orderJson), "Missing the instance of ProductOrder in the request body");
		try {
			// 1) parse request body to ProductOrder
			ProductOrder order = ProductOrder.fromJson(orderJson);
			// 2) calculate order price
			priceService.calculateOrderPrice(order);
			// 3) return updated ProductOrder
			return new ResponseEntity<String>(order.toJson(), HttpStatus.OK);
		}catch (Exception e) {
			logger.error(e.getMessage(), e);
			// Java exception is converted into HTTP status code by the ControllerExceptionHandler
			throw new Exception(e); //throw (e.getCause() != null) ? e.getCause() : e;
		}
	}*/
	

}
