package fr.insee.demo.httpexchange;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

public interface RegionContract {

    //same as @HttpExchange(method = "GET", value = "/nomenclature/{id}")
    @GetExchange("/nomenclature/{id}")
    List<Region> getRegions(@PathVariable String id);


}
