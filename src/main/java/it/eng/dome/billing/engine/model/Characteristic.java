package it.eng.dome.billing.engine.model;

/**
 * Class representing a Characteristic with a name, valueType and a value.
 */
public class Characteristic {
	
	private String name;
	
	private String valueType;
	
	private Object value;

	public Characteristic() {
	}

	public Characteristic(String name, String valueType, Object value) {
		super();
		this.name = name;
		this.valueType = valueType;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) {
		this.valueType = valueType;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

}
