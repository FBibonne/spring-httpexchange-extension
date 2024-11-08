package fr.insee.demo.httpexchange;

import fr.insee.demo.httpexchange.autobeangeneration.HttpInterface;

import static fr.insee.demo.httpexchange.SimpleExampleTest.BASE_URL;

@HttpInterface(baseUrl = BASE_URL, errorHandlerBeanName = "responseErrorHandler")
public interface RegionHttpInterfaceWithErrorHandler extends RegionContract{
}
