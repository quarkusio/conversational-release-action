package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.bot.release.util.Users;

public class UsersTest {

    @Test
    void testIsBot() {
        assertThat(Users.isBot("gsmet")).isFalse();
        assertThat(Users.isBot(null)).isTrue();
        assertThat(Users.isBot("quarkusbot")).isTrue();
        assertThat(Users.isBot("quarkus-bot")).isTrue();
        assertThat(Users.isBot("github actions [bot]")).isTrue();
    }
}
