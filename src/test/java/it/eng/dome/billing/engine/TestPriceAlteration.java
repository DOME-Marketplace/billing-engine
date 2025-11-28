package it.eng.dome.billing.engine;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import it.eng.dome.billing.engine.exception.BillingEngineValidationException;
import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.price.alteration.PriceAlterationCalculator;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.TimePeriod;

public class TestPriceAlteration {
	
	public static void main(String[] args) {
		
		PriceAlterationCalculator pac=new PriceAlterationCalculator();
		
		TimePeriod tp=new TimePeriod();
		tp.setStartDateTime(OffsetDateTime.parse("2025-01-01T00:00:00+01:00"));
		tp.setEndDateTime(OffsetDateTime.parse("2027-01-01T00:00:00+01:00"));
		
		ProductOfferingPrice pop1=new ProductOfferingPrice();
		pop1.setPriceType("discount");
		pop1.setPercentage(10f);
		pop1.setLifecycleStatus("active");
		pop1.setValidFor(tp);
		
		ProductOfferingPrice pop2=new ProductOfferingPrice();
		pop2.setPriceType("discount");
		pop2.setPercentage(20f);
		pop2.setLifecycleStatus("active");
		pop2.setValidFor(tp);
		
		List<ProductOfferingPrice> popRel=new ArrayList<ProductOfferingPrice>();
		popRel.add(pop1);
		popRel.add(pop2);
		
		Money money=new Money("EUR",100f);
		
		try {
			Money price= pac.applyAlterations(money, popRel);
			System.out.println("price alterated: "+price.getValue());
		} catch (BillingEngineValidationException | ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
