package com.kantenkugel.discordbot.versioncheck;

public enum RepoType
{
    JCENTER("http://jcenter.bintray.com/"),
    MAVENCENTRAL("https://repo.maven.apache.org/maven2/");


    private final String repoBase;

    RepoType(String repoBase)
    {
        this.repoBase = repoBase;
    }

    public String getRepoBase()
    {
        return repoBase;
    }
}
