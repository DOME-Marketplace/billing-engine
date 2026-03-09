package it.eng.dome.billing.engine.utils;

import it.eng.dome.tmforum.tmf620.v4.model.CharacteristicValueSpecification;
import jakarta.validation.constraints.NotNull;

public class CharacteristicUtils {
	
	public static boolean isRangeCharacteristic(@NotNull CharacteristicValueSpecification ch) {
		if(ch.getValueFrom()!=null && ch.getValueTo()!=null)
			return true;
		return false;
	}
	
	public static boolean isValueInCharacteristicRange(@NotNull Integer value, @NotNull CharacteristicValueSpecification ch) {
		Integer validFrom=ch.getValueFrom();
		Integer validTo=ch.getValueTo();
		return value >= validFrom && value <= validTo;
	}
	

}
