# Spring CORS Filter

This project provides a spring filter to enable CORS.

##LICENSE

This project is licensed under Apache v2.
 
##Build

```
mvn clean package
```
## Run Integration Tests
```
mvn clean verify
```

## Setup 
 * Add maven pom dependency in the client service:

```xml
	<dependency>
            <groupId>com.ge.predix</groupId>
            <artifactId>spring-cors-filter</artifactId>
            <version>1.0.0</version>
            <exclusions>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-parent</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-log4j12</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
```

* Configure properties:
 
```
cors.xhr.allowed.uris=<<put value here like...^/v2/api-docs$>>
cors.xhr.allowed.origins=<<put value here like....^.*\\.example\\.domain\\.ge\\.com$,^.*\\.predix\\.io$>>
cors.xhr.allowed.headers=<<put value here like...Origin,Accept,X-Requested-With,Content-Type,Access-Control-Request-Method,
Access-Control-Request-Headers,Authorization,username,data>>
cors.xhr.controlmaxage=<<put value here like...1728000L>>
cors.xhr.allowed.methods:<<put value here like...GET,OPTIONS>>
```

* Add a @ComponentScan annotation with base package entry of "com.ge.predix.web.cors". Example:
```java
@ComponentScan(basePackages = {"com.ge.predix.web.cors"})
```

