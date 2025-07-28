# Billing Engine

## Description
Billing Engine services for DOME project.



## Swagger REST APIs
A Swagger APIs are available at this URL `http://localhost:8080/swagger-ui.html` if you are running the **billing-engine** on `localhost` at port `8080`.

> [!NOTE] 
> In general the Swagger URL is `http://<SERVER_NAME>:<SERVER_PORT>/swagger-ui.html`.



## How to Run Application
**Start the application using these commands below**

> [!TIP] 
> Run these commands inside the root folder of this project; i.e inside the **billing-engine** folder.


> [!IMPORTANT] 
> Create a jar file using `mvn clean install` command.


- **Using maven (via command)** 
  ```
  mvn spring-boot:run
  ```

- **From jar file**
  ```
  java -jar target/billing-engine.jar
  ```

- **From Eclipse**
- Set the **Maven Build** configuration in eclipse; select in base directory the *billing-engine* workaspace, in the **goals** use `spring-boot:run`, and set the name (i.e. **billing-engine run**). 
- To run in **debug mode**, set **Java Application** configuration in eclipse, browser the **billing-engine** project and select with **search** bottom the `it.eng.dome.billing.engine.BillingEngineApplication` class. 
Don't forget to add breakpoint for debugging.

 
- **From Docker**
- Create the jar file following above compile instructions.
- Create manually Billing Engine Docker image by running: `docker build . -t billing-engine:X.Y.Z` (where `X.Y.Z` represents the version tag: i.e. 0.0.2).
- Run the Billing Engine Docker image by executing: `docker run -d -p8080:8080 billing-engine:X.Y.Z`
- If you want to give a name to the Billing Engine Docker image by run it as: `docker run -d -p8080:8080 --name billing-engine billing-engine:X.Y.Z`
- If the Billing Engine has to connect a specific **TMForum URL**, please overwrite the **env vars** `TMF_ENDPOINT` using: `docker run -p8080:8080 --name billing-engine billing-engine:X.Y.Z -e TMF_ENDPOINT=https://my-tmfoum-endpoint`. Default URL is `https://dome-dev.eng.it`. 

> [!NOTE]  
> By default spring boot application starts on port number 8080. If port 8080 is occupied in your system then you can change the port number by uncomment and updating the **server.port** property inside the **application.yaml** file that is available inside the **src > main > resources** folder.

> [!NOTE]  
> In order to use the profile (see pom.xml) please use: `mvn spring-boot:run -Pgenerate-rest-apis`


## How to Run Unit Test Cases
**Run the test cases using this command below**

> [!TIP] 
> This command needs to run inside the root folder of this project i.e inside the **billing-engine** folder

- **To run all the test cases**
  ```
  mvn test
  ```

