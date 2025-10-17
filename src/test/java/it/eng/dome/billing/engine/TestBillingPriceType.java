package it.eng.dome.billing.engine;

import it.eng.dome.brokerage.billing.utils.BillingPriceType;

public class TestBillingPriceType {
	
	public static void main(String[] args) {
		
		String priceType="Recurring";
		String priceType2="recurring postpaid";
		String priceType3="Pay-per-use";
		
		String priceType4="One-time";
		
		String priceTypeN1=BillingPriceType.normalize(priceType);
		String priceTypeN2=BillingPriceType.normalize(priceType2);
		String priceTypeN3=BillingPriceType.normalize(priceType3);
		String priceTypeN4=BillingPriceType.normalize(priceType4);
		
		System.out.println(priceTypeN1);
		System.out.println(priceTypeN2);
		System.out.println(priceTypeN3);
		System.out.println(priceTypeN4);
		
		System.out.println(BillingPriceType.getAllowedValues());
		
		if (priceTypeN4.equalsIgnoreCase(BillingPriceType.ONE_TIME.name()))
			System.out.println("yes");
		else
			System.out.println("false");
	}

}
