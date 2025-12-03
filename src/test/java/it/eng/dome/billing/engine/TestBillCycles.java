package it.eng.dome.billing.engine;

import java.time.OffsetDateTime;
import java.util.List;

import it.eng.dome.billing.engine.service.BillCycleService;
import it.eng.dome.brokerage.model.RecurringChargePeriod;
import it.eng.dome.brokerage.model.RecurringPeriod;

public class TestBillCycles {
	
	public static void main(String[] args) {
		BillCycleService bcs=new BillCycleService();
		
		RecurringChargePeriod rcp=new RecurringChargePeriod(RecurringPeriod.DAY, 2);
		
		OffsetDateTime activationDate=OffsetDateTime.parse("2025-12-25T10:04:38.983Z");
		OffsetDateTime limitDate=OffsetDateTime.parse("2026-04-30T10:04:38.983Z");
		
		List<OffsetDateTime> startDates= bcs.calculateBillingPeriodStartDates(rcp, activationDate, limitDate);
		
		List<OffsetDateTime> endDates= bcs.calculateBillingPeriodEndDates(rcp, activationDate, limitDate);
		
		System.out.println("StartDates");
		for(OffsetDateTime date:startDates) {
			System.out.print("-");
			System.out.print(date);
			System.out.print("-");
		}
		
		System.out.println("\n EndDates");
		for(OffsetDateTime date:endDates) {
			System.out.print("-");
			System.out.print(date);
			System.out.print("-");
		}
	}

}
