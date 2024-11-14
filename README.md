# Spring Framework extension to enhance @HttpExchange

@HttpExchange is an annotation from Spring Framework. It can be used to create [Http interface](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface) to easily request a Rest API from a java spring application.

**This lib makes easier the use of http interface avoiding boilerplate code**

## Getting started

### Add the lib in your classpath

for the moment, copy-paste the main code in your since it is not published on maven.

### Create your http interface with Exhange annotation

For example :
```java
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

public interface RegionContract {
    //same as @HttpExchange(method = "GET", value = "/nomenclature/{id}")
    @GetExchange("/nomenclature/{id}")
    List<Region> getRegions(@PathVariable String id);
}
```

> **BEST PRACTICE !**
> Instead of writing the interface, generate it from an OpenAPI spec in a [contract first approach](https://apihandyman.io/6-reasons-why-generating-openapi-from-code-when-designing-and-documenting-apis-sucks/)

### Annotate it with @HttpInterface

For example :
```java
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import fr.insee.demo.httpexchange.autobeangeneration.HttpInterface;

import java.util.List;

@HttpInterface(baseUrl = "https://base.url.of.the.api.I.consume/")
public interface RegionContract {
    //same as @HttpExchange(method = "GET", value = "/nomenclature/{id}")
    @GetExchange("/nomenclature/{id}")
    List<Region> getRegions(@PathVariable String id);
}
```

> TIP
> You can also extend thie interface RegionContract to keep it pure from the framework

```java
import fr.insee.demo.httpexchange.autobeangeneration.HttpInterface;

@HttpInterface(baseUrl = "https://base.url.of.the.api.I.consume/")
public interface RegionHttpInterface extends RegionContract{}
```

### Enable the lib

Just annotate a class scanned by spring with `@EnableHttpInterface` :
```java
import fr.insee.demo.httpexchange.autobeangeneration.EnableHttpInterface;

@EnableHttpInterface
// it can be a @Configuration class but it is not mandatory
class SimpleConfig{}
```

### Enjoy the generated client bean in you services

```java
@Component
public record GreatService(RegionContract regionContract){
    public List<Region> findRegionsById(RegionId regionId){
        // call the remote API, get the result, map it to Region and return the result
        return regionContract.getRegions(regionId.value());
    }
}
```

## Other features

### In `@HttpInterface`

- you can use placeholders with properties to define the base Url : `@HttpInterface(baseUrl = "${demo.baseUrl}")`
  - Just be aware to declare a `PropertySourcesPlaceholderConfigurer` bean
- you can use an `errorHandlerBeanName` attribute in the annotation to declare a bean name which will be used as an error handler
  - Example at [fr.insee.demo.httpexchange.SimpleExampleTest#withErrorHandler_ShouldHandle4xxResponseCode](https://gitlab.insee.fr/animation-developpement/communautes-dev-sndil/ateliers/spring-httpexchange/-/blob/main/src/test/java/fr/insee/demo/httpexchange/SimpleExampleTest.java?ref_type=heads#L64)
  - you must declare a bean with the name provided in `errorHandlerBeanName` attribute and whose type is[ResponseErrorHandler](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/ResponseErrorHandler.html)

### In `@EnableHttpInterface`

- you can use attribute `basePackages` to specify which packages (including subpackages) will be scanned to find @HttpInterface annotated
interfaces. If `basePackages` is not specified, all classpath will be scanned.
