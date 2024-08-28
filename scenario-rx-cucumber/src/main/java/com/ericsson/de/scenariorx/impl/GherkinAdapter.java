package com.ericsson.de.scenariorx.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.nullToEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ericsson.de.scenariorx.api.RxBasicDataRecord;
import com.ericsson.de.scenariorx.api.RxDataRecord;
import com.ericsson.de.scenariorx.api.RxScenario;
import com.ericsson.de.scenariorx.api.RxTestStep;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import gherkin.ast.DataTable;
import gherkin.ast.DocString;
import gherkin.ast.Examples;
import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.ScenarioOutline;
import gherkin.ast.Step;
import gherkin.ast.TableCell;
import gherkin.ast.TableRow;
import gherkin.formatter.model.Comment;
import gherkin.formatter.model.DataTableRow;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.Tag;


public class GherkinAdapter {
    public static final List<Comment> NO_COMMENTS = Lists.newArrayList();

    public static gherkin.formatter.model.Step adapt(Step gherkinStep) {
        List<DataTableRow> rows = null;
        gherkin.formatter.model.DocString docString = null;

        if (gherkinStep.getArgument() instanceof DataTable) {
            rows = adapt(DataTable.class.cast(gherkinStep.getArgument()));
        } else if (gherkinStep.getArgument() instanceof DocString) {
            DocString gherkinDocString = DocString.class.cast(gherkinStep.getArgument());
            docString = new gherkin.formatter.model.DocString(
                    gherkinDocString.getContentType(),
                    gherkinDocString.getContent(),
                    gherkinDocString.getLocation().getLine()
            );
        }

        return new gherkin.formatter.model.Step(
                NO_COMMENTS,
                gherkinStep.getKeyword(),
                gherkinStep.getText(),
                gherkinStep.getLocation().getLine(),
                rows,
                docString
        );
    }

    static gherkin.formatter.model.Step adapt(Step gherkinStep, RxDataRecord dataRecord) {
        String name = gherkinStep.getText();
        for (String param : outlineParams(name)) {
            Object fieldValue = dataRecord.getFieldValue(param);
            checkNotNull(fieldValue, "Unable to find Parameter " + fieldValue);
            name = name.replace("<"+param+">", fieldValue.toString());
        }

        return new gherkin.formatter.model.Step(
                NO_COMMENTS,
                gherkinStep.getKeyword(),
                name,
                gherkinStep.getLocation().getLine(),
                null,
                null
        );
    }

    static ArrayList<String> outlineParams(String text) {
        ArrayList<String> params = Lists.newArrayList();
        Matcher matcher = Pattern.compile("<([^<]*)>").matcher(text);
        while (matcher.find()) {
            params.add(matcher.group(1));
        }

        return params;
    }

    private static List<DataTableRow> adapt(DataTable cast) {
        List<DataTableRow> result = Lists.newArrayList();

        for (TableRow tableRow : cast.getRows()) {
            ArrayList<String> cells = Lists.newArrayList();

            for (TableCell tableCell : tableRow.getCells()) {
                cells.add(tableCell.getValue());
            }

            result.add(new DataTableRow(
                    NO_COMMENTS,
                    cells,
                    cast.getLocation().getLine()
            ));

        }

        return result;
    }

    public static gherkin.formatter.model.Step step(String name) {
        return new gherkin.formatter.model.Step(
                NO_COMMENTS,
                "",
                name,
                0,
                Lists.<DataTableRow>newArrayList(),
                null
        );
    }

    public static gherkin.formatter.model.Feature adapt(Feature feature) {
        return new gherkin.formatter.model.Feature(
                NO_COMMENTS,
                adapt(feature.getTags()),
                "",
                feature.getName(),
                nullToEmpty(feature.getDescription()),
                0,
                ""
        );
    }

    public static gherkin.formatter.model.Scenario adapt(RxScenario scenario) {
        return new Scenario(
                NO_COMMENTS,
                Lists.<Tag>newArrayList(),
                "",
                scenario.getName(),
                "",
                0,
                ""
        );
    }

    public static gherkin.formatter.model.Scenario adapt(ScenarioDefinition scenario) {
        return new Scenario(
                NO_COMMENTS,
                getTags(scenario),
                "",
                scenario.getName(),
                "",
                0,
                ""
        );
    }


    public static String adapt(RxTestStep.Status status) {
        if (RxTestStep.Status.SUCCESS.equals(status)) {
            return Result.PASSED;
        } else if (RxTestStep.Status.FAILED.equals(status)) {
            return Result.FAILED;
        } else if (RxTestStep.Status.SKIPPED.equals(status)) {
            return "skipped";
        }

        throw new IllegalArgumentException();
    }

    public static ArrayList<RxDataRecord> toDataSource(List<Examples> examples) {
        Preconditions.checkArgument(examples.size() == 1, "Only one Example is supported");
        ArrayList<RxDataRecord> dataRecords = Lists.newArrayList();
        Examples example = examples.get(0);
        Preconditions.checkArgument(example.getTableBody().size() > 0, "Empty Example!");
        for (TableRow tableRow : example.getTableBody()) {
            RxBasicDataRecord.DataRecordBuilder builder = RxBasicDataRecord.builder();

            List<TableCell> header = example.getTableHeader().getCells();
            List<TableCell> cells = tableRow.getCells();
            checkArgument(header.size() == cells.size());
            for (int i = 0; i < header.size(); i++) {
                builder.setField(header.get(i).getValue(), cells.get(i).getValue());
            }

            dataRecords.add(builder.build());
        }

        return dataRecords;
    }

    public static List<Tag> getTags(ScenarioDefinition scenarioDefinition) {
        List<gherkin.ast.Tag> tags = Lists.newArrayList();

        if (scenarioDefinition instanceof ScenarioOutline) {
            tags.addAll(ScenarioOutline.class.cast(scenarioDefinition).getTags());
        } else if (scenarioDefinition instanceof gherkin.ast.Scenario) {
            tags.addAll(gherkin.ast.Scenario.class.cast(scenarioDefinition).getTags());
        }

        return adapt(tags);
    }

    private static List<Tag> adapt(Collection<gherkin.ast.Tag> tags) {
        List<Tag> result = Lists.newArrayList();
        for (gherkin.ast.Tag tag : tags) {
            result.add(
                    new Tag(tag.getName(), tag.getLocation().getLine())
            );
        }

        return result;
    }

    static Set<Tag> tagsAndInheritedTags(Feature feature, ScenarioDefinition scenario) {
        Set<Tag> tags = new HashSet<Tag>();
        tags.addAll(adapt(feature.getTags()));
        tags.addAll(getTags(scenario));
        return tags;
    }
}
