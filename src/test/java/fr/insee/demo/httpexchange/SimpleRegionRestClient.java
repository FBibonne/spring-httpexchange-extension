package fr.insee.demo.httpexchange;

import fr.insee.demo.httpexchange.autobeangeneration.RestServiceClient;

import static fr.insee.demo.httpexchange.SimpleExampleTest.BASE_URL;

@RestServiceClient(baseUrl = BASE_URL)
public interface SimpleRegionRestClient extends RegionContract{
}
