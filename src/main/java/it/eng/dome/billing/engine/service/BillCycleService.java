package it.eng.dome.billing.engine.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.eng.dome.billing.engine.exception.BillingBadRequestException;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.brokerage.model.BillCycle;
import it.eng.dome.brokerage.model.PriceType;
import it.eng.dome.brokerage.model.RecurringChargePeriod;
import it.eng.dome.brokerage.model.RecurringPeriod;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;

@Service
public class BillCycleService{
	
	private final static Logger Logger = LoggerFactory.getLogger(BillCycleService.class);
	
	/**
	 * Calculates all the {@link TimePeriod}) representing the billingPeriod(s) of the BillCycle, considering a list of {@link OffsetDateTime} end dates and an {@link OffsetDateTime} activation date 
	 *
	 * @param billingPeriodEndDates List of {@link OffsetDateTime} representing the end dates of the billingPeriod(s)
	 * @param activationDate An {@link OffsetDateTime} representing the activation date from which the billingPeriod(s) are calculated
	 * @return A list of {@link TimePeriod} representing the billingPeriod(s) of the BillCycle
	 */
	public List<TimePeriod> calculateBillingPeriods(@NotNull List<OffsetDateTime> billingPeriodEndDates, @NotNull OffsetDateTime activationDate){
			
		Logger.info("Calculation of the billingPeriods from activationDate {}",activationDate);
		
		List<TimePeriod> billingPeriods=new ArrayList<TimePeriod>();
		
		if(!billingPeriodEndDates.isEmpty()) {
			
			List<OffsetDateTime> temp=new ArrayList<OffsetDateTime>(billingPeriodEndDates);
			// Sort dates
			//Collections.sort(billingPeriodEndDates);
			
			try {
			    Collections.sort(temp);
			} catch (Exception e) {
			    e.printStackTrace();
			}
			
			OffsetDateTime startDate=activationDate;
			
			for(OffsetDateTime endDate: billingPeriodEndDates) {
				TimePeriod tp=new TimePeriod();
				tp.setStartDateTime(startDate);
				tp.setEndDateTime(endDate);
				
				billingPeriods.add(tp);
				
				startDate=endDate.plusDays(1);
			}
		}
		
		return billingPeriods;
	}
	
	/**
	 * Calculates the billingPeriod END dates of the BillCycle, included from an activation {@link OffsetDateTime} and a limit {@link OffsetDateTime}, according to the specified {@link RecurringChargePeriod} (e.g., 5 DAY, 2 WEEK; 1 MONTH, 1 YEAR) 
	 * 
	 * @param recurringChargePeriod A {@link RecurringChargePeriod} specifying the recurringChargePeriodType and recurringChargePeriodLength  
	 * @param activationDate An {@link OffsetDateTime} representing a start date from which the billingPeriod end dates are calculated
	 * @param limitDate An {@link OffsetDateTime} representing a limit date to stop the calculation of billingPeriod end dates
	 * @return The list of {@link OffsetDateTime} representing all the billingPeriod END dates of the BillCycle that fall between the activation and limit dates
	 * @throws IllegalArgumentException If the {@link RecurringChargePeriod} contains unexpected values
	 */
	public List<OffsetDateTime> calculateBillingPeriodEndDates(@NotNull RecurringChargePeriod recurringChargePeriod, @NotNull OffsetDateTime activationDate, @NotNull OffsetDateTime limitDate) throws IllegalArgumentException{
		
		Logger.info("Calculation of the billingPeriod end dates for recurringPeriodLenght '{}' and recurringPeriodType '{}' and activation date '{}'",
				recurringChargePeriod.getRecurringChargePeriodLenght(),recurringChargePeriod.getRecurringChargePeriodType(), activationDate);
		
		List<OffsetDateTime> billPeriodEndDates=new ArrayList<OffsetDateTime>();
		
		RecurringPeriod billingPeriodType=recurringChargePeriod.getRecurringChargePeriodType();
		Integer billingPeriodLength=recurringChargePeriod.getRecurringChargePeriodLenght();
		
		if(billingPeriodType!=null && billingPeriodLength!=null && billingPeriodLength>0) {
			Stream<OffsetDateTime> streamData=Stream.empty();
			
			switch (recurringChargePeriod.getRecurringChargePeriodType()) {
			case DAY: {
				
				streamData = Stream.iterate(
		                activationDate.plusDays(recurringChargePeriod.getRecurringChargePeriodLenght()- 1),          
		                d -> d.plusDays(recurringChargePeriod.getRecurringChargePeriodLenght())                    
		        );
		       break;
			}
			case WEEK: {
				
				streamData = Stream.iterate(
		        		activationDate.plusDays((7 * recurringChargePeriod.getRecurringChargePeriodLenght())-1),          
		                d -> d.plusDays(7 * recurringChargePeriod.getRecurringChargePeriodLenght())                    
		        );
		       break;
			}
			case MONTH: {

				streamData = Stream.iterate(
				        1, i -> i + 1
				).map(i -> activationDate.plusMonths(i * recurringChargePeriod.getRecurringChargePeriodLenght()).minusDays(1));
		       break;
			}
			case YEAR: {

				streamData = Stream.iterate(
				        1, i -> i + 1
				).map(i -> activationDate.plusYears(i * recurringChargePeriod.getRecurringChargePeriodLenght()).minusDays(1));
		       break;
			}
			default:
				throw new IllegalArgumentException("Error in the RecurringChargePeriod: unexpected value for billingPeriodType");
			}
			
			billPeriodEndDates=streamData.takeWhile(d -> d.isBefore(limitDate) || d.isEqual(limitDate)).toList();
			if(activationDate.isAfter(limitDate))
		    	   Logger.warn("activationDate '{}' is after limitDate '{}'", activationDate, limitDate);
		    Logger.info("Per {} billingPeriod END dates:{}",billingPeriodType,billPeriodEndDates);
		    
		    return billPeriodEndDates;
			
			
		}else {
			throw new IllegalArgumentException("Error in the RecurringChargePeriod: billingPeriodType must not be null, billingPeriodLength must be greater than 0");
		}
	}
	
