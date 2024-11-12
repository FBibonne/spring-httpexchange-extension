package fr.insee.demo.httpexchange.autobeangeneration;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.*;

import static fr.insee.demo.httpexchange.autobeangeneration.Constants.NO_ERROR_HANDLER_BEAN_NAME_VALUE;
import static fr.insee.demo.httpexchange.autobeangeneration.Constants.GLOBSTAR_SEARCH_PATH;
import static java.util.Objects.requireNonNullElse;
import static org.springframework.util.CollectionUtils.toMultiValueMap;
import static org.springframework.web.client.support.RestClientAdapter.create;

@Component
public class HttpInterfaceRegister implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        var basePackagesToScan = findBasePackagesToScanFrom(registry);
        findRestServiceClientInterface(basePackagesToScan).forEach(metadata -> {
            try {
                registry.registerBeanDefinition(getName(metadata), getBeanDefinition(metadata));
            } catch (ClassNotFoundException e) {
                throw new BeansException("No class found for class name " + getName(metadata) + " from Spring metadata", e) {
                };
            }
        });
    }

    private List<String> findBasePackagesToScanFrom(BeanDefinitionRegistry registry) {
        return Arrays.asList(findEnableHttpInterface(registry).basePackages());
    }

    /**
     * Find one bean annotated with @EnableHttpInterface and return the associated instance of
     * EnableHttpInterface
     *
     * @param registry where to search beans
     * @return an instance of  EnableHttpInterface wich annotates a bean type registred in registry
     * @throws NoSuchBeanDefinitionException if no instance of EnableHttpInterface can be returned
     */
    private EnableHttpInterface findEnableHttpInterface(BeanDefinitionRegistry registry) {
        return Arrays.stream(registry.getBeanDefinitionNames())
                .map(registry::getBeanDefinition)
                .map(this::toEnableHttpInterfaceAnnotation)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny()
                .orElseThrow(() -> new NoSuchBeanDefinitionException("No @EnableHttpInterface annotated bean found"));
    }

    private Optional<EnableHttpInterface> toEnableHttpInterfaceAnnotation(BeanDefinition beanDefinition) {
        var beanType = beanDefinition.getResolvableType().getRawClass();
        if (beanType != null) {
            return extractEnableHttpInterfaceAnnotationIfExist(beanType);
        }
        return Optional.empty();
    }

    private Optional<EnableHttpInterface> extractEnableHttpInterfaceAnnotationIfExist(Class<?> beanType) {
        return Optional.ofNullable(beanType.getAnnotation(EnableHttpInterface.class));
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
        List<Object> errorHandlerBeanNames = attributesOfAnnotationHttpInterface(metadata)
                .get("errorHandlerBeanName");
        return (CollectionUtils.isEmpty(errorHandlerBeanNames) || NO_ERROR_HANDLER_BEAN_NAME_VALUE.equals(errorHandlerBeanNames.get(0))) ?
                Optional.empty() :
                Optional.of(errorHandlerBeanNames.get(0).toString());
    }

    @NotNull
    private static MultiValueMap<String, Object> attributesOfAnnotationHttpInterface(AnnotationMetadata metadata) {
        return requireNonNullElse(metadata.getAllAnnotationAttributes(HttpInterface.class.getName()), toMultiValueMap(Map.of()));
    }


    private String findBaseUrl(AnnotationMetadata metadata) {
        return attributesOfAnnotationHttpInterface(metadata)
                .get("baseUrl")
                .get(0)
                .toString();
    }

    private String getName(AnnotationMetadata metadata) {
        return metadata.getClassName();
    }


    private List<AnnotationMetadata> findRestServiceClientInterface(List<String> basePackagesToScan) {
        var scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(@NonNull AnnotatedBeanDefinition beanDefinition) {
                var metadata = beanDefinition.getMetadata();
                return metadata.isIndependent() && metadata.isInterface();
            }
        };
        scanner.addIncludeFilter(new AnnotationTypeFilter(HttpInterface.class));
        return fillEmptyArrayWithGlobstarPackageName(basePackagesToScan).stream()
                .map(scanner::findCandidateComponents)
                .flatMap(Set::stream)
                .map(ScannedGenericBeanDefinition.class::cast)
                .map(ScannedGenericBeanDefinition::getMetadata).toList();
    }

    private List<String> fillEmptyArrayWithGlobstarPackageName(List<String> basePackagesToScan) {
        return basePackagesToScan.isEmpty() ? List.of(GLOBSTAR_SEARCH_PATH) : basePackagesToScan;
    }

    @Component(Constants.BEAN_CREATOR_NAME)
    @SuppressWarnings("unused")
    record HttpInterfaceCreator(@Autowired(required = false) Map<String, ResponseErrorHandler> errorHandlerMap) {

        public HttpInterfaceCreator {
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
