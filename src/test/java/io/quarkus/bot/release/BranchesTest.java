package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.quarkus.bot.release.util.Branches;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BranchesTest {

    @Test
    void testPreviewRelease() {
        ReleaseInformation releaseInformation = new ReleaseInformation("3.6.0.CR1", "3.6", Branches.MAIN, "CR1", false, null,
                false,
                false, false);

        assertThat(Branches.getPlatformPreparationBranch(releaseInformation)).isEqualTo(Branches.MAIN);
        assertThat(Branches.getPlatformReleaseBranch(releaseInformation)).isEqualTo(Branches.MAIN);
    }

    @Test
    void testFirstFinalRelease() {
        ReleaseInformation releaseInformation = new ReleaseInformation("3.6.0", "3.6", Branches.MAIN, null, false, null, false,
                true,
                false);

        assertThat(Branches.getPlatformPreparationBranch(releaseInformation)).isEqualTo(Branches.MAIN);
        assertThat(Branches.getPlatformReleaseBranch(releaseInformation)).isEqualTo("3.6");
    }

    @Test
    void testFirstFinalLtsRelease() {
        ReleaseInformation releaseInformation = new ReleaseInformation("3.20.0", "3.20", Branches.MAIN, null, false, null,
                false,
                true, false);

        assertThat(Branches.getPlatformPreparationBranch(releaseInformation)).isEqualTo("3.20");
        assertThat(Branches.getPlatformReleaseBranch(releaseInformation)).isEqualTo("3.20");
    }

    @Test
    void testBugfixFinalRelease() {
        ReleaseInformation releaseInformation = new ReleaseInformation("3.6.1", "3.6", Branches.MAIN, null, false, null, false,
                false,
                false);

        assertThat(Branches.getPlatformPreparationBranch(releaseInformation)).isEqualTo("3.6");
        assertThat(Branches.getPlatformReleaseBranch(releaseInformation)).isEqualTo("3.6");
    }

    @Test
    void testLts() {
        assertThat(Branches.isLts("3.2")).isTrue();
        assertThat(Branches.isLts("3.4")).isFalse();
        assertThat(Branches.isLts("3.8")).isTrue();
        assertThat(Branches.isLts("3.9")).isFalse();
        assertThat(Branches.isLts("3.15")).isTrue();
        assertThat(Branches.isLts("3.20")).isTrue();
    }

    @Test
    void testGetLtsVersionsReleasedBefore() {
        assertThat(Branches.getLtsVersionsReleasedBefore("3.3")).containsExactly("3.2");
        assertThat(Branches.getLtsVersionsReleasedBefore("3.8")).containsExactly("3.2");
        assertThat(Branches.getLtsVersionsReleasedBefore("3.9")).containsExactly("3.2", "3.8");
        assertThat(Branches.getLtsVersionsReleasedBefore("3.16")).containsExactly("3.2", "3.8", "3.15");
    }

    @Test
    void testGetNextMinor() {
        assertThat(Branches.getNextMinor("1.0")).isEqualTo("1.1");
        assertThat(Branches.getNextMinor("2.15")).isEqualTo("2.16");
        assertThat(Branches.getNextMinor("10.99")).isEqualTo("10.100");
    }

    @Test
    void testGetPreviousMinor() {
        assertThat(Branches.getPreviousMinor("1.1")).isEqualTo("1.0");
        assertThat(Branches.getPreviousMinor("2.15")).isEqualTo("2.14");
        assertThat(Branches.getPreviousMinor("10.99")).isEqualTo("10.98");

        assertThatThrownBy(() -> Branches.getPreviousMinor("1.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to generate previous minor for .0 releases: 1.0");
    }
}
