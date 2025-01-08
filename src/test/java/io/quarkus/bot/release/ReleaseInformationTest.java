package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.bot.release.util.Branches;

public class ReleaseInformationTest {

    @ParameterizedTest
    // version,branch,qualifier,firstFinal,expectedResult
    @CsvSource({ "3.6.0.CR1,3.6,CR1,false,false",
            "3.8.2,3.8,,false,false",
            "3.15.0.CR1,3.15,CR1,false,false",
            "3.15.0,3.15,,true,false",
            "3.15.1,3.15,,true,false",
            "3.15.2,3.15,,false,true",
            "3.16.2,3.16,,false,false",
            "3.20.0,3.20,,true,false",
            "3.20.1,3.20,,false,true"})
    public void testIsLtsMaintenanceReleaseWithRegularReleaseCadence(ArgumentsAccessor argumentsAccessor) {
        String version = argumentsAccessor.getString(0);
        String branch = argumentsAccessor.getString(1);
        String qualifier = argumentsAccessor.getString(2);
        boolean firstFinal = argumentsAccessor.getBoolean(3);
        boolean expectedResult = argumentsAccessor.getBoolean(4);

        ReleaseInformation releaseInformation = new ReleaseInformation(version, branch, Branches.MAIN, qualifier, false, firstFinal, false);
        assertThat(releaseInformation.isLtsMaintenanceReleaseWithRegularReleaseCadence()).as("Version %s", releaseInformation.getVersion()).isEqualTo(expectedResult);
    }
}
