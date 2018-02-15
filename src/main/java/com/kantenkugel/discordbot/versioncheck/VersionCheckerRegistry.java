package com.kantenkugel.discordbot.versioncheck;

import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.versioncheck.updatehandle.JDAUpdateHandler;

public class VersionCheckerRegistry
{
    private static boolean initialized = false;
    public synchronized static void init()
    {
        if(initialized)
            return;
        initialized = true;
        register();
        VersionChecker.initUpdateLoop();
    }

    private static void register()
    {
        VersionChecker.addItem(new VersionedItem("JDA", RepoType.JCENTER, DependencyType.DEFAULT,
                "net.dv8tion", "JDA", JenkinsApi.LAST_BUILD_URL, new JDAUpdateHandler()));
        VersionChecker.addItem(new VersionedItem("Lavaplayer", RepoType.JCENTER, DependencyType.DEFAULT,
                "com.sedmelluq", "lavaplayer", "https://github.com/sedmelluq/lavaplayer#lavaplayer---audio-player-library-for-discord"));
        VersionChecker.addItem(new VersionedItem("JDA-Utilities", RepoType.JCENTER, DependencyType.POM,
                "com.jagrosh", "jda-utilities", "https://github.com/JDA-Applications/JDA-Utilities"));
    }
}
