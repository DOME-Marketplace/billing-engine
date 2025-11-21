package it.eng.dome.billing.engine.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotNull;

public class TmfConverter {
	
    private static final Logger logger = LoggerFactory.getLogger(TmfConverter.class);
	
	public static it.eng.dome.tmforum.tmf678.v4.model.BillingAccountRef convertBillingAccountRefTo678(@NotNull
			it.eng.dome.tmforum.tmf637.v4.model.BillingAccountRef in) {

		it.eng.dome.tmforum.tmf678.v4.model.BillingAccountRef out = new it.eng.dome.tmforum.tmf678.v4.model.BillingAccountRef();
		out.setId(in.getId());
		out.setHref(in.getHref());
		out.setName(in.getName());

		return out;
	}
	
	public static List<it.eng.dome.tmforum.tmf678.v4.model.RelatedParty> convertRpTo678(@NotNull List<it.eng.dome.tmforum.tmf637.v4.model.RelatedParty> relatedParty637) {
	
	    List<it.eng.dome.tmforum.tmf678.v4.model.RelatedParty> relatedParty678 = new ArrayList<>();
	
	    for (it.eng.dome.tmforum.tmf637.v4.model.RelatedParty rp637 : relatedParty637) {
	        it.eng.dome.tmforum.tmf678.v4.model.RelatedParty rp678 = new it.eng.dome.tmforum.tmf678.v4.model.RelatedParty();
	
	        rp678.setId(rp637.getId());
	        try {
				rp678.setHref(new URI(rp637.getId()));
			} catch (URISyntaxException e) {
				logger.warn("Invalid URI for RelatedParty id={} -> {}", rp637.getId(), e.getMessage());
			}
	        rp678.setName(rp637.getName());
	        rp678.setRole(rp637.getRole());
	        rp678.setAtReferredType(rp637.getAtReferredType()); // ??
	
	        relatedParty678.add(rp678);
	    }
	
	    return relatedParty678;
	}
	
	public static it.eng.dome.tmforum.tmf622.v4.model.Money convertMoneyTo622(@NotNull it.eng.dome.tmforum.tmf620.v4.model.Money moneyIn) {
		it.eng.dome.tmforum.tmf622.v4.model.Money  out=new it.eng.dome.tmforum.tmf622.v4.model.Money();
		
		out.setUnit(moneyIn.getUnit());
		out.setValue(moneyIn.getValue());
		
		return out;
	}
	
	public static it.eng.dome.tmforum.tmf622.v4.model.Money convertMoneyTo622(@NotNull it.eng.dome.billing.engine.model.Money moneyIn){
		
		it.eng.dome.tmforum.tmf622.v4.model.Money  out=new it.eng.dome.tmforum.tmf622.v4.model.Money();
		
		out.setUnit(moneyIn.getUnit());
		out.setValue(moneyIn.getValue());
		
		return out;
		
	}
	
	public static it.eng.dome.tmforum.tmf678.v4.model.Money convertMoneyTo678(@NotNull it.eng.dome.billing.engine.model.Money moneyIn){
		
		it.eng.dome.tmforum.tmf678.v4.model.Money  out=new it.eng.dome.tmforum.tmf678.v4.model.Money();
		
		out.setUnit(moneyIn.getUnit());
		out.setValue(moneyIn.getValue());
		
		return out;
		
	}
	
	public static it.eng.dome.billing.engine.model.Money convert622ToMoney(@NotNull it.eng.dome.tmforum.tmf622.v4.model.Money moneyIn){
		
		it.eng.dome.billing.engine.model.Money  out=new it.eng.dome.billing.engine.model.Money();
		
		out.setUnit(moneyIn.getUnit());
		out.setValue(moneyIn.getValue());
		
		return out;
		
	}
}
