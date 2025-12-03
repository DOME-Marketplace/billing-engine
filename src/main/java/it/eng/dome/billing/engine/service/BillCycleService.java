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
import it.eng.dome.billing.engine.utils.TMForumEntityUtils;
import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.brokerage.model.BillCycle;
import it.eng.dome.brokerage.model.PriceType;
import it.eng.dome.brokerage.model.RecurringChargePeriod;
import it.eng.dome.brokerage.model.RecurringPeriod;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;
import jakarta.validation.constraints.NotNull;

@Service
public class BillCycleService{
	
	private final static Logger Logger = LoggerFactory.getLogger(BillCycleService.class);
	
	/**
	 * Calculates all the {@link TimePeriod}) representing the billingPeriod(s) of the {@link BillCycle}, considering a {@link RecurringChargePeriod}, an {@link OffsetDateTime} activation date and a {@link OffsetDateTime} limitDate
	 *
	 * @param recurringChargePeriod A {@link RecurringChargePeriod} specifying the recurringChargePeriodType and recurringChargePeriodLength  
	 * @param activationDate An {@link OffsetDateTime} representing a start date from which the billingPeriod(s) are calculated
	 * @param limitDate An {@link OffsetDateTime} representing a limit date to stop the calculation of billingPeriod(s)
	 * 
	 * @return A list of {@link TimePeriod} representing the billingPeriod(s) of the BillCycle(s)
	 */
	public List<TimePeriod> calculateBillingPeriods(@NotNull RecurringChargePeriod recurringChargePeriod, @NotNull OffsetDateTime activationDate, @NotNull OffsetDateTime limitDate){
			
		Logger.debug("Calculation of the billingPeriods for RecurringChargePeriod {} from activationDate {} and limitDate {}",recurringChargePeriod.toString(),activationDate, limitDate);
		
		List<TimePeriod> billingPeriods=new ArrayList<TimePeriod>();
		
		List<OffsetDateTime> billingPeriodStartDates=this.calculateBillingPeriodStartDates(recurringChargePeriod, activationDate, limitDate);

		if(!billingPeriodStartDates.isEmpty()) {
			
			try {
			    Collections.sort(billingPeriodStartDates);
			} catch (Exception e) {
				Logger.error("Error: {}", e.getMessage());
			}

		    for (OffsetDateTime startDate : billingPeriodStartDates) {
		        OffsetDateTime endDate = calculateBillingPeriodEndDate(recurringChargePeriod, startDate);
		        
		        TimePeriod timePeriod=new TimePeriod();
		        timePeriod.setStartDateTime(startDate);
		        timePeriod.setEndDateTime(endDate);

		        billingPeriods.add(timePeriod);
		    }
		}
	    

	    return billingPeriods;
	}
	
