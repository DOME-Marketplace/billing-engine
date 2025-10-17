package it.eng.dome.billing.engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.Module;

import it.eng.dome.brokerage.utils.enumappers.TMF622EnumModule;


@Configuration
public class JacksonModuleConfig {
	
	// TMF622EnumModule handles ProductOrderStateType, ProductOrderItemStateType, OrderItemActionType, ProductStatusType, TaskStateType enums mapping 
 	@Bean
 	public Module getTmf622EnumModule() {
        return new TMF622EnumModule();
    }
}
