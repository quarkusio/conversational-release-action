package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.bot.release.util.Branches;

public class ReleaseInformationTest {

    @ParameterizedTest
    // qualifier,expectedResult
    @CsvSource(value = { ",false",
            "Alpha1,true",
            "Alpha2,true",
            "Beta1,true",
            "Beta2,true",
            "CR1,false",
            "CR2,false",
            "Final,false" }, nullValues = "null")
    public void testIsPreCR1(String qualifier, boolean expectedResult) {
        ReleaseInformation releaseInformation = new ReleaseInformation(null, "4.0", Branches.MAIN, qualifier, false, null,
                null, true, false, false);
        assertThat(releaseInformation.isPreCR1()).as("Qualifier %s", qualifier).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    // qualifier,expectedResult
    @CsvSource(value = { ",false",
            "Alpha1,false",
            "Beta1,false",
            "CR1,true",
            "CR2,false" }, nullValues = "null")
    public void testIsFirstCR(String qualifier, boolean expectedResult) {
        ReleaseInformation releaseInformation = new ReleaseInformation(null, "4.0", Branches.MAIN, qualifier, false, null,
                null, true, false, false);
        assertThat(releaseInformation.isFirstCR()).as("Qualifier %s", qualifier).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    // version,branch,qualifier,firstFinal,expectedResult
    @CsvSource({ "3.6.0.CR1,3.6,CR1,false,false",
            "3.16.2,3.16,,false,false",
            "3.20.0.CR1,3.20,CR1,false,false",
            "3.20.0,3.20,,true,false",
            "3.20.1,3.20,,false,true",
            "3.27.0,3.27,,true,false",
            "3.27.2,3.27,,false,true",
            "3.33.0,3.33,,true,false",
            "3.33.1,3.33,,false,true",
            "3.40.0,3.40,,true,false",
            "3.40.1,3.40,,false,true" })
    public void testIsLtsMaintenanceReleaseWithRegularReleaseCadence(ArgumentsAccessor argumentsAccessor) {
        String version = argumentsAccessor.getString(0);
        String branch = argumentsAccessor.getString(1);
        String qualifier = argumentsAccessor.getString(2);
        boolean firstFinal = argumentsAccessor.getBoolean(3);
        boolean expectedResult = argumentsAccessor.getBoolean(4);

        ReleaseInformation releaseInformation = new ReleaseInformation(version, branch, Branches.MAIN, qualifier, false, null,
                null, false, firstFinal, false);
        assertThat(releaseInformation.isLtsMaintenanceReleaseWithRegularReleaseCadence())
                .as("Version %s", releaseInformation.getVersion()).isEqualTo(expectedResult);
    }
}
