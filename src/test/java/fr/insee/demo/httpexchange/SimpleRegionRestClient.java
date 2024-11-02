package fr.insee.demo.httpexchange;

import fr.insee.demo.httpexchange.autobeangeneration.RestServiceClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

import static fr.insee.demo.httpexchange.SimpleExampleTest.BASE_URL;

@RestServiceClient(baseUrl = BASE_URL)
public interface SimpleRegionRestClient {

    //same as @HttpExchange(method = "GET", value = "/nomenclature/{id}")
    @GetExchange("/nomenclature/{id}")
    List<Region> getRegions(@PathVariable String id);


}
