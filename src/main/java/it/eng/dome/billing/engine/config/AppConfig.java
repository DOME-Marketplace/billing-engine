package it.eng.dome.billing.engine.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

	// register RestClient for serializing
	@Bean
	public RestClient restClient(ObjectMapper objectMapper) {

		MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);

		return RestClient.builder().messageConverters(converters -> {
			converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
			converters.add(jacksonConverter);
		}).build();
	}
}
