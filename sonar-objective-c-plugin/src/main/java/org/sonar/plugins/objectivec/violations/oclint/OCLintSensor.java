/*
 * Copyright © 2012 OCTO Technology, Backelite (${email})
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
package org.sonar.plugins.objectivec.violations.oclint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Settings;
import org.sonar.plugins.objectivec.ObjectiveCPlugin;
import org.sonar.plugins.objectivec.core.ObjectiveC;

import javax.annotation.Nonnull;

public final class OCLintSensor implements Sensor {
    public static final String REPORT_PATH_KEY = ObjectiveCPlugin.PROPERTY_PREFIX + ".oclint.report";
    public static final String DEFAULT_REPORT_PATH = "sonar-reports/*oclint.xml";

    private static final Logger LOGGER = LoggerFactory.getLogger(OCLintSensor.class);
    private static final String NAME = "OCLint violation sensor";

    private final ReportParser parser = new ReportParser();
    private final Settings conf;
    private final FileSystem fileSystem;

    @SuppressWarnings("WeakerAccess")
    public OCLintSensor(final FileSystem fileSystem, final Settings config) {
        this.conf = config;
        this.fileSystem = fileSystem;
    }

    @Override
    public void describe(@Nonnull SensorDescriptor descriptor) {
        descriptor.name(NAME);
        descriptor.onlyOnLanguage(ObjectiveC.KEY);
    }

    @Override
    public void execute(@Nonnull org.sonar.api.batch.sensor.SensorContext context) {
        final String projectBaseDir = fileSystem.baseDir().getPath();

        parseReportIn(projectBaseDir, ViolationPersistor.create(context));
    }

    private void parseReportIn(final String baseDir, ViolationPersistor persistor) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(new String[]{buildReportPath()});
        scanner.setBasedir(baseDir);
        scanner.setCaseSensitive(false);
        scanner.scan();
        String[] files = scanner.getIncludedFiles();

        List<Violation> violations = new ArrayList<>();

        for(String filename : files) {
            LOGGER.info("Processing OCLint report {}", filename);

            violations.addAll(parser.parse(new File(filename)));
        }

        persistor.saveViolations(violations);
    }

    private String buildReportPath() {
        String reportPath = conf.getString(REPORT_PATH_KEY);

        if (reportPath == null) {
            LOGGER.debug("No value specified for \"" + REPORT_PATH_KEY + "\" using default path");
            reportPath = DEFAULT_REPORT_PATH;
        }

        return reportPath;
    }
}
