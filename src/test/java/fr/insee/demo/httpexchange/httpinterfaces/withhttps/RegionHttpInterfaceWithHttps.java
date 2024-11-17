package fr.insee.demo.httpexchange.httpinterfaces.withhttps;

import fr.insee.demo.httpexchange.autobeangeneration.HttpInterface;
import fr.insee.demo.httpexchange.httpinterfaces.RegionContract;

import static fr.insee.demo.httpexchange.Http2ByDefaultTest.HTTPS_BASE_URL;

@HttpInterface(baseUrl = HTTPS_BASE_URL)
public interface RegionHttpInterfaceWithHttps extends RegionContract {
}
