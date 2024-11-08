package it.eng.dome.billing.engine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringfoxConfig {
	
	private final String TITLE = "Billing Engine";
	private final String DESCRIPTION = "Swagger REST APIs for the billing-engine software";
	
	@Autowired
    private BuildProperties buildProperties;

	@Bean
	public OpenAPI customOpenAPI() {
		
		String version = buildProperties.getVersion();

        return new OpenAPI()
                .info(new Info().title(TITLE)
                .description(DESCRIPTION)
                .version(version));
    }

}
