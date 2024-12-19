package it.eng.dome.billing.engine;
import java.time.OffsetDateTime;

import it.eng.dome.tmforum.tmf678.v4.JSON;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;

public class TestPeriodCoverage {
	
	public static void main(String[] args) {
		AppliedCustomerBillingRate appliedCustomerBillingRate=new AppliedCustomerBillingRate();
		
		appliedCustomerBillingRate.setId("1234");
		TimePeriod tp=new TimePeriod();
		tp.setStartDateTime(OffsetDateTime.now());
		tp.setEndDateTime(OffsetDateTime.now());
		appliedCustomerBillingRate.setPeriodCoverage(tp);
		
		String str=JSON.getGson().toJson(appliedCustomerBillingRate);
		System.out.print(str);
	}

}
