package it.eng.dome.billing.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import it.eng.dome.billing.engine.utils.PriceTypeKey;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;

public class TestPriceTypeKey {
	
	private HashMap<PriceTypeKey, List<OrderPrice>> hasMap=new HashMap<PriceTypeKey, List<OrderPrice>>();
	
	public static void main(String[] args) {
		
		OrderPrice op1=new OrderPrice();
		op1.setName("one-time");
		
		OrderPrice op2=new OrderPrice();
		op2.setName("one-time");
		
		OrderPrice op3=new OrderPrice();
		op3.setName("recurring-1-month");
		
		OrderPrice op4=new OrderPrice();
		op4.setName("recurring-2-month");
		
		OrderPrice op5=new OrderPrice();
		op5.setName("recurring-1-month");
		
		PriceTypeKey ptk1=new PriceTypeKey("one-time", "");
		PriceTypeKey ptk2=new PriceTypeKey("one-time", "");
		PriceTypeKey ptk3=new PriceTypeKey("recurring", "1 month");
		PriceTypeKey ptk4=new PriceTypeKey("recurring", "2 month");
		PriceTypeKey ptk5=new PriceTypeKey("recurring", "1 month");
		
		TestPriceTypeKey test=new TestPriceTypeKey();
		test.updateHashMap(op1, ptk1);
		test.updateHashMap(op2, ptk2);
		test.updateHashMap(op3, ptk3);
		test.updateHashMap(op4, ptk4);
		test.updateHashMap(op5, ptk5);
		
		test.printList();
	}
	
	public void updateHashMap(OrderPrice op, PriceTypeKey key) {
		if(hasMap.containsKey(key)) {
			hasMap.get(key).add(op);
		}else {
			List<OrderPrice> list=new ArrayList<OrderPrice>();
			list.add(op);
			hasMap.put(key, list);
		}
	}
	
	public void printList() {
		Set<PriceTypeKey> keySet=hasMap.keySet();
		for(PriceTypeKey key:keySet) {
			System.out.println("Key: "+key.getPriceType()+"-"+key.getRecurringChargePeriod());
			List<OrderPrice> list =hasMap.get(key);
			for(OrderPrice op:list) {
				System.out.println(op.getName());
			}
		}
	}
	

}
