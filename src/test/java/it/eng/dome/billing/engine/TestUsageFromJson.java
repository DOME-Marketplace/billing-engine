package it.eng.dome.billing.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import it.eng.dome.tmforum.tmf635.v4.model.Usage;

public class TestUsageFromJson {
	
	public static void main(String[] args) {
		
		
		String percorsoFile = "C:\\Users\\sdagosti\\dome-workspace\\billing-engine\\src\\test\\java\\sanple-data\\UsageExample_B.json";
		
		try {
			String contenuto = new String(Files.readAllBytes(Paths.get(percorsoFile)));
			
			//String contenuto = "{'description':'Windows VM usage','usageType':'VM_USAGE','ratedProductUsage':[{'productRef':{'id':'urn:ngsi-ld:product:19402bf1-0889-46e5-9f6c-6fc458be024f','href':'urn:ngsi-ld:product:19402bf1-0889-46e5-9f6c-6fc458be024f','name':'Product for VDC Test'}}],'relatedParty':[{'id':'urn:ngsi-ld:organization:38063c78-fc9f-42ca-a39e-518107a2d403','href':'urn:ngsi-ld:organization:38063c78-fc9f-42ca-a39e-518107a2d403','name':'FICODES','role':'seller','@referredType':'organization'},{'id':'urn:ngsi-ld:organization:38817de3-8c3e-4141-a344-86ffd915cc3b','href':'urn:ngsi-ld:organization:38817de3-8c3e-4141-a344-86ffd915cc3b','name':'DHUB, Engineering D.HUB S.p.A.','role':'buyer','@referredType':'organization'}],'status':'received','usageCharacteristic':[{'id':'7bbc511e-b227-455c-8fd2-47377cebdca6','name':'CORE_hour','valueType':'float','value':2.45},{'id':'81e3b6fc-9be5-4680-b736-d7add7c72dec','name':'RAM_GB_hour','valueType':'float','value':45.37},{'id':'b1b2ecff-1586-4545-b6f4-384ce685070a','name':'DISK_GB_hour','valueType':'float','value':6122.59}]}";

			System.out.println(contenuto);
			
			Usage usage=Usage.fromJson(contenuto);
			
			System.out.println(usage.toJson());
			
	    } catch (IOException e) {
	        e.printStackTrace();
	     }
		
	}
	
	public Usage getUsageExampleA() {
		Usage usageA=null;
		
		String usageExampleA_path = "C:\\Users\\sdagosti\\dome-workspace\\billing-engine\\src\\test\\java\\sanple-data\\UsageExample_A.json";
		try {
			String usageExampleA = new String(Files.readAllBytes(Paths.get(usageExampleA_path)));
			System.out.println(usageExampleA);
			
			usageA=Usage.fromJson(usageExampleA);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return usageA;
	}
	
	public Usage getUsageExampleB() {
		Usage usageB=null;
		
		String usageExampleB_path = "C:\\Users\\sdagosti\\dome-workspace\\billing-engine\\src\\test\\java\\sanple-data\\UsageExample_B.json";
		try {
			String usageExampleB = new String(Files.readAllBytes(Paths.get(usageExampleB_path)));
			System.out.println(usageExampleB);
			
			usageB=Usage.fromJson(usageExampleB);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return usageB;
	}

	
}
