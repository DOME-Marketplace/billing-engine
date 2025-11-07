package it.eng.dome.billing.engine.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {
	
	//private final AppProperties appProperties;

    /*public AppConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }*/

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
	
	/*@Bean 
	public AppProperties appProperties() {
		return appProperties;
	}*/
}
