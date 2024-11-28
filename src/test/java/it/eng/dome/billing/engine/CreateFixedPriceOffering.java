package it.eng.dome.billing.engine;

import java.net.URI;
import java.net.URISyntaxException;

import it.eng.dome.tmforum.tmf620.v4.ApiClient;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.Configuration;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingApi;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingPriceApi;
import it.eng.dome.tmforum.tmf620.v4.api.ProductSpecificationApi;
import it.eng.dome.tmforum.tmf620.v4.model.CharacteristicValueSpecification;
import it.eng.dome.tmforum.tmf620.v4.model.Duration;
import it.eng.dome.tmforum.tmf620.v4.model.Money;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOffering;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingCreate;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPriceCreate;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPriceRefOrValue;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPriceRelationship;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingTerm;
import it.eng.dome.tmforum.tmf620.v4.model.ProductSpecification;
import it.eng.dome.tmforum.tmf620.v4.model.ProductSpecificationCharacteristic;
import it.eng.dome.tmforum.tmf620.v4.model.ProductSpecificationCharacteristicValueUse;
import it.eng.dome.tmforum.tmf620.v4.model.ProductSpecificationCreate;
import it.eng.dome.tmforum.tmf620.v4.model.ProductSpecificationRef;

public class CreateFixedPriceOffering {
	private static String TMF_SERVER = "https://dome-dev.eng.it";
	//private static String TMF_SERVER = "http://localhost:8100";

	public static void main(String[] args) throws URISyntaxException {
		try {
			ApiClient offeringClient = Configuration.getDefaultApiClient();
			offeringClient.setBasePath(TMF_SERVER + "/tmf-api/productCatalogManagement/v4");
			
			CreateFixedPriceOffering cfpo = new CreateFixedPriceOffering();
			// 1) creates Product Specifications
			ProductSpecification ps = cfpo.storeProductSpecifications(offeringClient);
			System.out.println("Creato ProductSpecification con id: " + ps.getId() + ", href: " + ps.getHref());
			// 2) Creates Fixed Price Product Offering
		    ProductOffering po = cfpo.storeNewProductOffering(offeringClient, ps);
			System.out.println("Creato ProductOffering con id: " + po.getId() + ", href: " + po.getHref());

		} catch (ApiException e) {
			e.printStackTrace();
		}
	}
	
