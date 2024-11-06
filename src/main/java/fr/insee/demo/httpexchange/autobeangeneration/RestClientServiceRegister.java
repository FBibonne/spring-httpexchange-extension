package fr.insee.demo.httpexchange.autobeangeneration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
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
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;

import static org.springframework.web.client.support.RestClientAdapter.create;

@Component(RestClientServiceRegister.BEAN_NAME)
public class RestClientServiceRegister implements BeanDefinitionRegistryPostProcessor {

    /**
     * Set at <code>** /** /*.class</code> an AntPathMatcher to search all classes
     */
    public static final String SEARCH_PATH_PREFIX = "**";
    public static final String BEAN_NAME = "restClientServiceRegister";

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
        beanDefinition.setFactoryMethodName("restServiceFactory");
        beanDefinition.setFactoryBeanName(BEAN_NAME);
        arguments.addGenericArgumentValue(targetType);
        arguments.addGenericArgumentValue(findBaseUrl(metadata));
        beanDefinition.setConstructorArgumentValues(arguments);
        return beanDefinition;
    }

    public <T> T restServiceFactory(Class<T> clazz, String baseUrl) {
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(create(restClient)).build();
        return factory.createClient(clazz);
    }

    private String findBaseUrl(AnnotationMetadata metadata) {
        return metadata.getAllAnnotationAttributes(RestServiceClient.class.getName())
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
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestServiceClient.class));
        var beandefs = scanner.findCandidateComponents(SEARCH_PATH_PREFIX);
        return beandefs.stream().map(ScannedGenericBeanDefinition.class::cast)
                .map(ScannedGenericBeanDefinition::getMetadata).toList();
    }

}
