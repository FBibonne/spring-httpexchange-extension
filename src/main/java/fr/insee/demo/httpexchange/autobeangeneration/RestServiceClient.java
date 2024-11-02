package fr.insee.demo.httpexchange.autobeangeneration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RestServiceClient {
    String baseUrl();
}
