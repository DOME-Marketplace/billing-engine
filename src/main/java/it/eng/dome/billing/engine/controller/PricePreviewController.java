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
import it.eng.dome.billing.engine.service.PricePreviewService;
import it.eng.dome.brokerage.billing.dto.BillingPreviewRequestDTO;
import it.eng.dome.brokerage.billing.dto.BillingRequestDTO;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf635.v4.model.Usage;

@RestController
//@RequestMapping("/price")
@Tag(name = "Price Preview Controller", description = "APIs to manage the price calculation for orders")
public class PricePreviewController {
	
	protected final Logger logger = LoggerFactory.getLogger(PricePreviewController.class);

	@Autowired
	protected PricePreviewService priceService;
    
    /**
     * The POST /billing/previewPrice REST API is invoked to calculate the price preview of a {@link ProductOrder} without taxes
     * 
     * @param billPreviewRequestDTO A {@link BillingPreviewRequestDTO} containing information about the ProductOrder and, in case of a pay-per-use scenario, the list of simulate {@link Usage}
     * @return The ProductOrder with the calculated prices
     * @throws ApiException if some error occurs retrieving the TMF620 entities
     * @throws {@link BillingEngineValidationException} if some error occurs during the validation of TMForum entities
     * @throws IllegalArgumentException if some illegal argument is provided in input
     * @throws {@link BillingBadRequestException} if the {@link BillingRequestDTO} is not well formed
     */ 
	@RequestMapping(value = "/billing/previewPrice", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<ProductOrder> calculateOrderPrice(@RequestBody BillingPreviewRequestDTO billPreviewRequestDTO) throws IllegalArgumentException, BillingEngineValidationException, ApiException, BillingBadRequestException{
		
		ProductOrder order;
		List<Usage> usageData;

		order = billPreviewRequestDTO.getProductOrder();
		
		if (order == null)
			throw new BillingBadRequestException("Missing the instance of ProductOrder in the BillingPreviewRequestDTO");
		
		logger.info("Received request for calculating price preview for the order {}",order.getId());
		
		usageData = billPreviewRequestDTO.getUsage();
		
		if(usageData == null || (usageData !=null & usageData.isEmpty())) {
			logger.info("Usage data in the BillingPreviewRequestDTO is null or empty");
			usageData =null;
			
		}else {
			logger.info("found {} Usage data in the BillingPreviewRequestDTO", usageData.size());		
		}
		try {
		
		// 2) calculate order price
		ProductOrder updatedOrder=priceService.calculateOrderPrice(order,usageData);

		// 3) return updated ProductOrder
		return ResponseEntity.ok(updatedOrder);
		} catch (Exception ex) {
			logger.error("Error: {}", ex.getMessage());		
			return ResponseEntity.badRequest().build();
		}
	}
}
