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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

final class ReportPersistor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportPersistor.class);

    private final SensorContext context;
    private final FileSystem fileSystem;

    private ReportPersistor(@Nonnull SensorContext context) {
        this.context = context;
        fileSystem = context.fileSystem();
    }

    @Nonnull
    static ReportPersistor create(@Nonnull SensorContext sensorContext) {
        return new ReportPersistor(sensorContext);
    }

    void saveReports(@Nonnull List<TestReport> testReports) {
        for (TestReport testReport : testReports) {
            for (TestSuite testSuite : testReport.getTestSuites()) {
                Optional<InputFile> value = buildInputFile(testSuite.getClassName());
                if (value.isPresent()) {
                    getInputFile(value.get(), testSuite);
                    continue;
                }

                LOGGER.warn("No path for {}", testSuite.getClassName());
            }
        }
    }

    @Nonnull
    private Optional<InputFile> buildInputFile(@Nonnull String className) {
        String filename = buildFilename(className);

        FilePredicate predicate = fileSystem.predicates().matchesPathPattern("**/" + filename);
        InputFile inputFile = fileSystem.inputFile(predicate);
        return Optional.ofNullable(inputFile);
    }

    @Nonnull
    private String buildFilename(@Nonnull String className) {
        className = replaceCategorySeparator(className);
        return appendFileExtension(className);
    }

    @Nonnull
    private String replaceCategorySeparator(@Nonnull String className) {
        return className.replace("_", "+");
    }

    @Nonnull
    private String appendFileExtension(@Nonnull String className) {
        return className + ".m";
    }

    private void getInputFile(@Nonnull InputFile inputFile, @Nonnull TestSuite testSuite) {
        saveMeasure(inputFile, CoreMetrics.TESTS, testSuite.getNumberOfTests());
        saveMeasure(inputFile, CoreMetrics.TEST_FAILURES, testSuite.getNumberOfFailedTests());
        saveMeasure(inputFile, CoreMetrics.TEST_EXECUTION_TIME, testSuite.getDurationInMilliseconds());
    }

    private void saveMeasure(@Nonnull InputFile inputFile, @Nonnull Metric metric, Serializable value) {
        //noinspection unchecked
        context.newMeasure()
                .forMetric(metric)
                .on(inputFile)
                .withValue(value)
                .save();
    }
}
