package com.kantenkugel.discordbot.jdocparser;

import java.util.List;
import java.util.Map;

public interface Documentation {
    String getTitle();
    String getUrl();
    String getContent();
    default Map<String, List<String>> getFields() {return null;}
}
