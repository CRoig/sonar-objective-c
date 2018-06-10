/*
 * Copyright © 2012 OCTO Technology, Backelite (${email})
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.objectivec.core.ObjectiveC;
import org.sonar.squidbridge.rules.SqaleXmlLoader;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OCLintRulesDefinition implements RulesDefinition {
    static final String REPOSITORY_KEY = "OCLint";
    static final String REPOSITORY_NAME = REPOSITORY_KEY;

    private static final Logger LOGGER = LoggerFactory.getLogger(OCLintRulesDefinition.class);
    private static final String RULES_FILE = "/org/sonar/plugins/oclint/rules.txt";

    @Override
    public void define(@Nonnull Context context) {
        NewRepository repository = createRepository(context);

        try {
            loadRules(repository);
        } catch (IOException e) {
            LOGGER.error("Failed to load OCLint rules", e);
        }

        SqaleXmlLoader.load(repository, "/com/sonar/sqale/oclint-model.xml");

        repository.done();
    }

    @Nonnull
    NewRepository createRepository(@Nonnull Context context) {
        return context.createRepository(REPOSITORY_KEY, ObjectiveC.KEY)
                .setName(REPOSITORY_NAME);
    }

    private void loadRules(NewRepository repository) throws IOException {
        Reader reader = new BufferedReader(new InputStreamReader(getClass()
                .getResourceAsStream(RULES_FILE), CharEncoding.UTF_8));

        final List<String> listLines = IOUtils.readLines(reader);

        populateRepositoryWithRulesFromLines(repository, listLines);
    }

    void populateRepositoryWithRulesFromLines(@Nonnull NewRepository repository, @Nonnull List<String> listLines) {
        String previousLine = null;
        Map<String, String> rule = new HashMap<>();
        boolean inDescription = false;
        for (String line : listLines) {
            if (isLineIgnored(line)) {
                inDescription = false;
            } else if (isLineSeparator(line)) {
                LOGGER.debug("Rule found : {}", previousLine);

                // Remove the rule name from the description of the previous
                // rule
                if (rule.get("description") != null) {
                    String description = rule.get("description");
                    final int index = description.lastIndexOf(previousLine);
                    if (index > 0) {
                        rule.put("description", description.substring(0, index));
                    }

                }

                rule.clear();

                rule.put("name", StringUtils.capitalize(previousLine));
                rule.put("key", previousLine);


            } else if (isSummary(line)) {
                inDescription = true;
                rule.put("description", line.substring(line.indexOf(':') + 1));
            } else if (isCategory(line)) {
                inDescription = true;

                // Create rule when last filed found
                RulesDefinition.NewRule newRule = repository.createRule(rule.get("key"));
                newRule.setName(rule.get("name"));
                newRule.setSeverity(rule.get("severity"));
                newRule.setHtmlDescription(rule.get("description"));

            } else if (isSeverity(line)) {
                inDescription = false;
                final String severity = line.substring("Severity: ".length());
                rule.put("severity", OCLintRuleSeverity.valueOfInt(Integer.valueOf(severity)).name());
            } else {
                if (inDescription) {
                    line = ruleDescriptionLink(line);
                    String description = rule.get("description");
                    rule.put("description", description + "<br>" + line);
                }
            }

            previousLine = line;
        }
    }

    private boolean isLineIgnored(String line) {
        return line.matches("\\=.*") || line.matches("Priority:.*");
    }

    private boolean isLineSeparator(String line) {
        return line.matches("[\\-]{4,}.*");
    }

    private boolean isSummary(String line) {
        return line.matches("Summary:.*");
    }

    private boolean isCategory(String line) {
        return line.matches("Category:.*");
    }

    private boolean isSeverity(String line) {
        return line.matches("Severity:.*");
    }

    private String ruleDescriptionLink(final String line) {
        String result = line;
        final int indexOfLink = line.indexOf("http://");
        if (0 <= indexOfLink) {
            final String link = line.substring(indexOfLink);
            final StringBuilder htmlText = new StringBuilder("<a href=\"");
            htmlText.append(link);
            htmlText.append("\" target=\"_blank\">");
            htmlText.append(link);
            htmlText.append("</a>");
            result = htmlText.toString();
        }
        return result;
    }
}
