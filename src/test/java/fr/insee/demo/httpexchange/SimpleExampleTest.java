package fr.insee.demo.httpexchange;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import fr.insee.demo.httpexchange.autobeangeneration.EnableHttpInterface;
import fr.insee.demo.httpexchange.httpinterfaces.errorhandler.RegionHttpInterfaceWithErrorHandler;
import fr.insee.demo.httpexchange.httpinterfaces.placeholder.RegionHttpInterfaceWithPlaceholder;
import fr.insee.demo.httpexchange.httpinterfaces.simple.SimpleRegionHttpInterface;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


@WireMockTest(httpPort = SimpleExampleTest.BASE_PORT)
public class SimpleExampleTest {

    public static final int BASE_PORT = 19782;
    public static final String BASE_URL = "http://localhost:" + BASE_PORT;
    public static final String BASE_URL_PROPERTY = "demo.baseUrl";
    private static final List<Region> ALL_REGIONS = List.of(new Region("04", "La Réunion"), new Region("76", "Occitanie"));
    private static ResponseErrorHandler responseErrorHandlerStub;

    @Test
    void whenHttpExchangeInterfaceInContext_ShouldRequestCorrectlyServer() throws IOException {
        var id = "regions2019";
        stubFor(get("/nomenclature/" + id).willReturn(ok()
                .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                .withBody(regionsResponse())));

        // More efficient way to box an object than an AtomicReference : see "craking the java coding interview" : "not denotable types"
        var simpleRegionRestClient = new Object() {
            SimpleRegionHttpInterface value;
        };

        assertDoesNotThrow(() -> simpleRegionRestClient.value = (new AnnotationConfigApplicationContext(SimpleConfig.class)).getBean(SimpleRegionHttpInterface.class));
        assertThat(simpleRegionRestClient.value.getRegions(id)).isEqualTo(ALL_REGIONS);
    }

    @Test
    void withPlaceholderForBasUrl_ShouldRequestCorrectlyServer() throws IOException {
        var id = "regions2019";
        stubFor(get("/nomenclature/" + id).willReturn(ok()
                .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                .withBody(regionsResponse())));

        var simpleRegionRestClient = new Object() {
            RegionHttpInterfaceWithPlaceholder value;
        };
        System.setProperty(BASE_URL_PROPERTY, BASE_URL);
        assertDoesNotThrow(() -> simpleRegionRestClient.value = (new AnnotationConfigApplicationContext(ConfigForPropertyPlaceholder.class)).getBean(RegionHttpInterfaceWithPlaceholder.class));
        assertThat(simpleRegionRestClient.value.getRegions(id)).isEqualTo(ALL_REGIONS);
    }

    @Test
    void withErrorHandler_ShouldHandle4xxResponseCode() {
        var id = "regions2019";
        stubFor(get("/nomenclature/" + id).willReturn(badRequest()));

        var regionHttpInterfaceWithErrorHandler = new Object() {
            RegionHttpInterfaceWithErrorHandler value;
        };

        final var responseErrorHandlerSpy = new ResponseErrorHandler() {

            boolean hasError = false;
            boolean handleErrorCalled = false;

            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                hasError = HttpStatus.BAD_REQUEST.equals(response.getStatusCode());
                return hasError;
            }

            @Override
            public void handleError(ClientHttpResponse response) {
                handleErrorCalled = true;
            }
        };

        SimpleExampleTest.responseErrorHandlerStub = responseErrorHandlerSpy;

