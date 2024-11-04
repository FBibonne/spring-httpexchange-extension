package fr.insee.demo.httpexchange;

import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import static org.assertj.core.api.Assertions.assertThat;

class CheckAntPathMatcherTest {

    @Test
    void shouldRetrieveAllResources(){
        var matchPattern = "**/**/*.class";
        AntPathMatcher matcher = new AntPathMatcher();
        assertThat(matcher.match(matchPattern, "fr/insee/demo/httpexchange/CheckAntPathMatcher.class")).isTrue();
        assertThat(matcher.match(matchPattern, "ClassToBeFoundAtRoot.class")).isTrue();
        assertThat(matcher.match(matchPattern, "./ClassToBeFoundAtRoot.class")).isTrue();
        assertThat(matcher.match(matchPattern, "fr/ClassToBeFoundAtFr.class")).isTrue();
        assertThat(matcher.match(matchPattern, "./fr/ClassToBeFoundAtFr.class")).isTrue();
    }

}
