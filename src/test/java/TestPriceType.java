import it.eng.dome.brokerage.billing.utils.ProductOfferingPriceUtils;
import it.eng.dome.brokerage.model.PriceType;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;

public class TestPriceType {
	
	public static void main(String[] args) {
		ProductOfferingPrice pop=new ProductOfferingPrice();
		
		pop.setPriceType("oneTime");
		
		ProductOfferingPrice pop2=new ProductOfferingPrice();
		
		pop2.setPriceType("one Time");
		
		ProductOfferingPrice pop3=new ProductOfferingPrice();
		
		pop3.setPriceType("recurring");
		
		PriceType priceType=PriceType.fromString(pop.getPriceType());
		System.out.println(priceType);
		System.out.println(priceType.name());
		System.out.println(priceType.toString());
		
		System.out.println(ProductOfferingPriceUtils.getPriceType(pop).equals(PriceType.ONE_TIME));
		System.out.println(ProductOfferingPriceUtils.getPriceType(pop2).equals(PriceType.ONE_TIME));
		System.out.println(ProductOfferingPriceUtils.getPriceType(pop3).equals(PriceType.ONE_TIME));
		System.out.println(ProductOfferingPriceUtils.getPriceType(pop2).equals(PriceType.RECURRING));
		
		
	}

}
