package com.almightyalpaca.discord.jdabutler;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.XML;

public class Lavaplayer
{

    public static final String ARTIFACT_ID = "lavaplayer";
    public static final String GROUP_ID = "com.sedmelluq";

    public static String getLatestVersion()
    {
        try
        {
            return XML.toJSONObject(Unirest.get("https://dl.bintray.com/sedmelluq/com.sedmelluq/com/sedmelluq/lavaplayer/maven-metadata.xml").asString().getBody()).getJSONObject("metadata").getJSONObject("versioning").getString("release");
        }
        catch (final UnirestException e)
        {
            throw new RuntimeException(e);
        }
    }

}
