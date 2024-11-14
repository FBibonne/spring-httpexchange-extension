package fr.insee.demo.httpexchange.httpinterfaces.placeholder;

import fr.insee.demo.httpexchange.autobeangeneration.HttpInterface;
import fr.insee.demo.httpexchange.httpinterfaces.RegionContract;

import static fr.insee.demo.httpexchange.SimpleExampleTest.BASE_URL_PROPERTY;

@HttpInterface(baseUrl = "${"+BASE_URL_PROPERTY+"}")
public interface RegionHttpInterfaceWithPlaceholder extends RegionContract {
}
