package com.almightyalpaca.discord.jdabutler;

import org.json.XML;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class Lavaplayer {

	public static final String	GROUP_ID	= "com.sedmelluq";
	public static final String	ARTIFACT_ID	= "lavaplayer";

	public static String getLatestVersion() {
		try {
			return XML.toJSONObject(Unirest.get("https://dl.bintray.com/sedmelluq/com.sedmelluq/com/sedmelluq/lavaplayer/maven-metadata.xml").asString().getBody()).getJSONObject("metadata")
					.getJSONObject("versioning").getString("release");
		} catch (final UnirestException e) {
			throw new RuntimeException(e);
		}
	}

}
