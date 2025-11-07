package it.eng.dome.billing.engine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "schema")
public class AppProperties {
	
	private String  schemaLocationRelatedParty;
	

	public String getSchemaLocationRelatedParty() {
		return schemaLocationRelatedParty;
	}
	public void setSchemaLocationRelatedParty(String schemaLocationRelatedParty) {
		this.schemaLocationRelatedParty = schemaLocationRelatedParty;
	}

}