        assertDoesNotThrow(() -> regionHttpInterfaceWithErrorHandler.value = (new AnnotationConfigApplicationContext(ConfigForErrorHandler.class)).getBean(RegionHttpInterfaceWithErrorHandler.class));
        regionHttpInterfaceWithErrorHandler.value.getRegions(id);
        assertThat(responseErrorHandlerSpy.hasError).isTrue();
        assertThat(responseErrorHandlerSpy.handleErrorCalled).isTrue();
    }

    @Test
    void withCacheEnabled_ShouldCacheHttpResponse() throws IOException {
        var id = "regions2019";
        stubFor(get("/nomenclature/" + id).willReturn(ok()
                .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                .withHeader("Cache-Control", CacheControl.maxAge(1L, TimeUnit.HOURS).getHeaderValue(),
                        CacheControl.empty().mustRevalidate().getHeaderValue(),
                        CacheControl.empty().cachePrivate().getHeaderValue())
                .withBody(regionsResponse())));
        var regionHttpInterfaceWithCache = new Object() {
            SimpleRegionHttpInterface value;
        };

        assertDoesNotThrow(() -> regionHttpInterfaceWithCache.value = (new AnnotationConfigApplicationContext(SimpleConfig.class)).getBean(SimpleRegionHttpInterface.class));
        assertThat(regionHttpInterfaceWithCache.value.getRegions(id)).isEqualTo(ALL_REGIONS);
        assertThat(regionHttpInterfaceWithCache.value.getRegions(id)).isEqualTo(ALL_REGIONS);
        WireMock.verify(1, getRequestedFor(urlEqualTo("/nomenclature/" + id)));
    }

    @Test
    void withManyPackageInEnableHttpCache_shouldRetrieveManyBeans() {
        var applicationContext = new Object() {
            ApplicationContext value;
        };
        System.setProperty(BASE_URL_PROPERTY, BASE_URL);
        assertDoesNotThrow(() -> applicationContext.value = new AnnotationConfigApplicationContext(ManyPackagesConfig.class));
        assertThat(applicationContext.value.getBean(SimpleRegionHttpInterface.class)).isNotNull();
        assertThat(applicationContext.value.getBean(RegionHttpInterfaceWithPlaceholder.class)).isNotNull();
        assertThat(applicationContext.value.getBeanNamesForType(RegionHttpInterfaceWithErrorHandler.class)).isEmpty();
    }

    @Test
    void withNoPackageInEnableHttpCache_shouldRetrieveAllBeans() {
        var applicationContext = new Object() {
            ApplicationContext value;
        };
        System.setProperty(BASE_URL_PROPERTY, BASE_URL);
        assertDoesNotThrow(() -> applicationContext.value = new AnnotationConfigApplicationContext(NoPackageConfig.class));
        assertThat(applicationContext.value.getBean(SimpleRegionHttpInterface.class)).isNotNull();
        assertThat(applicationContext.value.getBean(RegionHttpInterfaceWithErrorHandler.class)).isNotNull();
        assertThat(applicationContext.value.getBean(RegionHttpInterfaceWithPlaceholder.class)).isNotNull();
    }

    @Test
    void withParentPackageInEnableHttpCache_shouldRetrieveAllBeans() {
        var applicationContext = new Object() {
            ApplicationContext value;
        };
        System.setProperty(BASE_URL_PROPERTY, BASE_URL);
        assertDoesNotThrow(() -> applicationContext.value = new AnnotationConfigApplicationContext(ParentPackagesConfig.class));
        assertThat(applicationContext.value.getBean(SimpleRegionHttpInterface.class)).isNotNull();
        assertThat(applicationContext.value.getBean(RegionHttpInterfaceWithErrorHandler.class)).isNotNull();
        assertThat(applicationContext.value.getBean(RegionHttpInterfaceWithPlaceholder.class)).isNotNull();
    }

    private String regionsResponse() throws IOException {
        try(var regionsResponseStream = SimpleExampleTest.class.getClassLoader().getResourceAsStream("regionResponse.json")){
            return new String(regionsResponseStream.readAllBytes());
        }
    }

    @EnableHttpInterface(basePackages = "fr.insee.demo.httpexchange.httpinterfaces.simple")
    @Configuration
    static class SimpleConfig {
    }

    @EnableHttpInterface(basePackages = {"fr.insee.demo.httpexchange.httpinterfaces.placeholder", "fr.insee.demo.httpexchange.httpinterfaces.simple"})
    @Configuration
    static class ManyPackagesConfig {
    }

    @EnableHttpInterface
    @Configuration
    static class NoPackageConfig {
        @Bean("responseErrorHandler")
        static ResponseErrorHandler responseErrorHandler() {
            return new ResponseErrorHandler() {
                @Override
                public boolean hasError(ClientHttpResponse response) {
                    return false;
                }

                @Override
                public void handleError(ClientHttpResponse response) {
                    //no op implementation for Stub
                }
            };
        }
    }

    @EnableHttpInterface(basePackages = "fr.insee.demo.httpexchange.httpinterfaces")
    @Configuration
    static class ParentPackagesConfig {
        @Bean("responseErrorHandler")
        static ResponseErrorHandler responseErrorHandler() {
            return new ResponseErrorHandler() {
                @Override
                public boolean hasError(ClientHttpResponse response) {
                    return false;
                }

                @Override
                public void handleError(ClientHttpResponse response) {
                    //no op implementation for Stub
                }
            };
        }
    }

    @EnableHttpInterface(basePackages = "fr.insee.demo.httpexchange.httpinterfaces.placeholder")
    @Configuration
    static class ConfigForPropertyPlaceholder extends SimpleConfig {
        @Bean
        static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }

    @EnableHttpInterface(basePackages = "fr.insee.demo.httpexchange.httpinterfaces.errorhandler")
    @Configuration
    static class ConfigForErrorHandler {
        @Bean("responseErrorHandler")
        static ResponseErrorHandler responseErrorHandler() {
            return responseErrorHandlerStub;
        }
    }

}
