package fr.insee.demo.httpexchange;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import fr.insee.demo.httpexchange.autobeangeneration.EnableHttpInterface;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


@WireMockTest(httpPort = SimpleExampleTest.BASE_PORT)
class SimpleExampleTest {

    public static final int BASE_PORT = 19782;
    public static final String BASE_URL = "http://localhost:" + BASE_PORT;
    public static final String BASE_URL_PROPERTY = "demo.baseUrl";
    private static final List<Region> ALL_REGIONS = List.of(new Region("04", "La Réunion"), new Region("76", "Occitanie"));
    private static ResponseErrorHandler responseErrorHandlerStub;

    @Test
    void whenHttpExchangeInterfaceInContext_ShouldRequestCorrectlyServer() throws IOException {
        var id="regions2019";
        stubFor(get("/nomenclature/"+id).willReturn(ok()
                .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                .withBody(regionsResponse())));

        // More efficient way to box an object than an AtomicReference : see "craking the java coding interview" : "not denotable types"
        var simpleRegionRestClient = new Object(){
            SimpleRegionRestClient value;
        };

        assertDoesNotThrow(()->simpleRegionRestClient.value=(new AnnotationConfigApplicationContext(SimpleConfig.class)).getBean(SimpleRegionRestClient.class));
        assertThat(simpleRegionRestClient.value.getRegions(id)).isEqualTo(ALL_REGIONS);
    }

    @Test
    void withPlaceholderForBasUrl_ShouldRequestCorrectlyServer() throws IOException {
        var id="regions2019";
        stubFor(get("/nomenclature/"+id).willReturn(ok()
                .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                .withBody(regionsResponse())));

        var simpleRegionRestClient = new Object(){
            RegionRestClientWithPlaceholder value;
        };
        System.setProperty(BASE_URL_PROPERTY, BASE_URL);
        assertDoesNotThrow(()->simpleRegionRestClient.value=(new AnnotationConfigApplicationContext(ConfigForPropertyPlaceholder.class)).getBean(RegionRestClientWithPlaceholder.class));
        assertThat(simpleRegionRestClient.value.getRegions(id)).isEqualTo(ALL_REGIONS);
    }

    @Test
    void withErrorHandler_ShouldHandle4xxResponseCode(){
        var id="regions2019";
        stubFor(get("/nomenclature/"+id).willReturn(badRequest()));

        var regionHttpInterfaceWithErrorHandler = new Object(){
            RegionHttpInterfaceWithErrorHandler value;
        };

        final var responseErrorHandlerStub = new ResponseErrorHandler() {

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

        SimpleExampleTest.responseErrorHandlerStub=responseErrorHandlerStub;

        assertDoesNotThrow(()->regionHttpInterfaceWithErrorHandler.value=(new AnnotationConfigApplicationContext(ConfigForErrorHandler.class)).getBean(RegionHttpInterfaceWithErrorHandler.class));
        regionHttpInterfaceWithErrorHandler.value.getRegions(id);
        assertThat(responseErrorHandlerStub.hasError).isTrue();
        assertThat(responseErrorHandlerStub.handleErrorCalled).isTrue();
    }

    private String regionsResponse() throws IOException {
        var regionsResponseStream= SimpleExampleTest.class.getClassLoader().getResourceAsStream("regionResponse.json");
        return new String(regionsResponseStream.readAllBytes());
    }

    @EnableHttpInterface
    @Configuration
    static class SimpleConfig{
        @Bean("responseErrorHandler")
        static ResponseErrorHandler responseErrorHandler(){
            return new ResponseErrorHandler() {
                @Override
                public boolean hasError(ClientHttpResponse response) {
                    return false;
                }

                @Override
                public void handleError(ClientHttpResponse response) {

                }
            };
        }
    }

    @EnableHttpInterface
    @Configuration
    static class ConfigForPropertyPlaceholder extends SimpleConfig {
        @Bean
        static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(){
            return new PropertySourcesPlaceholderConfigurer();
        }
    }

    @EnableHttpInterface
    @Configuration
    static class ConfigForErrorHandler {
        @Bean("responseErrorHandler")
        static ResponseErrorHandler responseErrorHandler(){
            return responseErrorHandlerStub;
        }
    }

}
