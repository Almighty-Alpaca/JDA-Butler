package com.kantenkugel.discordbot.versioncheck.items;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.kantenkugel.discordbot.versioncheck.RepoType;
import com.kantenkugel.discordbot.versioncheck.VersionUtils;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.function.Supplier;

public class JDActionItem extends VersionedItem
{
    private static final String versionDir =
            "https://plugins.gradle.org/m2/com/sedmelluq/jdaction/com.sedmelluq.jdaction.gradle.plugin/";

    @Override
    public String getName()
    {
        return "JDAction";
    }

    @Override
    public RepoType getRepoType()
    {
        return null;
    }

    @Override
    public String getGroupId()
    {
        return null;
    }

    @Override
    public String getArtifactId()
    {
        return null;
    }

    @Override
    public String getUrl()
    {
        return "https://github.com/sedmelluq/jdaction";
    }

    @Override
    public Supplier<String> getCustomVersionSupplier()
    {
        return this::getCustomVersion;
    }

    private String getCustomVersion()
    {
        Request req = new Request.Builder().get().url(versionDir).build();
        try
        {
            Response res = Bot.httpClient.newCall(req).execute();
            Document htmlDoc = Jsoup.parse(res.body().byteStream(), "UTF-8", versionDir);
            Elements links = htmlDoc.getElementsByTag("a");
            return links.stream()
                    .map(e ->
                    {
                        String text = e.text();
                        return text.substring(0, text.length() - 1);
                    })
                    .max(VersionUtils.VERSION_STRING_COMP)
                    .orElse(null);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
