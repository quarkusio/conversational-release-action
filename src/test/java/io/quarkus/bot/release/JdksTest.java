package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.bot.release.util.Jdks;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JdksTest {

    @Inject
    Jdks jdks;

    @Test
    public void testJdks() {
        assertThat(jdks.getJdkVersion("3.2")).isEqualTo("11");
        assertThat(jdks.getJdkVersion("3.6")).isEqualTo("11");
        assertThat(jdks.getJdkVersion("3.7")).isEqualTo("17");
        assertThat(jdks.getJdkVersion("4.0")).isEqualTo("17");
        assertThat(jdks.getJdkVersion("main")).isEqualTo("17");
    }
}
