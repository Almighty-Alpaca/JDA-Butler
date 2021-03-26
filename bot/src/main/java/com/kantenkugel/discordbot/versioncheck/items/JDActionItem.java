package com.kantenkugel.discordbot.versioncheck.items;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.kantenkugel.discordbot.versioncheck.RepoType;
import com.kantenkugel.discordbot.versioncheck.VersionChecker;
import okhttp3.Request;
import okhttp3.Response;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

public class JDActionItem extends VersionedItem
{
    private static final String versionDir =
            "https://plugins.gradle.org/m2/com/sedmelluq/jdaction/com.sedmelluq.jdaction.gradle.plugin/maven-metadata.xml";

    private static final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

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
    public Set<RepoType> getAdditionalRepositories() {
        return Collections.emptySet();
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
        try(Response res = Bot.httpClient.newCall(req).execute())
        {

            if(!res.isSuccessful())
            {
                VersionChecker.LOG.warn("Http call to JDAction repo failed");
                return null;
            }

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(res.body().byteStream());

            Element root = doc.getDocumentElement();
            root.normalize();

            Element versioningElem = (Element) root.getElementsByTagName("versioning").item(0);
            if (versioningElem == null)
            {
                VersionChecker.LOG.warn("Could not find versioning node for JDAction");
                return null;
            }

            Element versionElem = (Element) versioningElem.getElementsByTagName("release").item(0);
            if (versionElem == null)
            {
                VersionChecker.LOG.warn("Could not find release node for JDAction");
                return null;
            }

            return versionElem.getTextContent();
        }
        catch(SocketTimeoutException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch(SAXException | ParserConfigurationException | IOException e)
        {
            VersionChecker.LOG.warn("There was an error fetching the newest version of JDAction", e);
        }
        return null;
    }
}
