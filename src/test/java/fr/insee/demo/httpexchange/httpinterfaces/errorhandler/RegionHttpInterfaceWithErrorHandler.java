package fr.insee.demo.httpexchange.httpinterfaces.errorhandler;

import fr.insee.demo.httpexchange.autobeangeneration.HttpInterface;
import fr.insee.demo.httpexchange.httpinterfaces.RegionContract;

import static fr.insee.demo.httpexchange.SimpleExampleTest.BASE_URL;

@HttpInterface(baseUrl = BASE_URL, errorHandlerBeanName = "responseErrorHandler")
public interface RegionHttpInterfaceWithErrorHandler extends RegionContract {}
