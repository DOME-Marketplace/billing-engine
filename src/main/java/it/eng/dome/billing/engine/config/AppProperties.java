package it.eng.dome.billing.engine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "")
public class AppProperties {
	
	
	private Schema schema;
    private BillCycle billCycle;


	public Schema getSchema() {
		return schema;
	}

	public BillCycle getBillCycle() {
		return billCycle;
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
        private boolean billCycleSpecEnabled;

		public boolean isBillCycleSpecEnabled() {
			return billCycleSpecEnabled;
		}

		public void setBillCycleSpecEnabled(boolean billCycleSpecEnabled) {
			this.billCycleSpecEnabled = billCycleSpecEnabled;
		}

        // getter & setter
    }

}
