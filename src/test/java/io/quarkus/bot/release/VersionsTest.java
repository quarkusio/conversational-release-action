package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TreeSet;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.jupiter.api.Test;

import io.quarkus.bot.release.util.Versions;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class VersionsTest {

    @Test
    void testVersions() {
        assertThat(Versions.getVersion("main").compareTo(Versions.VERSION_3_7)).isGreaterThan(0);
        assertThat(Versions.VERSION_3_2.compareTo(Versions.VERSION_3_7)).isLessThan(0);
        assertThat(Versions.VERSION_3_2.compareTo(Versions.VERSION_3_2)).isEqualTo(0);
        assertThat(Versions.getVersion("2.13").compareTo(Versions.VERSION_3_7)).isLessThan(0);
    }

    @Test
    void testGetBranch() {
        assertThat(Versions.getBranch("main").toString()).isEqualTo("999-SNAPSHOT");
        assertThat(Versions.getBranch("3.2").toString()).isEqualTo("3.2");
        assertThat(Versions.getBranch("3.2.1").toString()).isEqualTo("3.2");
        assertThat(Versions.getBranch("3.2.0.CR1").toString()).isEqualTo("3.2");
        assertThat(Versions.getBranch("2.13.9.Final").toString()).isEqualTo("2.13");
    }

    @Test
    void testPreviousMinor() {
        TreeSet<ComparableVersion> existingBranches = new TreeSet<>();
        existingBranches.add(Versions.MAIN);
        existingBranches.add(Versions.getBranch("3.8.0"));
        existingBranches.add(Versions.getBranch("3.7.0.CR1"));
        existingBranches.add(Versions.getBranch("3.6.2"));
        existingBranches.add(Versions.getBranch("3.6.1"));
        existingBranches.add(Versions.getBranch("3.6.0"));
        existingBranches.add(Versions.getBranch("3.6.0.CR1"));
        existingBranches.add(Versions.getBranch("3.5.3"));
        existingBranches.add(Versions.getBranch("2.13.9"));

        assertThat(Versions.getPreviousMinorBranch(existingBranches, Versions.getBranch("3.7"))).isEqualTo("3.6");
        assertThat(Versions.getPreviousMinorBranch(existingBranches, Versions.getBranch("3.6"))).isEqualTo("3.5");
        assertThat(Versions.getPreviousMinorBranch(existingBranches, Versions.getBranch("3.5"))).isEqualTo("2.13");
    }

    @Test
    void testUtils() {
        assertThat(Versions.getMinorVersion("3.6.1")).isEqualTo("3.6");
        assertThat(Versions.getMinorVersion("3.8.3")).isEqualTo("3.8");
        assertThat(Versions.getMinorVersion("3.2.10.Final")).isEqualTo("3.2");

        assertThat(Versions.getDot0("3.6.1")).isEqualTo("3.6.0");
        assertThat(Versions.getDot0("3.8.10")).isEqualTo("3.8.0");

        assertThat(Versions.getDot1("3.6.0")).isEqualTo("3.6.1");
        assertThat(Versions.getDot1("3.8.10")).isEqualTo("3.8.1");

        assertThat(Versions.isDot0("3.6.0")).isTrue();
        assertThat(Versions.isDot0("3.8.10")).isFalse();

        assertThat(Versions.isFirstMicroMaintenanceRelease("3.6.0")).isFalse();
        assertThat(Versions.isFirstMicroMaintenanceRelease("3.8.1")).isTrue();
        assertThat(Versions.isFirstMicroMaintenanceRelease("3.8.10")).isFalse();
    }
}