	private OffsetDateTime calculateBillingPeriodEndDate(@NotNull RecurringChargePeriod recurringChargePeriod, @NotNull OffsetDateTime startDate) {

	    RecurringPeriod billingPeriodType = recurringChargePeriod.getRecurringChargePeriodType();
	    int billingPeriodLenght = recurringChargePeriod.getRecurringChargePeriodLenght();

	    switch (billingPeriodType) {
	        case DAY:
	            return startDate.plusDays(billingPeriodLenght - 1);

	        case WEEK:
	            return startDate.plusDays((long) billingPeriodLenght * 7 - 1);

	        case MONTH:
	            return startDate.plusMonths(billingPeriodLenght).minusDays(1);

	        case YEAR:
	            return startDate.plusYears(billingPeriodLenght).minusDays(1);

	        default:
	        	throw new IllegalArgumentException("Error in the RecurringChargePeriod: unexpected value for billingPeriodType");
	    }
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
		
		Logger.debug("Calculation of the billingPeriod end dates for recurringPeriodLenght '{}' and recurringPeriodType '{}' and activation date '{}'",
				recurringChargePeriod.getRecurringChargePeriodLenght(),recurringChargePeriod.getRecurringChargePeriodType(), activationDate);
		
		List<OffsetDateTime> endDates=new ArrayList<OffsetDateTime>();
		
		RecurringPeriod billingPeriodType=recurringChargePeriod.getRecurringChargePeriodType();
		Integer billingPeriodLength=recurringChargePeriod.getRecurringChargePeriodLenght();
		

	    if (activationDate.isAfter(limitDate)) {
	        Logger.warn("activationDate '{}' is after limitDate '{}'", activationDate, limitDate);
	        return endDates;
	    }

	    if (billingPeriodType == null || billingPeriodLength == null || billingPeriodLength <= 0) {
	    	throw new IllegalArgumentException("Error in the RecurringChargePeriod: billingPeriodType must not be null, billingPeriodLength must be greater than 0");
	    }
			
		Stream<OffsetDateTime> streamData=Stream.empty();
		
		switch (recurringChargePeriod.getRecurringChargePeriodType()) {
		case DAY: {
			
			streamData = Stream.iterate(
	                activationDate.plusDays(billingPeriodLength- 1),          
	                d -> d.plusDays(billingPeriodLength)                    
	        );
	       break;
		}
		case WEEK: {
			
			streamData = Stream.iterate(
	        		activationDate.plusDays((7 * billingPeriodLength)-1),          
	                d -> d.plusDays(7 * billingPeriodLength)                    
	        );
	       break;
		}
		case MONTH: {

			streamData = Stream.iterate(
			        1, i -> i + 1
			).map(i -> activationDate.plusMonths(i * billingPeriodLength).minusDays(1));
	       break;
		}
		case YEAR: {

			streamData = Stream.iterate(
			        1, i -> i + 1
			).map(i -> activationDate.plusYears(i * billingPeriodLength).minusDays(1));
	       break;
		}
		default:
			throw new IllegalArgumentException("Error in the RecurringChargePeriod: unexpected value for billingPeriodType");
		}
		
		endDates=streamData
	            .takeWhile(d -> !d.isAfter(limitDate))
	            .toList(); // immutable → create new ArrayList
	    
		Logger.debug("Per {} {} billingPeriod END dates:{}",billingPeriodLength,billingPeriodType,endDates);
	    
	    return new ArrayList<>(endDates);
	}
	
