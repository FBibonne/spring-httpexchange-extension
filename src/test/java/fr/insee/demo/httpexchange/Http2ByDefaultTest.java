package fr.insee.demo.httpexchange;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import fr.insee.demo.httpexchange.autobeangeneration.EnableHttpInterface;
import fr.insee.demo.httpexchange.httpinterfaces.withhttps.RegionHttpInterfaceWithHttps;
import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static fr.insee.demo.httpexchange.SimpleExampleTest.BASE_HOST_PORT;
import static fr.insee.demo.httpexchange.SimpleExampleTest.regionsResponse;
import static org.assertj.core.api.Assertions.assertThat;

public class Http2ByDefaultTest {

    public static final String HTTPS_BASE_URL="https://"+BASE_HOST_PORT;

    @Test
    void withDefaults_checkHttp2IsEnabled() throws IOException {
        var id = "regions2019";
        Options options = WireMockConfiguration.options()
                .httpsPort(SimpleExampleTest.BASE_PORT)
                .http2TlsDisabled(false);

        WireMockServer server = new WireMockServer(options);
        server.stubFor(prepareWireMockWithoutOption(id));
        server.start();
        prepareWireMockWithoutOption(id);
        (new AnnotationConfigApplicationContext(SimpleConfigForHttps.class)).getBean(RegionHttpInterfaceWithHttps.class).getRegions(id);
        assertThat(server.getAllServeEvents().get(0).getRequest().getProtocol()).hasToString("HTTP/2");
        server.stop();
    }

    private MappingBuilder prepareWireMockWithoutOption(String id) throws IOException {
        ResponseDefinitionBuilder responseDefinitionBuilder = ok()
                .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                .withBody(regionsResponse());
        return get("/nomenclature/" + id).willReturn(responseDefinitionBuilder);
    }

    @EnableHttpInterface(basePackages = "fr.insee.demo.httpexchange.httpinterfaces.withhttps")
    @Configuration
    static class SimpleConfigForHttps {
    }
}
