package it.eng.dome.billing.engine.model;

import java.util.List;

import it.eng.dome.tmforum.tmf637.v4.model.Characteristic;

public class ProductInfo {
	
	private final String id;
	private final List<Characteristic> productCharacteristic;
	
	private final Source source;


	public ProductInfo(String id, List<Characteristic> productCharacteristic, Source source) {
		super();
		this.id = id;
		this.productCharacteristic = productCharacteristic;
		this.source = source;
	}


	public String getId() {
		return id;
	}


	public List<Characteristic> getProductCharacteristic() {
		return productCharacteristic;
	}
	
	public enum Source {
		PRODUCT,
		PRODUCT_ORDER
	}
	
	public boolean isFromProduct() {
        return source == Source.PRODUCT;
    }

    public boolean isFromProductOrder() {
        return source == Source.PRODUCT_ORDER;
    }
	

}
