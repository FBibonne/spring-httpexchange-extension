package fr.insee.demo.httpexchange;

import fr.insee.demo.httpexchange.autobeangeneration.HttpInterface;

import static fr.insee.demo.httpexchange.SimpleExampleTest.BASE_URL;
import static fr.insee.demo.httpexchange.SimpleExampleTest.BASE_URL_PROPERTY;

@HttpInterface(baseUrl = "${"+BASE_URL_PROPERTY+"}")
public interface RegionRestClientWithPlaceholder extends RegionContract{
}
