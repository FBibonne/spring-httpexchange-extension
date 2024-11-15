package fr.insee.test;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.impl.cache.CacheConfig;
import org.apache.hc.client5.http.impl.cache.CachingH2AsyncClientBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@WireMockTest(httpPort = 8080)
class OkHttpAlone {

    @Test
    void okHttpAlone_works() throws IOException {
        var ok = "OK";
        String path = "/test";


        var client = CachingH2AsyncClientBuilder.create()
                .setCacheConfig(
                        CacheConfig.custom()
                                .setNeverCacheHTTP10ResponsesWithQueryString(false)
                                .setSharedCache(true)
                                .build()
                )
                .build();


        stubFor(get(path).willReturn(ok()
                .withBody(ok)));

        //OkHttpClient client = new OkHttpClient();
        //Request.Builder requestBuilder = new Request.Builder();

        var url = "http://localhost:8080" + path;

        client.start();
        var request = SimpleRequestBuilder.get(url).build();



        var response = client.execute(request, )

//        try (Response response = client.newCall(requestBuilder.build()).execute()) {
//            assertThat(response.body().string()).hasToString(ok);
//        }
    }

}
