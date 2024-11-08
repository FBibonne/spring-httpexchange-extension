package fr.insee.demo.httpexchange.autobeangeneration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fr.insee.demo.httpexchange.autobeangeneration.Constants.NO_ERROR_HANDLER_BEAN_NAME_VALUE;
import static org.springframework.web.client.support.RestClientAdapter.create;

@Component
public class HttpInterfaceRegister implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        findRestServiceClientInterface().forEach(metadata -> {
            try {
                registry.registerBeanDefinition(getName(metadata), getBeanDefinition(metadata));
            } catch (ClassNotFoundException e) {
                throw new BeansException("No class found for class name "+getName(metadata)+" from Spring metadata",e) {};
            }
        });
    }

    private BeanDefinition getBeanDefinition(AnnotationMetadata metadata) throws ClassNotFoundException {
        ConstructorArgumentValues arguments = new ConstructorArgumentValues();

        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        Class<?> targetType = Class.forName(metadata.getClassName());
        beanDefinition.setTargetType(targetType);
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        beanDefinition.setFactoryMethodName("httpInterfaceFactory");
        beanDefinition.setFactoryBeanName(Constants.BEAN_CREATOR_NAME);
        Optional<String> errorHandlerBeanName = findErrorHandlerBeanName(metadata);
        errorHandlerBeanName.ifPresent(beanDefinition::setDependsOn);
        arguments.addGenericArgumentValue(targetType);
        arguments.addGenericArgumentValue(findBaseUrl(metadata));
        arguments.addGenericArgumentValue(errorHandlerBeanName);
        beanDefinition.setConstructorArgumentValues(arguments);
        return beanDefinition;
    }

    private Optional<String> findErrorHandlerBeanName(AnnotationMetadata metadata) {
        List<Object> errorHandlerBeanNames = metadata.getAllAnnotationAttributes(HttpInterface.class.getName())
                .get("errorHandlerBeanName");
        return (CollectionUtils.isEmpty(errorHandlerBeanNames) || NO_ERROR_HANDLER_BEAN_NAME_VALUE.equals(errorHandlerBeanNames.get(0)))?
                Optional.empty():
                Optional.of(errorHandlerBeanNames.get(0).toString());
    }


    private String findBaseUrl(AnnotationMetadata metadata) {
        return metadata.getAllAnnotationAttributes(HttpInterface.class.getName())
                .get("baseUrl")
                .get(0)
                .toString();
    }

    private String getName(AnnotationMetadata metadata) {
        return metadata.getClassName();
    }


    private List<AnnotationMetadata> findRestServiceClientInterface(){
        var scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(@NonNull AnnotatedBeanDefinition beanDefinition) {
                var metadata = beanDefinition.getMetadata();
                return metadata.isIndependent() && metadata.isInterface();
            }
        };
        scanner.addIncludeFilter(new AnnotationTypeFilter(HttpInterface.class));
        var beandefs = scanner.findCandidateComponents(Constants.SEARCH_PATH_PREFIX);
        return beandefs.stream().map(ScannedGenericBeanDefinition.class::cast)
                .map(ScannedGenericBeanDefinition::getMetadata).toList();
    }

    @Component(Constants.BEAN_CREATOR_NAME)
    record HttpInterfaceCreator(@Autowired(required = false)Map<String, ResponseErrorHandler> errorHandlerMap) {

        public HttpInterfaceCreator{
            if (errorHandlerMap == null) {
                errorHandlerMap = Map.of();
            }
        }

        public <T> T httpInterfaceFactory(Class<T> clazz, String baseUrl, Optional<String> errorHandlerBeanName) {
            RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(baseUrl);
            errorHandlerBeanName.ifPresent(s -> addErrorHandlerIfFound(s, restClientBuilder));
            RestClient restClient = restClientBuilder.build();
            HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(create(restClient))
                    .build();
            return factory.createClient(clazz);
        }

        private void addErrorHandlerIfFound(String beanName, RestClient.Builder restClientBuilder) {
            ResponseErrorHandler errorHandler = errorHandlerMap.get(beanName);
            if (errorHandler != null) {
                restClientBuilder.defaultStatusHandler(errorHandler);
            }
        }

    }

}
