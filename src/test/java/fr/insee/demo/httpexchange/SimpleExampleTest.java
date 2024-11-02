package fr.insee.demo.httpexchange;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


@WireMockTest(httpPort = SimpleExampleTest.BASE_PORT)
class SimpleExampleTest {

    public static final int BASE_PORT = 19782;
    public static final String BASE_URL = "http://localhost:" + BASE_PORT;
    private static final List<Region> ALL_REGIONS = List.of(new Region("04", "La Réunion"), new Region("76", "Occitanie"));


    @Test
    void whenHttpExchangeInterfaceInContext_ShouldRequestCorrectlyServer() throws IOException {
        var id="regions2019";
        stubFor(get("/nomenclature/"+id).willReturn(ok().withBody(regionsResponse())));

        AtomicReference<SimpleRegionRestClient> simpleRegionRestClient = new AtomicReference<>();

        assertDoesNotThrow(()->simpleRegionRestClient.set((new AnnotationConfigApplicationContext(Config.class)).getBean(SimpleRegionRestClient.class)));
        assertThat(simpleRegionRestClient.get().getRegions(id)).isEqualTo(ALL_REGIONS);
    }

    private String regionsResponse() throws IOException {
        var regionsResponseStream= SimpleExampleTest.class.getClassLoader().getResourceAsStream("regionResponse.json");
        return new String(regionsResponseStream.readAllBytes());
    }

    @Configuration
    static class Config {}

}
