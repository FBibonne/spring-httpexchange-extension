package fr.insee.demo.httpexchange.autobeangeneration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    /**
     * Set at <code>** /** /*.class</code> an AntPathMatcher to search all classes
     */
    public static final String GLOBSTAR_SEARCH_PATH = "**";
    public static final String BEAN_CREATOR_NAME = "httpInterfaceCreator";
    public static final String NO_ERROR_HANDLER_BEAN_NAME_VALUE = "";
}
