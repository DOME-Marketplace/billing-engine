package it.eng.dome.billing.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.eng.dome.tmforum.tmf635.v4.model.Usage;
import it.eng.dome.tmforum.tmf635.v4.model.UsageCharacteristic;
import lombok.NonNull;

public class TestUsageDataMap {
	
	List<Usage> usageData=new ArrayList<Usage>();
	
	
	
	public TestUsageDataMap() {
		
		UsageCharacteristic usageCharacteristicA_1=new UsageCharacteristic();
		usageCharacteristicA_1.setId("uChA_1");
		usageCharacteristicA_1.setName("CPU/hour");
		usageCharacteristicA_1.setValue(1);
		
		UsageCharacteristic usageCharacteristicA_2=new UsageCharacteristic();
		usageCharacteristicA_2.setId("uChA_2");
		usageCharacteristicA_2.setName("CPU/hour");
		usageCharacteristicA_2.setValue(2);
		
		UsageCharacteristic usageCharacteristicA_3=new UsageCharacteristic();
		usageCharacteristicA_3.setId("uChA_3");
		usageCharacteristicA_3.setName("RAM/hour");
		usageCharacteristicA_3.setValue(10);
		
		List<UsageCharacteristic> listA=new ArrayList<UsageCharacteristic>();
		listA.add(usageCharacteristicA_1);
		listA.add(usageCharacteristicA_2);
		listA.add(usageCharacteristicA_3);
		
		UsageCharacteristic usageCharacteristicB_1=new UsageCharacteristic();
		usageCharacteristicB_1.setId("uChB_1");
		usageCharacteristicB_1.setName("CPU/hour");
		usageCharacteristicB_1.setValue(3);
		
		List<UsageCharacteristic> listB=new ArrayList<UsageCharacteristic>();
		listB.add(usageCharacteristicB_1);
		
		Usage usageA=new Usage();
		usageA.setId("usageA");
		usageA.setUsageCharacteristic(listA);
		
		Usage usageB=new Usage();
		usageB.setId("usageB");
		usageB.setUsageCharacteristic(listB);
		
		usageData.add(usageA);
		usageData.add(usageB);
		
	}


	public static void main(String[] args) {
		
		TestUsageDataMap test=new TestUsageDataMap();
		Map<String, List<UsageCharacteristic>> map=test.createUsageDataMap(test.usageData);
		System.out.println("key set "+ map.keySet().toString());
		Set<String> keys=map.keySet();
		
		for(String key: keys) {
			System.out.println(key);
			System.out.println(map.get(key).toString());
		}
		
	}
	
	
	private Map<String, List<UsageCharacteristic>> createUsageDataMap(@NonNull List<Usage> usages){
		Map<String, List<UsageCharacteristic>> usageData=new HashMap<String, List<UsageCharacteristic>>();
		
		for (Usage usage : usages) {
			if (usage.getUsageCharacteristic() != null && !usage.getUsageCharacteristic().isEmpty()) {
				System.out.println(" -> Current usage: " + usage.getId());
				
				List<UsageCharacteristic> usageCharacteristics = usage.getUsageCharacteristic();
				System.out.println(" -> UsageCharactetistic list size: " + usageCharacteristics.size());
				for (UsageCharacteristic usageCharacteristic : usageCharacteristics) {
					if (usageCharacteristic != null) {
						usageData.computeIfAbsent(usageCharacteristic.getName(), K -> new ArrayList<UsageCharacteristic>()).add(usageCharacteristic);
						//System.out.println("Add UsageCharackey/value in Map: " + usageCharacteristic.getName() + " - " + usageCharacteristic.getValue());
						///usageData.put(usageCharacteristic.getName(), usageCharacteristic);
					}
					else {
						System.out.println("UsageCharacteristic cannot be null for usage: " + usage.getId());
					}
				}
			}
		}
		
		return usageData;
	}
	
	public void createUsageFromJson() {
		
		String percorsoFile = "./src/test/java/sample-data/UsageExample_A.json";

		try {
			List<String> righe = Files.readAllLines(Paths.get(percorsoFile));
            for (String riga : righe) {
                System.out.println(riga);
            }
	    } catch (IOException e) {
	        e.printStackTrace();
	     }
	}

}