	/**
	 * Checks if a {@link OffsetDateTime} bill date falls within a {@link TimePeriod} representing the billingPeriod
	 * 
	 * @param billingDate A {@link OffsetDateTime} bill date to check
	 * @param billingPeriod A {@link TimePeriod}  representing the billingPeriod
	 * @return true if the bill date falls within the billingPeriod, false otherwise
	 */
	public boolean isBillDateWithinBillingPeriod(@NotNull OffsetDateTime billingDate, @NotNull TimePeriod billingPeriod) {
		return (!billingDate.isBefore(billingPeriod.getStartDateTime())) && (!billingDate.isAfter(billingPeriod.getEndDateTime()));
	}
	
	
	public List<OffsetDateTime> calculateBillDates(@NotNull ProductOfferingPrice pop, @NotNull Product prod, @NotNull OffsetDateTime limitDate) throws BillingBadRequestException{
		Logger.info("Calculating billDate(s) for ProductOfferingPrice '{}' with priceType '{}", pop.getId(),pop.getPriceType());
		
		List<OffsetDateTime> billDates=new ArrayList<OffsetDateTime>();
		if(ProductOfferingPriceUtils.isPriceTypeOneTime(pop))
			billDates.add(prod.getStartDate());
		
		if(ProductOfferingPriceUtils.isPriceTypeRecurring(pop)) {
			List<OffsetDateTime> billingPeriodEndDates=this.calculateBillingPeriodEndDates(ProductOfferingPriceUtils.getRecurringChargePeriod(pop), prod.getStartDate(), limitDate);
			List<TimePeriod> billingPeriods= this.calculateBillingPeriods(billingPeriodEndDates, prod.getStartDate());
			if(ProductOfferingPriceUtils.isPriceTypeRecurringPrepaid(pop))
				billDates=billingPeriods.stream()
					.map(TimePeriod::getStartDateTime)
					.toList();
			if(ProductOfferingPriceUtils.isPriceTypeRecurringPostpaid(pop))
				billDates=billingPeriods.stream()
					.map(TimePeriod::getEndDateTime)
					.toList();
		}
		if(ProductOfferingPriceUtils.isPriceTypeCustom(pop))
			throw new BillingBadRequestException(String.format("Error: Not possible to calculate billDates for PriceType '%s' in ProductOfferingPrice '%s' ", PriceType.CUSTOM.toString(), pop.getId()));
		
		
		return billDates;
		
	}
	
	public List<BillCycle> getBillCycles(@NotNull ProductOfferingPrice pop, @NotNull Product prod, @NotNull TimePeriod billingPeriod) throws BillingBadRequestException{
		List<BillCycle> billCycles=new ArrayList<BillCycle>();
		
		List<OffsetDateTime> billDates=this.calculateBillDates(pop, prod, billingPeriod.getEndDateTime());
		for(OffsetDateTime billDate: billDates) {
			
			if(this.isBillDateWithinBillingPeriod(billDate, billingPeriod)) {
				BillCycle billCycle=new BillCycle();
				billCycle.setBillDate(billDate);
				billCycle.setBillingPeriod(billingPeriod);
				billCycles.add(billCycle);
			}	
		}
		
		return billCycles;
				
	}

}
