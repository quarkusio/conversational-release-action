package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.bot.release.util.Branches;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BranchesTest {

    @Test
    void testPreviewRelease() {
        ReleaseInformation releaseInformation = new ReleaseInformation("3.6.0.CR1", "3.6", Branches.MAIN, "CR1", false, false, false, false);

        assertThat(Branches.getPlatformPreparationBranch(releaseInformation)).isEqualTo(Branches.MAIN);
        assertThat(Branches.getPlatformReleaseBranch(releaseInformation)).isEqualTo(Branches.MAIN);
    }

    @Test
    void testFirstFinalRelease() {
        ReleaseInformation releaseInformation = new ReleaseInformation("3.6.0", "3.6", Branches.MAIN, null, false, false, true, false);

        assertThat(Branches.getPlatformPreparationBranch(releaseInformation)).isEqualTo(Branches.MAIN);
        assertThat(Branches.getPlatformReleaseBranch(releaseInformation)).isEqualTo("3.6");
    }

    @Test
    void testBugfixFinalRelease() {
        ReleaseInformation releaseInformation = new ReleaseInformation("3.6.1", "3.6", Branches.MAIN, null, false, false, false, false);

        assertThat(Branches.getPlatformPreparationBranch(releaseInformation)).isEqualTo("3.6");
        assertThat(Branches.getPlatformReleaseBranch(releaseInformation)).isEqualTo("3.6");
    }

    @Test
    void testLts() {
        assertThat(Branches.isLts("2.13")).isTrue();
        assertThat(Branches.isLts("2.14")).isFalse();
        assertThat(Branches.isLts("3.2")).isTrue();
        assertThat(Branches.isLts("3.4")).isFalse();
        assertThat(Branches.isLts("3.8")).isTrue();
        assertThat(Branches.isLts("3.9")).isFalse();
        assertThat(Branches.isLts("3.15")).isTrue();
        assertThat(Branches.isLts("3.20")).isTrue();
    }

    @Test
    void testGetLtsVersionsReleasedBefore() {
        assertThat(Branches.getLtsVersionsReleasedBefore("3.1")).containsExactly("2.13");
        assertThat(Branches.getLtsVersionsReleasedBefore("3.2")).containsExactly("2.13");
        assertThat(Branches.getLtsVersionsReleasedBefore("3.3")).containsExactly("2.13", "3.2");
        assertThat(Branches.getLtsVersionsReleasedBefore("3.8")).containsExactly("2.13", "3.2");
        assertThat(Branches.getLtsVersionsReleasedBefore("3.9")).containsExactly("2.13", "3.2", "3.8");
        assertThat(Branches.getLtsVersionsReleasedBefore("3.16")).containsExactly("2.13", "3.2", "3.8", "3.15");
    }
}
