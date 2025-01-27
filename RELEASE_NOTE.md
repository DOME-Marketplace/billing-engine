# Release Notes

**Release Notes** of the *Billing Engine* software:


### <code>0.0.8</code> :calendar: 27/01/2025
**Improvements**
* Update new version of `brokerage-utils:0.0.3`.


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