	ProductSpecification storeProductSpecifications(ApiClient offeringClient) throws ApiException, URISyntaxException {
		ProductSpecificationCreate pSpec = new ProductSpecificationCreate();
		
		pSpec
		.name("Preconfigured Linux VM")
		.brand("D-HUB")
		.productNumber("CSC-340-NGFW")
		.description("A Linux VM with multiple preconfigured options")
		.isBundle(false)
		.lifecycleStatus("Active");
		
		// CPU
		{
			ProductSpecificationCharacteristic cpuChar = new ProductSpecificationCharacteristic();
			cpuChar
			.id("CPU_SPEC")
			.name("CPU")
			.configurable(true)
			.valueType("number");
			
			CharacteristicValueSpecification cpuValue1 = new CharacteristicValueSpecification();
			cpuValue1
			.isDefault(true)
			.value(4)
			.unitOfMeasure("unit");
			cpuChar.addProductSpecCharacteristicValueItem(cpuValue1);
			
			CharacteristicValueSpecification cpuValue2 = new CharacteristicValueSpecification();
			cpuValue2
			.isDefault(false)
			.value(8)
			.unitOfMeasure("unit");
			cpuChar.addProductSpecCharacteristicValueItem(cpuValue2);
			
			pSpec.addProductSpecCharacteristicItem(cpuChar);
		}
		
		// RAM
		{
			ProductSpecificationCharacteristic ramChar = new ProductSpecificationCharacteristic();
			ramChar
			.id("RAM_SPEC")
			.name("RAM")
			.configurable(true)
			.valueType("number");
			
			CharacteristicValueSpecification ramValue1 = new CharacteristicValueSpecification();
			ramValue1
			.isDefault(true)
			.value(16)
			.unitOfMeasure("GB");
			ramChar.addProductSpecCharacteristicValueItem(ramValue1);
			
			CharacteristicValueSpecification ramValue2 = new CharacteristicValueSpecification();
			ramValue2
			.isDefault(false)
			.value(32)
			.unitOfMeasure("GB");
			ramChar.addProductSpecCharacteristicValueItem(ramValue2);
			
			pSpec.addProductSpecCharacteristicItem(ramChar);
		}
		
		// Storage
		{
			ProductSpecificationCharacteristic storageChar = new ProductSpecificationCharacteristic();
			storageChar
			.id("Storage_SPEC")
			.name("Storage")
			.configurable(true)
			.valueType("number");
			
			CharacteristicValueSpecification storageValue1 = new CharacteristicValueSpecification();
			storageValue1
			.isDefault(true)
			.value(20)
			.unitOfMeasure("GB");
			storageChar.addProductSpecCharacteristicValueItem(storageValue1);
			
			CharacteristicValueSpecification storageValue2 = new CharacteristicValueSpecification();
			storageValue2
			.isDefault(false)
			.value(40)
			.unitOfMeasure("GB");
			storageChar.addProductSpecCharacteristicValueItem(storageValue2);
			
			CharacteristicValueSpecification storageValue3 = new CharacteristicValueSpecification();
			storageValue3
			.isDefault(false)
			.value(60)
			.unitOfMeasure("GB");
			storageChar.addProductSpecCharacteristicValueItem(storageValue3);
			
			pSpec.addProductSpecCharacteristicItem(storageChar);
		}		
		
		ProductSpecificationApi specApi = new ProductSpecificationApi(offeringClient);
		
		final ProductSpecification storedPS = specApi.createProductSpecification(pSpec);
		return storedPS;
		// System.out.println(storedPS.toJson());
	}
	
	
	ProductOffering storeNewProductOffering(ApiClient offeringClient, ProductSpecification ps) throws ApiException, URISyntaxException {
	    final var popApi = new ProductOfferingPriceApi(offeringClient);
		final var productSpecRef = new ProductSpecificationRef();		
		productSpecRef.id(ps.getId()).href(new URI(ps.getHref()));
		
		ProductOfferingPriceCreate smallPriceCreate = createProductOfferingPriceSmall(productSpecRef);
		ProductOfferingPrice smallPrice = popApi.createProductOfferingPrice(smallPriceCreate);
		System.out.println("Creato POP small con id: " + smallPrice.getId() + ", href: " + smallPrice.getHref());
		
		ProductOfferingPriceCreate discountPopCreate = createSixMonthsDiscount();
		ProductOfferingPrice discountPop = popApi.createProductOfferingPrice(discountPopCreate);
		System.out.println("Creato POP discount con id: " + discountPop.getId() + ", href: " + discountPop.getHref());
		
		ProductOfferingPriceCreate largePriceCreate = createProductOfferingPriceLarge(productSpecRef, discountPop);
		ProductOfferingPrice largePrice = popApi.createProductOfferingPrice(largePriceCreate);
		System.out.println("Creato POP large con id: " + largePrice.getId() + ", href: " + largePrice.getHref());
		
		// Crea la Product Offering
	    final ProductOfferingApi offeringApi = new ProductOfferingApi(offeringClient);
	    ProductOfferingCreate poc = createProductOffering();
	    // add first price
	    var priceItem = new ProductOfferingPriceRefOrValue();
	    priceItem.href(new URI(smallPrice.getHref()));
	    priceItem.id(smallPrice.getId());
	    poc.addProductOfferingPriceItem(priceItem);
	    // add second price
	    priceItem = new ProductOfferingPriceRefOrValue();
	    priceItem.href(new URI(largePrice.getHref()));
	    priceItem.id(largePrice.getId());
	    poc.addProductOfferingPriceItem(priceItem);
	    // add specifications
	    poc.setProductSpecification(productSpecRef);
	    
	    ProductOffering po = offeringApi.createProductOffering(poc);
		return po;
	}
	
	private ProductOfferingCreate createProductOffering() {
		ProductOfferingCreate poc = new ProductOfferingCreate();
		
		poc
		.name("Your custom VM at fixed price")
		.description("A customizable VM at fixed price")
		.isBundle(false)
		.lifecycleStatus("Launched");
		
		return poc;
	}
	
