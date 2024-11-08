package fr.insee.demo.httpexchange.autobeangeneration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HttpInterface {
    String baseUrl();
    String errorHandlerBeanName() default Constants.NO_ERROR_HANDLER_BEAN_NAME_VALUE;
}
