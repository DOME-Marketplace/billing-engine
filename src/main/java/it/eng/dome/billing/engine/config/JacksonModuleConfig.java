package it.eng.dome.billing.engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.Module;

import it.eng.dome.brokerage.utils.enumappers.TMF622EnumModule;
import it.eng.dome.brokerage.utils.enumappers.TMF637EnumModule;


@Configuration
public class JacksonModuleConfig {
	
	// TMF622EnumModule handles ProductOrderStateType, ProductOrderItemStateType, OrderItemActionType, ProductStatusType, TaskStateType enums mapping 
 	@Bean
 	public Module getTmf622EnumModule() {
        return new TMF622EnumModule();
    }
 	
 	// TMF637EnumModule handles ProductStatusType enum mapping
  	@Bean
  	public Module getTmf637EnumModule() {
         return new TMF637EnumModule();
     }
}
