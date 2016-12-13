package com.almightyalpaca.discord.jdabutler;

import org.json.XML;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class Lavaplayer {

	public static final String	GROUP_ID	= "com.sedmelluq";
	public static final String	ARTIFACT_ID	= "lavaplayer";

	public static final String	REPO_URL	= "http://maven.sedmelluq.com/";
	public static final String	REPO_NAME	= "sedmelluq";
	public static final String	REPO_ID		= "sedmelluq";

	public static String getLatestVersion() {
		try {
			return XML.toJSONObject(Unirest.get("http://maven.sedmelluq.com/com/sedmelluq/lavaplayer/maven-metadata-local.xml").asString().getBody()).getJSONObject("metadata").getJSONObject(
					"versioning").getString("release");
		} catch (final UnirestException e) {
			throw new RuntimeException(e);
		}
	}

}
