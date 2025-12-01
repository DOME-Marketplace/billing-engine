package it.eng.dome.billing.engine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Class to get properties set in the application.yml file
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {
	
	private Schema schema;
    private BillCycle billCycle;

	public Schema getSchema() {
		return schema;
	}

	public void setSchema(Schema schema) {
		this.schema = schema;
	}

	public BillCycle getBillCycle() {
		return billCycle;
	}

	public void setBillCycle(BillCycle billCycle) {
		this.billCycle = billCycle;
	}
	
    public static class Schema {
        private String schemaLocationRelatedParty;

		public String getSchemaLocationRelatedParty() {
			return schemaLocationRelatedParty;
		}

		public void setSchemaLocationRelatedParty(String schemaLocationRelatedParty) {
			this.schemaLocationRelatedParty = schemaLocationRelatedParty;
		}
    }

    public static class BillCycle {

        private boolean billCycleSpecEnabled = false; // default

		public boolean isBillCycleSpecEnabled() {
			return billCycleSpecEnabled;
		}

		public void setBillCycleSpecEnabled(Boolean billCycleSpecEnabled) {
			this.billCycleSpecEnabled = billCycleSpecEnabled;
		}

    }

}