	private ProductOfferingPriceCreate createProductOfferingPriceSmall(ProductSpecificationRef productSpecRef) {
		Money price = new Money();
		price.value(24F).unit("EUR");
		
		ProductOfferingPriceCreate pop = new ProductOfferingPriceCreate();
		pop
		.name("Small Fixed Price")
		.description("4CPU, 16GB RAM, 20GB HD: 24 eu per m, recurring prepaid")
		.version("1.0")
		.priceType("recurring-prepaid")
		.recurringChargePeriodLength(1)
		.recurringChargePeriodType("month")
		.isBundle(false)
		.lifecycleStatus("Launched")
		.price(price);
		
		{
			var cpuValue = new CharacteristicValueSpecification();
			cpuValue.isDefault(true).value(4);
			
			var cpuSpec = new ProductSpecificationCharacteristicValueUse();
			cpuSpec
			.id("CPU_SPEC")
			.name("CPU")
			.valueType("number")
			.addProductSpecCharacteristicValueItem(cpuValue)
			.setProductSpecification(productSpecRef);
			
			pop.addProdSpecCharValueUseItem(cpuSpec);
		}
		
		{
			var ramValue = new CharacteristicValueSpecification();
			ramValue.isDefault(true).value(16).unitOfMeasure("GB");
			
			var ramSpec = new ProductSpecificationCharacteristicValueUse();
			ramSpec
			.id("RAM_SPEC")
			.name("RAM")
			.valueType("number")
			.addProductSpecCharacteristicValueItem(ramValue)
			.setProductSpecification(productSpecRef);
			
			pop.addProdSpecCharValueUseItem(ramSpec);
		}
		
		{
			var diskValue = new CharacteristicValueSpecification();
			diskValue.isDefault(true).value(20).unitOfMeasure("GB");
			
			var diskSpec = new ProductSpecificationCharacteristicValueUse();
			diskSpec
			.id("Storage_SPEC")
			.name("Storage")
			.valueType("number")
			.addProductSpecCharacteristicValueItem(diskValue)
			.setProductSpecification(productSpecRef);
			
			pop.addProdSpecCharValueUseItem(diskSpec);
		}
		
		return pop;
	}
	
	
	private ProductOfferingPriceCreate createProductOfferingPriceLarge(ProductSpecificationRef productSpecRef,
			ProductOfferingPrice discountPop) throws URISyntaxException {
		Money price = new Money();
		price.value(30F).unit("EUR");
		
		ProductOfferingPriceCreate pop = new ProductOfferingPriceCreate();
		pop
		.name("Alessio Price LARGE")
		.description("8CPU, 32GB RAM, 40GB HD: 30 eu per m, recurring prepaid")
		.version("1.0")
		.priceType("recurring-prepaid")
		.recurringChargePeriodLength(1)
		.recurringChargePeriodType("month")
		.isBundle(false)
		.lifecycleStatus("Launched")
		.price(price);
		
		{
			var cpuValue = new CharacteristicValueSpecification();
			cpuValue.isDefault(true).value(8);
			
			var cpuSpec = new ProductSpecificationCharacteristicValueUse();
			cpuSpec
			.id("CPU_SPEC")
			.name("CPU")
			.valueType("number")
			.addProductSpecCharacteristicValueItem(cpuValue)
			.setProductSpecification(productSpecRef);
			
			pop.addProdSpecCharValueUseItem(cpuSpec);
		}
		
		{
			var ramValue = new CharacteristicValueSpecification();
			ramValue.isDefault(true).value(32).unitOfMeasure("GB");
			
			var ramSpec = new ProductSpecificationCharacteristicValueUse();
			ramSpec
			.id("RAM_SPEC")
			.name("RAM")
			.valueType("number")
			.addProductSpecCharacteristicValueItem(ramValue)
			.setProductSpecification(productSpecRef);
			
			pop.addProdSpecCharValueUseItem(ramSpec);
		}
		
		{
			var diskValue = new CharacteristicValueSpecification();
			diskValue.isDefault(true).value(40).unitOfMeasure("GB");
			
			var diskSpec = new ProductSpecificationCharacteristicValueUse();
			diskSpec
			.id("Storage_SPEC")
			.name("Storage")
			.valueType("number")
			.addProductSpecCharacteristicValueItem(diskValue)
			.setProductSpecification(productSpecRef);
			
			pop.addProdSpecCharValueUseItem(diskSpec);
		}
		
		// Add discount
		if (discountPop != null) {
			ProductOfferingPriceRelationship discountRel = new ProductOfferingPriceRelationship();
			discountRel
			.relationshipType("discount")
			.href(new URI(discountPop.getHref()))
			.id(discountPop.getId());
			
			pop.addPopRelationshipItem(discountRel);
		}
		
		return pop;
	}
	
	
	private ProductOfferingPriceCreate createSixMonthsDiscount() {
		ProductOfferingPriceCreate pop = new ProductOfferingPriceCreate();
		pop
		.name("Six months 50% special price")
		.description("Six months 50% special price")
		.version("1.0")
		.priceType("discount")
		.percentage(50F)
		.isBundle(false)
		.lifecycleStatus("Launched");
				
		ProductOfferingTerm durationTerm = new ProductOfferingTerm();
		Duration duration = new Duration();
		duration.amount(6).units("month");
		durationTerm.setDuration(duration);
		
		pop.addProductOfferingTermItem(durationTerm);
		
		return pop;
	}
}
