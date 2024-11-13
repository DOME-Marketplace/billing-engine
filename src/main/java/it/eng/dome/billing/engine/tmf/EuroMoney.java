package it.eng.dome.billing.engine.tmf;

import lombok.Getter;

public class EuroMoney {
	@Getter
	private String currency = "EUR";
	
	@Getter
	private float amount;

	public EuroMoney(float amount) {
		super();
		this.amount = amount;
	}
	
}
