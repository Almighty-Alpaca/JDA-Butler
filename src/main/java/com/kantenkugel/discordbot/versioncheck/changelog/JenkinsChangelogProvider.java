package com.kantenkugel.discordbot.versioncheck.changelog;

import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsChange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JenkinsChangelogProvider implements ChangelogProvider
{
    private final JenkinsApi jenkins;

    public JenkinsChangelogProvider(JenkinsApi jenkins)
    {
        this.jenkins = jenkins;
    }

    @Override
    public String getChangelogUrl()
    {
        return jenkins.getChangesetUrl();
    }

    @Override
    public boolean supportsIndividualLogs()
    {
        return true;
    }

    @Override
    public Changelog getChangelog(String version)
    {
        List<Changelog> changelogs = getChangelogs(version, version);
        return changelogs.isEmpty() ? null : changelogs.get(0);
    }

    @Override
    public List<Changelog> getChangelogs(String startVersion, String endVersion)
    {
        int start = extractBuild(startVersion);
        int end = extractBuild(endVersion);
        if(start == 0 || end == 0)
            return Collections.emptyList();
        if(start > end)
        {
            int tmp = start;
            start = end;
            end = tmp;
        }

        List<Changelog> changelogs = new ArrayList<>();
        for (int i = start; i <= end; i++)
        {
            JenkinsBuild build = jenkins.getBuild(i);
            if(build == null)
                continue;

            String title = build.status == JenkinsBuild.Status.SUCCESS
                    ? build.artifacts.values().iterator().next().fileNameParts.get(1)
                    : "Build " + build.buildNum + " (failed)";

            final List<JenkinsChange> changeSet = build.changes;

            final List<String> changes;
            if (changeSet.size() > 0)
            {
                changes = getChangelog(changeSet);
            }
            else
            {
                changes = Collections.singletonList("No git commits assigned");
            }

            changelogs.add(new Changelog(title, changes, build.getUrl()));
        }
        return changelogs;
    }

    private static int extractBuild(String version)
    {
        int i = version.lastIndexOf('_');
        try
        {
            return Math.max(0, Integer.parseInt(version.substring(i + 1)));
        }
        catch(NumberFormatException ex)
        {
            return 0;
        }
    }

    private static List<String> getChangelog(List<JenkinsChange> changeSet)
    {
        final List<String> fields = new ArrayList<>();

        StringBuilder builder = new StringBuilder();

        for (final JenkinsChange item : changeSet)
        {
            final String[] lines = item.commitMsg.split("\n");

            for (int j = 0; j < lines.length; j++)
            {
                final StringBuilder line = new StringBuilder();
                line.append("[`").append(j == 0 ? item.getShortId() : "`.......`").append("`](https://github.com/DV8FromTheWorld/JDA/commit/").append(item.commitId).append(")").append(" ").append(lines[j]).append("\n");

                if (builder.length() + line.length() > 1021)
                {
                    fields.add(builder.toString());
                    builder = new StringBuilder();
                }

                builder.append(line);
            }
        }

        if (builder.length() > 0)
            fields.add(builder.toString());

        return fields;

    }
}
