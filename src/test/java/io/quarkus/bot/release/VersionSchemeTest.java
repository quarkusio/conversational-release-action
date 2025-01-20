package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.Test;

import io.smallrye.common.version.VersionScheme;

public class VersionSchemeTest {

    private static List<String> SORTED_VERSIONS = List.of("3.14.5", "3.15.0.Alpha1", "3.15.0.Alpha2", "3.15.0.Beta1",
            "3.15.0.CR1", "3.15.0", "3.15.0.1", "3.15.0.2", "3.15.1", "3.15.1.1", "3.15.2", "3.16.0");
    private static List<String> SHUFFLED_VERSIONS;

    static {
        SHUFFLED_VERSIONS = new ArrayList<>(SORTED_VERSIONS);
        Collections.shuffle(SHUFFLED_VERSIONS);
    }

    @Test
    public void testMavenImplementation() {
        TreeSet<ComparableVersion> mavenVersions = new TreeSet<>();
        for (String version : SHUFFLED_VERSIONS) {
            mavenVersions.add(new ComparableVersion(version));
        }

        assertThat(mavenVersions.stream().map(v -> v.toString()).toList())
                .containsExactly(SORTED_VERSIONS.toArray(new String[0]));
    }

    @Test
    public void testSmallRyeCommonImplementation() {
        List<String> smallryeVersions = new ArrayList<>(SHUFFLED_VERSIONS);
        Collections.sort(smallryeVersions, VersionScheme.MAVEN);

        assertThat(smallryeVersions).containsExactly(SORTED_VERSIONS.toArray(new String[0]));
    }
}
