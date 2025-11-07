package it.eng.dome.billing.engine.price.alteration;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import it.eng.dome.billing.engine.model.Money;
import it.eng.dome.billing.engine.utils.TmfConverter;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingTerm;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
import jakarta.validation.constraints.NotNull;

@Component(value = "discountAlterationOperation")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DiscountAlterationOperation implements PriceAlterationOperation{
    private final Logger logger = LoggerFactory.getLogger(DiscountAlterationOperation.class);

	@Override
	public PriceAlteration applyAlteration(float basePrice,  @NotNull ProductOfferingPrice alterationPOP) {
		BigDecimal discount = new BigDecimal(basePrice * (alterationPOP.getPercentage() / 100));
		discount = discount.setScale(2, RoundingMode.HALF_EVEN);
		//EuroMoney euro = new EuroMoney((basePrice - discount.floatValue()) < 0 ? 0 : basePrice - discount.floatValue());
		Float discountedAmount=(basePrice - discount.floatValue()) < 0 ? 0 : basePrice - discount.floatValue();
		Money discounteMoney = new Money(alterationPOP.getPrice().getUnit(),discountedAmount);
		
		Price price = new Price();
		//price.setDutyFreeAmount(euro.toMoney());
		price.setDutyFreeAmount(TmfConverter.convertMoneyTo622(discounteMoney));
		PriceAlteration priceAlteration = new PriceAlteration();
		priceAlteration
		.description(alterationPOP.getDescription())
		.name(alterationPOP.getName())
		.priceType(alterationPOP.getPriceType())
		.setPrice(price);
		
		if (!CollectionUtils.isEmpty(alterationPOP.getProductOfferingTerm())) {
			ProductOfferingTerm term = alterationPOP.getProductOfferingTerm().get(0);
			
			priceAlteration
			.applicationDuration(term.getDuration().getAmount())
			.setUnitOfMeasure(term.getDuration().getUnits());
		}
		
		logger.info("Applied {}% discount to price of {} euro. Discounted price: {} {}", 
				alterationPOP.getPercentage(), basePrice, discounteMoney.getValue(), discounteMoney.getUnit());
		
		return priceAlteration;
	}

}
