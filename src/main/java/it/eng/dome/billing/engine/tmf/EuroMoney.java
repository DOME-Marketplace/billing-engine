package it.eng.dome.billing.engine.tmf;

import it.eng.dome.tmforum.tmf622.v4.model.Money;
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
	
	public Money toMoney() {
		return (new Money()).unit(getCurrency()).value(getAmount());
	}
	
}
