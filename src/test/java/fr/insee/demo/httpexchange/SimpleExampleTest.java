package fr.insee.demo.httpexchange;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import fr.insee.demo.httpexchange.autobeangeneration.EnableRestServiceClientRegister;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

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
        stubFor(get("/nomenclature/"+id).willReturn(ok()
                .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                .withBody(regionsResponse())));

        AtomicReference<SimpleRegionRestClient> simpleRegionRestClient = new AtomicReference<>();

        assertDoesNotThrow(()->simpleRegionRestClient.set((new AnnotationConfigApplicationContext(Config.class)).getBean(SimpleRegionRestClient.class)));
        assertThat(simpleRegionRestClient.get().getRegions(id)).isEqualTo(ALL_REGIONS);
    }

    private String regionsResponse() throws IOException {
        var regionsResponseStream= SimpleExampleTest.class.getClassLoader().getResourceAsStream("regionResponse.json");
        return new String(regionsResponseStream.readAllBytes());
    }

    @EnableRestServiceClientRegister
    static class Config {
    }

}
