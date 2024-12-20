package it.eng.dome.billing.engine;

import java.net.URI;
import java.net.URISyntaxException;

import it.eng.dome.tmforum.tmf620.v4.ApiClient;
import it.eng.dome.tmforum.tmf620.v4.ApiException;
import it.eng.dome.tmforum.tmf620.v4.Configuration;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingApi;
import it.eng.dome.tmforum.tmf620.v4.api.ProductOfferingPriceApi;
import it.eng.dome.tmforum.tmf620.v4.api.ProductSpecificationApi;
import it.eng.dome.tmforum.tmf620.v4.model.BundledProductOfferingPriceRelationship;
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
import it.eng.dome.tmforum.tmf620.v4.model.Quantity;

public class CreateFlexiblePriceOffering {
	private static String TMF_SERVER = "https://dome-dev.eng.it";
	//private static String TMF_SERVER = "http://localhost:8100";


	public static void main(String[] args) throws URISyntaxException {
		try {
			ApiClient offeringClient = Configuration.getDefaultApiClient();
			offeringClient.setBasePath(TMF_SERVER + "/tmf-api/productCatalogManagement/v4");
			
			CreateFlexiblePriceOffering cfpo = new CreateFlexiblePriceOffering();
			// 1) creates Product Specifications
			//ProductSpecification ps = cfpo.storeProductSpecifications(offeringClient);
			//System.out.println("Creato ProductSpecification con id: " + ps.getId() + ", href: " + ps.getHref());
			
			// 1.1) Retrieve Product Specification
			ProductSpecificationApi psApi = new ProductSpecificationApi(offeringClient);
			ProductSpecification ps = psApi.retrieveProductSpecification("urn:ngsi-ld:product-specification:cc8764c7-552e-4eec-b798-0693519fd4dd", null);
			System.out.println("Recuperato ProductSpecification con id: " + ps.getId() + ", href: " + ps.getHref());

			//
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
		.name("Configurable Linux VM")
		.brand("D-HUB")
		.productNumber("CSC-786-WERT")
		.description("A configurable Linux VM")
		.isBundle(false)
		.lifecycleStatus("Active");
		
		// CPU
		{
			ProductSpecificationCharacteristic cpuChar = new ProductSpecificationCharacteristic();
			cpuChar
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
			.name("Storage")
			.configurable(true)
			.valueType("number");
			
			CharacteristicValueSpecification storageValue1 = new CharacteristicValueSpecification();
			storageValue1
			.isDefault(true)
			.valueFrom(20)
			.valueTo(60)
			.rangeInterval("open")
			.unitOfMeasure("GB");
			storageChar.addProductSpecCharacteristicValueItem(storageValue1);
			
			pSpec.addProductSpecCharacteristicItem(storageChar);
		}		
		
		ProductSpecificationApi specApi = new ProductSpecificationApi(offeringClient);
		
		System.out.println(pSpec.toJson());

		final ProductSpecification storedPS = specApi.createProductSpecification(pSpec);
		return storedPS;
	}
	
	
	ProductOffering storeNewProductOffering(ApiClient offeringClient, ProductSpecification ps) throws ApiException, URISyntaxException {
	    final var popApi = new ProductOfferingPriceApi(offeringClient);
		final var productSpecRef = new ProductSpecificationRef();		
		productSpecRef.id(ps.getId()).href(new URI(ps.getHref()));
		
		ProductOfferingPriceCreate containerPopCreate = createContainerPop(productSpecRef);

		// 1 of 7 - RAM 16 Fixed
		ProductOfferingPriceCreate _16GBFPopCreate = createRAM16GBPop(productSpecRef);
		ProductOfferingPrice _16GBFPop = popApi.createProductOfferingPrice(_16GBFPopCreate);
		System.out.println("Creato POP RAM16GB Fixed con id: " + _16GBFPop.getId() + ", href: " + _16GBFPop.getHref());
		{
			var bp = new BundledProductOfferingPriceRelationship();
			bp.id(_16GBFPop.getId()).href(_16GBFPop.getHref());
			containerPopCreate.addBundledPopRelationshipItem(bp);
		}

		// 2 of 7 - RAM 32 Fixed
		ProductOfferingPriceCreate _32GBFPopCreate = createRAM32GBPop(productSpecRef);
		ProductOfferingPrice _32GBFPop = popApi.createProductOfferingPrice(_32GBFPopCreate);
		System.out.println("Creato POP RAM32GB Fixed con id: " + _32GBFPop.getId() + ", href: " + _32GBFPop.getHref());
		{
			var bp = new BundledProductOfferingPriceRelationship();
			bp.id(_32GBFPop.getId()).href(_32GBFPop.getHref());
			containerPopCreate.addBundledPopRelationshipItem(bp);
		}

		// 3 of 7 - CPU Flexible
		ProductOfferingPriceCreate cpuFlexiblePopCreate = createCPUFlexiblePop(productSpecRef);
		ProductOfferingPrice cpuFlexiblePop = popApi.createProductOfferingPrice(cpuFlexiblePopCreate);
		System.out.println("Creato POP CPU Flexible con id: " + cpuFlexiblePop.getId() + ", href: " + cpuFlexiblePop.getHref());
		{
			var bp = new BundledProductOfferingPriceRelationship();
			bp.id(cpuFlexiblePop.getId()).href(cpuFlexiblePop.getHref());
			containerPopCreate.addBundledPopRelationshipItem(bp);
		}
		
		// 4 of 7 - Storage Flexible Discount
		ProductOfferingPriceCreate discountPopCreate = createSixMonthsDiscount();
		ProductOfferingPrice discountPop = popApi.createProductOfferingPrice(discountPopCreate);
		System.out.println("Creato POP discount con id: " + discountPop.getId() + ", href: " + discountPop.getHref());
				
		// 5 of 7 - Storage Flexible
		ProductOfferingPriceCreate storageFlexiblePopCreate = createStorageFlexiblePop(productSpecRef, discountPop);
		ProductOfferingPrice storageFlexiblePop = popApi.createProductOfferingPrice(storageFlexiblePopCreate);
		System.out.println("Creato POP Storage Flexible con id: " + storageFlexiblePop.getId() + ", href: " + storageFlexiblePop.getHref());
		{
			var bp = new BundledProductOfferingPriceRelationship();
			bp.id(storageFlexiblePop.getId()).href(storageFlexiblePop.getHref());
			containerPopCreate.addBundledPopRelationshipItem(bp);
		}
		
		// 6 of 7 - Storage Fixed
		ProductOfferingPriceCreate storageFixedPopCreate = createStorageFixedPop(productSpecRef);
		ProductOfferingPrice storageFixedPop = popApi.createProductOfferingPrice(storageFixedPopCreate);
		System.out.println("Creato POP Storage Fixed con id: " + storageFixedPop.getId() + ", href: " + storageFixedPop.getHref());
		{
			var bp = new BundledProductOfferingPriceRelationship();
			bp.id(storageFixedPop.getId()).href(storageFixedPop.getHref());
			containerPopCreate.addBundledPopRelationshipItem(bp);
		}
				
		// 7 of 7 - Container POP
		ProductOfferingPrice containerPop = popApi.createProductOfferingPrice(containerPopCreate);
		System.out.println("Creato container Pop con id: " + containerPop.getId() + ", href: " + containerPop.getHref());
		System.out.println(containerPop.toJson());
		// Crea la Product Offering
	    final ProductOfferingApi offeringApi = new ProductOfferingApi(offeringClient);
	    ProductOfferingCreate poc = createProductOffering();
	    // add first price
	    var priceItem = new ProductOfferingPriceRefOrValue();
	    priceItem.href(new URI(containerPop.getHref()));
	    priceItem.id(containerPop.getId());
	    poc.addProductOfferingPriceItem(priceItem);
	    // add specifications
	    poc.setProductSpecification(productSpecRef);
	    
	    ProductOffering po = offeringApi.createProductOffering(poc);
	    
	    //System.out.println(po.toJson());
		return po;
	}
	
	
	private ProductOfferingCreate createProductOffering() {
		ProductOfferingCreate poc = new ProductOfferingCreate();
		
		poc
		.name("Your custom VM at a configurable price")
		.description("A customizable VM at a configurable price")
		.isBundle(false)
		.lifecycleStatus("Launched");
		
		return poc;
	}
	
	
	private ProductOfferingPriceCreate createContainerPop(ProductSpecificationRef productSpecRef) {
		ProductOfferingPriceCreate pop = new ProductOfferingPriceCreate();
		pop
		.name("Flexible VM Price")
		.description("Flexible VM Price, monthly billed, recurring prepaid")
		.version("1.0")
		.priceType("recurring-prepaid")
		.recurringChargePeriodLength(1)
		.recurringChargePeriodType("month")
		.isBundle(true)
		.lifecycleStatus("Launched");
		
		return pop;
	}
	
	
	private ProductOfferingPriceCreate createRAM16GBPop(ProductSpecificationRef productSpecRef) {
		Money price = new Money();
		price.value(4F).unit("EUR");
		
		ProductOfferingPriceCreate pop = new ProductOfferingPriceCreate();
		pop
		.name("16GB RAM Fixed Price")
		.description("16GB RAM Fixed Price: 4 eu per m, recurring prepaid")
		.version("1.0")
		.priceType("recurring-prepaid")
		.recurringChargePeriodLength(1)
		.recurringChargePeriodType("month")
		.isBundle(false)
		.lifecycleStatus("Launched")
		.price(price);
		
		{
			var ramValue = new CharacteristicValueSpecification();
			ramValue.isDefault(true).value(16).valueType("number").unitOfMeasure("GB");
			
			var ramSpec = new ProductSpecificationCharacteristicValueUse();
			ramSpec
			.name("RAM")
			.addProductSpecCharacteristicValueItem(ramValue)
			.setProductSpecification(productSpecRef);
			
			pop.addProdSpecCharValueUseItem(ramSpec);
		}
		
		return pop;
	}
	
	
	private ProductOfferingPriceCreate createRAM32GBPop(ProductSpecificationRef productSpecRef) {
		Money price = new Money();
		price.value(6F).unit("EUR");
		
		ProductOfferingPriceCreate pop = new ProductOfferingPriceCreate();
		pop
		.name("32GB RAM Fixed Price")
		.description("32GB RAM Fixed Price: 6 eu per m, recurring prepaid")
		.version("1.0")
		.priceType("recurring-prepaid")
		.recurringChargePeriodLength(1)
		.recurringChargePeriodType("month")
		.isBundle(false)
		.lifecycleStatus("Launched")
		.price(price);
		
		{
			var ramValue = new CharacteristicValueSpecification();
			ramValue.isDefault(true).value(32).valueType("number").unitOfMeasure("GB");
			
			var ramSpec = new ProductSpecificationCharacteristicValueUse();
			ramSpec
			.name("RAM")
			.addProductSpecCharacteristicValueItem(ramValue)
			.setProductSpecification(productSpecRef);
			
			pop.addProdSpecCharValueUseItem(ramSpec);
		}
		
		return pop;
	}
	
	
	private ProductOfferingPriceCreate createCPUFlexiblePop(ProductSpecificationRef productSpecRef) {
		Money price = new Money();
		price.value(0.4F).unit("EUR");
		
		Quantity uom = new Quantity();
		uom.amount(1F).units("unit");
		
		ProductOfferingPriceCreate pop = new ProductOfferingPriceCreate();
		pop
		.name("CPU Flexible Price")
		.description("CPU Flexible Price: 0.4 eu per m per CPU, recurring prepaid")
		.version("1.0")
		.priceType("recurring-prepaid")
		.recurringChargePeriodLength(1)
		.recurringChargePeriodType("month")
		.unitOfMeasure(uom)
		.isBundle(false)
		.lifecycleStatus("Launched")
		.price(price);
		
		{
			var ramSpec = new ProductSpecificationCharacteristicValueUse();
			ramSpec
			.name("CPU")
			.setProductSpecification(productSpecRef);
			
			pop.addProdSpecCharValueUseItem(ramSpec);
		}
		
		return pop;
	}
	
	
	private ProductOfferingPriceCreate createSixMonthsDiscount() {
		ProductOfferingPriceCreate pop = new ProductOfferingPriceCreate();
		pop
		.name("Six months 30% special price")
		.description("Six months 30% special price")
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
	
	
	private ProductOfferingPriceCreate createStorageFlexiblePop(ProductSpecificationRef productSpecRef,
			ProductOfferingPrice discountPop) throws URISyntaxException {
		Money price = new Money();
		price.value(2F).unit("EUR");
		
		Quantity uom = new Quantity();
		uom.amount(10F).units("GB");
		
		ProductOfferingPriceCreate pop = new ProductOfferingPriceCreate();
		pop
		.name("Storage Flexible Price")
		.description("Storage Flexible Price, 2 eu per m every 10GB, recurring prepaid")
		.version("1.0")
		.priceType("recurring-prepaid")
		.recurringChargePeriodLength(1)
		.recurringChargePeriodType("month")
		.unitOfMeasure(uom)
		.isBundle(false)
		.lifecycleStatus("Launched")
		.price(price);
		
		{			
			var diskSpec = new ProductSpecificationCharacteristicValueUse();
			diskSpec
			.name("Storage")
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
	
	
	private ProductOfferingPriceCreate createStorageFixedPop(ProductSpecificationRef productSpecRef) throws URISyntaxException {
		Money price = new Money();
		price.value(10F).unit("EUR");
		
		ProductOfferingPriceCreate pop = new ProductOfferingPriceCreate();
		pop
		.name("Storage Fixed Price")
		.description("Storage Fixed Price, 60GB, 10 eu per m, recurring prepaid")
		.version("1.0")
		.priceType("recurring-prepaid")
		.recurringChargePeriodLength(1)
		.recurringChargePeriodType("month")
		.isBundle(false)
		.lifecycleStatus("Launched")
		.price(price);
		
		{
			var diskValue = new CharacteristicValueSpecification();
			diskValue.isDefault(true).value(60).valueType("number").unitOfMeasure("GB");
			
			var diskSpec = new ProductSpecificationCharacteristicValueUse();
			diskSpec
			.name("Storage")
			.addProductSpecCharacteristicValueItem(diskValue)
			.setProductSpecification(productSpecRef);
			
			pop.addProdSpecCharValueUseItem(diskSpec);
		}
		
		return pop;
	}
	
	
}