	/**
	 * Calculates the billingPeriod START dates of the BillCycle, included from an activation {@link OffsetDateTime} and a limit {@link OffsetDateTime}, according to the specified {@link RecurringChargePeriod} (e.g., 5 DAY, 2 WEEK; 1 MONTH, 1 YEAR) 
	 * 
	 * @param recurringChargePeriod A {@link RecurringChargePeriod} specifying the recurringChargePeriodType and recurringChargePeriodLength  
	 * @param activationDate An {@link OffsetDateTime} representing a start date from which the billingPeriod start dates are calculated
	 * @param limitDate An {@link OffsetDateTime} representing a limit date to stop the calculation of billingPeriod start dates
	 * @return The list of {@link OffsetDateTime} representing all the billingPeriod START dates of the BillCycle that fall between the activation and limit dates
	 * @throws IllegalArgumentException If the {@link RecurringChargePeriod} contains unexpected values
	 */
	public List<OffsetDateTime> calculateBillingPeriodStartDates(@NotNull RecurringChargePeriod recurringChargePeriod, @NotNull OffsetDateTime activationDate, @NotNull OffsetDateTime limitDate) {

	    RecurringPeriod billingPeriodType = recurringChargePeriod.getRecurringChargePeriodType();
	    Integer billingPeriodLength = recurringChargePeriod.getRecurringChargePeriodLenght();

	    List<OffsetDateTime> startDates = new ArrayList<>();

	    if (activationDate.isAfter(limitDate)) {
	        Logger.warn("activationDate '{}' is after limitDate '{}'", activationDate, limitDate);
	        return startDates;
	    }

	    if (billingPeriodType == null || billingPeriodLength == null || billingPeriodLength <= 0) {
	    	throw new IllegalArgumentException("Error in the RecurringChargePeriod: billingPeriodType must not be null, billingPeriodLength must be greater than 0");
	    }

	    Stream<OffsetDateTime> streamData = Stream.empty();

	    switch (billingPeriodType) {
	        case DAY: {
	            // start date for period N = activationDate + (N-1) * length DAYS
	            streamData = Stream.iterate(
	                    activationDate,                                      // first start date
	                    d -> d.plusDays(billingPeriodLength)                 // next period start
	            );
	            break;
	        }

	        case WEEK: {
	            // length is number of weeks
	            // start date for period N = activationDate + (N-1) * (length * 7) DAYS
	            streamData = Stream.iterate(
	                    activationDate,
	                    d -> d.plusDays(7 * billingPeriodLength)
	            );
	            break;
	        }

	        case MONTH: {
	            // start date for period N = activationDate + (N-1) * length MONTHS
	            streamData = Stream.iterate(
	                    activationDate,
	                    d -> d.plusMonths(billingPeriodLength)
	            );
	            break;
	        }

	        case YEAR: {
	            // start date for period N = activationDate + (N-1) * length YEARS
	            streamData = Stream.iterate(
	                    activationDate,
	                    d -> d.plusYears(billingPeriodLength)
	            );
	            break;
	        }

	        default:
	        	throw new IllegalArgumentException("Error in the RecurringChargePeriod: unexpected value for billingPeriodType");
	    }

	    // Take all start dates <= limitDate
	    startDates = streamData
	            .takeWhile(d -> !d.isAfter(limitDate))
	            .toList(); // immutable → create new ArrayList

	    //Logger.debug("Per {} {} billingPeriod START dates:{}",billingPeriodLength,billingPeriodType,startDates);
	    
	    return new ArrayList<>(startDates);
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
	
	/**
	 * Returns the a list of {@link BillCycle} calculated from an activation date to a limit date, according to the recurring charge period specified in the {@link ProductOfferingPrice}. 
	 * @param pop The {@link ProductOfferingPrice} that specifies the recurring charge period
	 * @param activationDate an {@link OffsetDateTime} from witch to start the calculation of the bill cycles
	 * @param limitDate an {@link OffsetDateTime} representing the limit date
	 * @return a list of {@link BillCycle} representing the billCycle(s)
	 * @throws BillingBadRequestException if the billCycles(s) can't be calculated because the {@link ProductOfferingPrice} is custom
	 */
	public List<BillCycle> getBillCycles(@NotNull ProductOfferingPrice pop, @NotNull OffsetDateTime activationDate, @NotNull OffsetDateTime limitDate) throws BillingBadRequestException{
		List<BillCycle> billCycles=new ArrayList<BillCycle>();
		
		if(ProductOfferingPriceUtils.isPriceTypeOneTime(pop)) {
			billCycles.add(this.getBillCycleForOneTime(activationDate));
		}
		
		if(ProductOfferingPriceUtils.isPriceTypeRecurringPrepaid(pop)) {
			billCycles.addAll(this.getBillCycleForRecurringPrepaid(pop, activationDate, limitDate));
		}
		
		if(ProductOfferingPriceUtils.isPriceTypeInRecurringPostpaidCategory(pop)) {
			billCycles.addAll(this.getBillCycleForRecurringPostpaid(pop, activationDate, limitDate));
		}
		
		if(ProductOfferingPriceUtils.isPriceTypeCustom(pop))
			throw new BillingBadRequestException(String.format("Error: Not possible to calculate billDates for PriceType '%s' in ProductOfferingPrice '%s' ", PriceType.CUSTOM.toString(), pop.getId()));
		
		return billCycles;
				
	}
	
	/**
	 * Returns the BillCyle for the specified {@link ProductOfferingPrice} of {@link PriceType} ONE_TIME
	 * 
	 * @param pop the {@link ProductOfferingPrice}
	 * @param activationDate an {@link OffsetDateTime} representing the activation date
	 * @return the BillCyle for the specified {@link ProductOfferingPrice} ONE_TIME
	 */
	private BillCycle getBillCycleForOneTime(@NotNull OffsetDateTime activationDate) {
		BillCycle billCycle=new BillCycle();
		billCycle.setBillDate(activationDate);
		billCycle.setBillingPeriod(TMForumEntityUtils.createTimePeriod678(activationDate, activationDate));
		
		return billCycle;
	}
	
	/**
	 * Returns the list of BillCyle for the specified {@link ProductOfferingPrice} of {@link PriceType} RECURRING_PREPAID
	 * 
	 * @param pop the {@link ProductOfferingPrice}
	 * @param activationDate an {@link OffsetDateTime} representing the activation date
	 * @param limitDate an {@link OffsetDateTime} representing the limit date
	 * @return the list of BillCyle for the specified {@link ProductOfferingPrice} RECURRING_PREPAID
	 */
	private List<BillCycle> getBillCycleForRecurringPrepaid(@NotNull ProductOfferingPrice pop, @NotNull OffsetDateTime activationDate, OffsetDateTime limitDate) {
		List<BillCycle> billCycles=new ArrayList<BillCycle>();
		
		List<TimePeriod> periodCoverages=this.calculateBillingPeriods(ProductOfferingPriceUtils.getRecurringChargePeriod(pop), activationDate, limitDate);
		
		for(TimePeriod periodCoverage: periodCoverages) {
			BillCycle billCycle=new BillCycle();
			billCycle.setBillDate(periodCoverage.getStartDateTime());
			billCycle.setBillingPeriod(TMForumEntityUtils.createTimePeriod678(periodCoverage.getStartDateTime(), periodCoverage.getEndDateTime()));
			
			billCycles.add(billCycle);
		}
		return billCycles;
	}
	
	/**
	 * Returns the list of BillCyle for the specified {@link ProductOfferingPrice} of {@link PriceType} RECURRING_POSTPAID
	 * 
	 * @param pop the {@link ProductOfferingPrice}
	 * @param activationDate an {@link OffsetDateTime} representing the activation date
	 * @param limitDate an {@link OffsetDateTime} representing the limit date
	 * @return the list of BillCyle for the specified {@link ProductOfferingPrice} RECURRING_POSTPAID
	 */
	private List<BillCycle> getBillCycleForRecurringPostpaid(@NotNull ProductOfferingPrice pop, @NotNull OffsetDateTime activationDate, OffsetDateTime limitDate) {
		List<BillCycle> billCycles=new ArrayList<BillCycle>();
		
		List<TimePeriod> periodCoverages=this.calculateBillingPeriods(ProductOfferingPriceUtils.getRecurringChargePeriod(pop), activationDate, limitDate);
		
		for(TimePeriod periodCoverage: periodCoverages) {
			BillCycle billCycle=new BillCycle();
			billCycle.setBillDate(periodCoverage.getEndDateTime());
			billCycle.setBillingPeriod(TMForumEntityUtils.createTimePeriod678(periodCoverage.getStartDateTime(), periodCoverage.getEndDateTime()));
			
			billCycles.add(billCycle);
		}
		return billCycles;
	}

	/**
	 * Gets, from the specified list of {@link BillCycle}, the ones that belongs to the specified billingPeriod
	 *  
	 * @param billCycles A list of {@link BillCycle} 
	 * @param billingPeriod a {@link TimePeriod} representing the billingPriod
	 * @return a list of {@link BillCycle} belonging to the billingPeriod
	 */
	public List<BillCycle> getBillCyclesInBillingPeriod(@NotNull List<BillCycle> billCycles, @NotNull TimePeriod billingPeriod){
		
		List<BillCycle> billCyclesInBillingPeriod=new ArrayList<BillCycle>();
		
		for(BillCycle billCycle:billCycles) {
			if(isBillDateWithinBillingPeriod(billCycle.getBillDate(), billingPeriod)){
				billCyclesInBillingPeriod.add(billCycle);
			}
		}
		
		return billCyclesInBillingPeriod;
		
	}
	
	/**
	 * Returns a list of {@link OffsetDateTime} representing the billDates of the specified list of {@link BillCycle}
	 * 
	 * @param billCycles A list of {@link BillCycle}
	 * @return A list of {@link OffsetDateTime} representing the billDates of the specified list of {@link BillCycle}
	 */
	List<OffsetDateTime> getBillDates(@NotNull List<BillCycle> billCycles){
		List<OffsetDateTime> billDates=new ArrayList<OffsetDateTime>();
		
		for(BillCycle billCycle:billCycles) {
			billDates.add(billCycle.getBillDate());
		}
		
		return billDates;
	}

}
