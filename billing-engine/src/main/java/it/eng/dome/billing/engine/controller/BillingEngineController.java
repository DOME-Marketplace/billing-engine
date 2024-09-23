package it.eng.dome.billing.engine.controller;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/engine")
public class BillingEngineController {

    private static final Logger log = LoggerFactory.getLogger(BillingEngineController.class);

    @Value("${application.name}")
    private String appName;

    @Value("${build.version}")
    private String buildVersion;

    @Value("${build.timestamp}")
    private String buildTimestamp;

    @GetMapping("/info")
    public HashMap<String, String> getInfo() {
        log.info("Request getInfo");
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("name", appName);
        map.put("version", buildVersion);
        map.put("timestamp", buildTimestamp);
        log.debug(map.toString());
        return map;
    }
}
