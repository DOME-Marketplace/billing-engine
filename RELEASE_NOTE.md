# Release Notes

**Release Notes** of the *Billing Engine* software:

### <code>1.4.4</code> :calendar: 27/10/2025
**BugFixing**
* Solved *bug fix* to calculate price for Characteristic
* Add `getTmf637EnumModule` class in the **JacksonModuleConfig** to *serialize* and *deserialize* the **TMForum enum types**.

### <code>1.4.3</code> :calendar: 23/10/2025
**Improvements**
* Removed Exception in `/billing/preciewPrice` when none `ProductOfferingPrice` match with the `productCharacteristic` of the `ProductOrder`

### <code>1.4.2</code> :calendar: 17/10/2025
**Improvements**
* Usage of the new `Brokerage Utils` version: `2.2.0`.
* Add `TmfApiConfig` class to avoid loading the **TMForum Apis** classes every time they are used in service classes.
* Add `TrailingSlashFilter` filter to remove trailing slash from request path.
* Usage of `AbstractHealthService` class from `Brokerage Utils` to manage **getInfo()** and **getHealth()** features.
* Add `TMF622EnumModule` and `TMF637EnumModule` classes in the **JacksonModuleConfig** to *serialize* and *deserialize* the **TMForum enum types**.


### <code>1.4.1</code> :calendar: 01/09/2025
**Improvements**
* Updated priceType values to include `usage` vale to manage pay-per-use use case.

### <code>1.4.0</code> :calendar: 28/07/2025
**Improvements**
* Updated price preview functionality to manage pay-per-use use case.
* Generate automatic `REST_APIs.md` file from **Swagger APIs** using the `generate-rest-apis` profile.

**BugFixing**
* Solved *bug fix* to calculate pay-per-use bill.

### <code>1.3.0</code> :calendar: 14/07/2025
**Improvements**
* Add `RelatedParty` to the `AppliedCustomerBillingRate` using the `@schemaLocation`
* Updated *Billing Service* to manage the `pay-per-use` use case.
* Display `ENV VARs` in the Listener at beginning.

**BugFixing**
* Solved *bug fix* in the bill calculate.

### <code>1.2.0</code> :calendar: 03/06/2025
**Improvements**
* Set of `[2.1.0, 2.2.0)` version of `Brokerage Utils`.
* Update paths for TMForum internal services.


### <code>1.1.0</code> :calendar: 01/04/2025
**Improvements**
* Usage of `2.0.0` version of `Brokerage Utils`.


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

