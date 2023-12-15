package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.bot.release.util.Users;

public class UsersTest {

    @Test
    void testIsBot() {
        assertThat(Users.isIgnored("gsmet")).isFalse();
        assertThat(Users.isIgnored(null)).isTrue();
        assertThat(Users.isIgnored("quarkusbot")).isFalse();
        assertThat(Users.isIgnored("quarkus-bot")).isTrue();
        assertThat(Users.isIgnored("github actions [bot]")).isTrue();
    }
}
