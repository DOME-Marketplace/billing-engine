# Release Notes

**Release Notes** of the *Billing Engine* software:

### <code>1.0.1</code> :calendar: 10/03/2025
**Improvements**
* Updated API `/billing/bill` to calculate the correct amount of the Bill of a Product.
* Add `StartupListener` listener to log (display) the current version of *Billing Engine* at startup.

**BugFixing**
* Set pattern console to `%d{yyyy-MM-dd HH:mm:ss} [%-5level] %logger{36} - %msg%n`.


### <code>1.0.0</code> :calendar: 19/02/2025
**Bug fixing**
* Updated API `/billing/previewPrice` to manage the correct price calculation for ProductOrder with customer configuration.
* Set `org.apache.coyote.http11: ERROR` to avoid the `Error parsing HTTP request header`.


### <code>0.0.9</code> :calendar: 29/01/2025
**Improvements**
* Updated API `/billing/previewPrice` to include aggregation in **OrderTotalPrice**.


### <code>0.0.8</code> :calendar: 27/01/2025
**Improvements**
* Update to newest versions of `brokerage-utils` from 0.0 to 1.0 (`[0.0, 1.0)`).


### <code>0.0.7</code> :calendar: 20/01/2025
**Improvements**
* Deprecated **price preview** functionality via `/price/order` REST APIs in favour of **price preview** functionality via `/billing/previewPrice` REST APIs.


### <code>0.0.6</code> :calendar: 16/01/2025
**Improvements**
* Add `apiProxy` settings via **environment variables**. Set TMF_ENVOY to `true`, TMF_NAMESPACE, TMF_POSTFIX, and TMF_PORT to apply it.


### <code>0.0.5</code> :calendar: 20/12/2024
**Feature**
* Add **calculate bill** functionality via `/billing/bill` REST APIs.

### <code>0.0.4</code> :calendar: 05/12/2024
**Feature**
* Included the **actuator** feature in `pom.xml` to get the **health** info via REST APIs (`http://localhost:9000/health`).


### <code>0.0.3</code> :calendar: 08/11/2024
**Feature**
* Add **price preview** functionality via `/price/order` REST APIs.


### <code>0.0.2</code> :calendar: 08/11/2024
**Feature**
* Add swagger UI for REST APIs.

**Improvements**
* Usage of **BuildProperties** to get info from `pom.xml` instead of from the `application.yaml`.


### <code>0.0.1</code> :calendar: 24/09/2024
**Feature**
* Init project.

