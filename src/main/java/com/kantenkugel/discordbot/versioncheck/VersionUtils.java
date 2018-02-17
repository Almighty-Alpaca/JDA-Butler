package com.kantenkugel.discordbot.versioncheck;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionUtils
{
    public static final Comparator<VersionSplits> VERSION_COMP = Comparator
            .comparingInt((VersionSplits s) -> s.major)
            .thenComparingInt((VersionSplits s) -> s.minor)
            .thenComparingInt((VersionSplits s) -> s.patch)
            .thenComparingInt((VersionSplits s) -> s.build)
            .thenComparing((VersionSplits s1, VersionSplits s2) ->
                    s1.preReleaseInfo == null && s2.preReleaseInfo == null ? 0
                            : s1.preReleaseInfo != null ? -1 : 1
            );
    public static final Comparator<String> VERSION_STRING_COMP = Comparator.comparing(VersionUtils::parseVersion, VERSION_COMP);

    public static VersionSplits parseVersion(String version)
    {
        return VersionSplits.parse(version);
    }

    public static class VersionSplits
    {
        //major is mandatory, others default to 0
        public final int major, minor, patch, build;
        //default to null if not present
        public final String preReleaseInfo, metaData;

        protected VersionSplits(int major, int minor, int patch, int build, String preReleaseInfo, String metaData)
        {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.build = build;
            this.preReleaseInfo = preReleaseInfo;
            this.metaData = metaData;
        }

        //major(mandatory), minor, patch, build, preRelease, metaData
        public static final Pattern EXTENDED_SEMVER_PATTERN =
                Pattern.compile("(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?(?:_(\\d+))?" +
                        "(?:-([A-Za-z0-9-.]+))?(?:\\+([A-Za-z0-9-.]+))?");

        public static VersionSplits parse(String versionString)
        {
            Matcher matcher = EXTENDED_SEMVER_PATTERN.matcher(versionString);
            if(!matcher.matches())
                throw new IllegalArgumentException("Given version string is not extended semver");
            int major = Integer.parseInt(matcher.group(1));
            int minor = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
            int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
            int build = matcher.group(4) == null ? 0 : Integer.parseInt(matcher.group(4));
            String preReleaseInfo = matcher.group(5);
            String metaData = matcher.group(6);

            return new VersionSplits(major, minor, patch, build, preReleaseInfo, metaData);
        }
    }
}
