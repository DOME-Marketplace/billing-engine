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

**From Eclipse**
-  Run BillingEngineApplication class, providing the Access Node Endpoint as program argument (from Run Configurations)

**From jar file**
- Create a jar file using `mvn -Dmaven.test.skip clean package` command
- Execute: `java -jar target/billing-engine.jar [Access Node URL]`
  
  
**From Docker**
- Create the jar file following above instructions
- Create the BE Docker image by running: `docker build . -t billing-engine:X.Y.Z`
- Run the BE Docker image by executing: `docker run -d -p8080:8080 billing-engine:X.Y.Z [Access Node URL]`
- If you want to give a name to the BE Docker image by run it as: `docker run -d -p8080:8080 --name billing-engine billing-engine:X.Y.Z [Access Node URL]`
- If the BE has to connect to an AccessNode that is running inside the same Docker, run the BE on the same network of the AccessNode and provide the internal port as reference and not the external: `docker run -p8080:8080 --name billing-engine --net [network name] billing-engine:0.0.2 [Access Node URL using internal port number]`

> [!NOTE]  
> By default spring boot application starts on port number 8080. If port 8080 is occupied in your system then you can change the port number by uncommenting and updating the **server.port** property inside the **application.yaml** file that is available inside the **src > main > resources** folder.



## How to Run Unit Test Cases
**Run the test cases using this command below**

> [!TIP] 
> This command needs to run inside the root folder of this project i.e inside the **billing-engine** folder

- **To run all the test cases**
  ```
  mvn test
  ```

