package it.eng.dome.billing.engine.model;

public class Money {
	
	private static final String DEFAULT_CURRENCY = "EUR";
	
	private String unit;
	private Float value;
	
	public Money() {
		// TODO Auto-generated constructor stub
	}
	
	public Money(String unit, Float value) {
		super();
		if(unit==null)
			this.unit=DEFAULT_CURRENCY;
		else 
			this.unit = unit;
		this.value = value;
	}


	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public Float getValue() {
		return value;
	}

	public void setValue(Float value) {
		this.value = value;
	}
	
	

}
