package it.eng.dome.billing.engine;

import java.net.URI;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

@SpringBootApplication
public class BillingEngineApplication implements ApplicationListener<ApplicationStartedEvent> {
	
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
		logger.info("BillingEngine is using the following AccessNode endpoint: " + BillingEngineApplication.accessNodeEndpoint.toString());
    }
    
    private static final Logger logger = LoggerFactory.getLogger(BillingEngineApplication.class);
    public static URL accessNodeEndpoint;

    public static void main(String[] args) {
    	if (args.length == 0) {
    		System.out.println("Error! No AccessNode endpoint argument provided! BillingEngine cannot start.");
    		System.out.println("Please provide as argument the endpoint of the AccessNode on the shell that is starting the BillingEngine.");
    		System.out.println("Tipical usage:");
    		System.out.println("1) java -jar target/billing-engine.jar [Access Node URL].");
    		System.out.println("2) docker run -d -p8080:8080 --name billing-engine billing-engine:X.Y.Z [Access Node URL]");
    	} 
    	
    	if (args.length >= 1) {
    		try {
    			URI tmpURI = URI.create(args[0]);
    			BillingEngineApplication.accessNodeEndpoint = tmpURI.toURL();
    	        SpringApplication.run(BillingEngineApplication.class, args);
    		} catch (Exception e) {
    			logger.error("Provided argument '" + args[0] + "' is not a valid URI", e);
    		}
    	}    	
    }

}
