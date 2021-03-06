/*
 * Copyright (c) 2018 Tobias Raatiniemi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sonar.plugins.objectivec.surefire;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.config.MapSettings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.plugins.objectivec.core.ObjectiveC;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class SurefireSensorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final Path resourcePath = Paths.get("src", "test", "resources", "surefire", "reports");
    private final MapSettings settings = new MapSettings();

    private SurefireSensor sensor;
    private SensorContextTester context;

    @Before
    public void setUp() {
        sensor = new SurefireSensor(settings);
        context = SensorContextTester.create(temporaryFolder.getRoot());
    }

    @After
    public void tearDown() {
        temporaryFolder.delete();
    }

    @Nonnull
    private DefaultInputFile createFile(@Nonnull String relativePath) {
        return new DefaultInputFile(context.module().key(), relativePath)
                .setLanguage("bla")
                .setType(InputFile.Type.TEST)
                .initMetadata("1\n2\n3\n4\n5\n6");
    }

    private void addFileToFs(@Nonnull DefaultInputFile inputFile) {
        context.fileSystem().add(inputFile);
    }

    @Nonnull
    private <T extends Serializable> T getMeasure(@Nonnull String componentKey, @Nonnull String testKey) {
        Measure<T> measure = context.measure(componentKey, testKey);

        return measure.value();
    }

    @Test
    public void describe() {
        DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();

        sensor.describe(descriptor);

        assertEquals("Surefire sensor", descriptor.name());
        assertTrue(descriptor.languages().contains(ObjectiveC.KEY));
    }

    @Test
    public void execute() {
        settings.setProperty("sonar.junit.reportsPath", resourcePath.toString());
        addFileToFs(createFile("FirstClassNameTest.m"));
        addFileToFs(createFile("SecondClassNameTest.m"));

        sensor.execute(context);

        assertEquals(Integer.valueOf(2), getMeasure("projectKey:FirstClassNameTest.m", CoreMetrics.TESTS_KEY));
        assertEquals(Integer.valueOf(0), getMeasure("projectKey:FirstClassNameTest.m", CoreMetrics.TEST_FAILURES_KEY));
        assertEquals(Long.valueOf(4), getMeasure("projectKey:FirstClassNameTest.m", CoreMetrics.TEST_EXECUTION_TIME_KEY));
        assertEquals(Integer.valueOf(2), getMeasure("projectKey:SecondClassNameTest.m", CoreMetrics.TESTS_KEY));
        assertEquals(Integer.valueOf(0), getMeasure("projectKey:SecondClassNameTest.m", CoreMetrics.TEST_FAILURES_KEY));
        assertEquals(Long.valueOf(2), getMeasure("projectKey:SecondClassNameTest.m", CoreMetrics.TEST_EXECUTION_TIME_KEY));
    }

    @Test
    public void execute_withoutReports() {
        sensor.execute(context);
    }
}
