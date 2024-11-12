package fr.insee.demo.httpexchange.httpinterfaces.simple;

import fr.insee.demo.httpexchange.autobeangeneration.HttpInterface;
import fr.insee.demo.httpexchange.httpinterfaces.RegionContract;

import static fr.insee.demo.httpexchange.SimpleExampleTest.BASE_URL;

@HttpInterface(baseUrl = BASE_URL)
public interface SimpleRegionHttpInterface extends RegionContract {
}
