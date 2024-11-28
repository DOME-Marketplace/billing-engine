package it.eng.dome.billing.engine.tmf;

import java.math.BigDecimal;
import java.math.RoundingMode;

import it.eng.dome.tmforum.tmf622.v4.model.Money;
import lombok.Getter;

public class EuroMoney {
	@Getter
	private String currency = "EUR";
	
	@Getter
	private float amount;

	public EuroMoney(double amount) {
		BigDecimal bd = new BigDecimal(amount);
		bd = bd.setScale(2, RoundingMode.HALF_UP);
		this.amount = bd.floatValue();
	}
	
	public Money toMoney() {
		return (new Money()).unit(currency).value(amount);
	}
	
}
