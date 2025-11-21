package it.eng.dome.billing.engine.exception;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import it.eng.dome.billing.engine.validator.ValidationIssue;

/**
 * Custom exception raised when the BillingEngine service finds unexpected/missing values during the validation of TMForum entities.
 * 
 */
public class BillingEngineValidationException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private final List<ValidationIssue> issues;

    public BillingEngineValidationException(List<ValidationIssue> issues) {
        super(buildMessage(issues));
        this.issues = issues;
    }
    
    public BillingEngineValidationException(ValidationIssue issue) {
        super(buildMessage(issue));
        this.issues = new ArrayList<ValidationIssue>();
        this.issues.add(issue);
    }
    

    public List<ValidationIssue> getIssues() {
        return issues;
    }

    private static String buildMessage(List<ValidationIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return "Validation successful: no issues provided";
        }

        String joinedIssues = issues.stream()
                .map(issue -> " - " + issue.toString())
                .collect(Collectors.joining("\n"));

        return "Validation failed:\n" + joinedIssues;
    }
    
    private static String buildMessage(ValidationIssue issue) {
    	
    	if (issue == null ) {
            return "Validation successful: no issue provided";
        }

        return "Validation failed:\n" + issue.toString();
    }
}

