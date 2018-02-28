/** *****************************************************************************
 * Copyright (c) 2017 Synopsys, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Synopsys, Inc - initial implementation and documentation
 ****************************************************************************** */
package com.synopsys.protecode.sc.jenkins.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.synopsys.protecode.sc.jenkins.types.InternalTypes.FileAndResult;
import com.synopsys.protecode.sc.jenkins.types.InternalTypes.SerializableResult;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.*;
import java.util.List;

/**
 * Provides static methods for reporting and summaries.
 */
public class ReportBuilder {

  // TODO: This isn't a very nice way of identifying files.
  private static final String PROTECODE_FILE_TAG = "protecodesc";

  /**
   * Writes a json report for scans of a single build
   * @param results results received from Protecode SC
   * @param listener for console printing
   * @param reportsDirectory where to write the reports
   * @return true if no errors happened
   */
  public static boolean report(
    List<FileAndResult> results,
    TaskListener listener,
    FilePath reportsDirectory
  ) {
    PrintStream log = listener.getLogger();
    ObjectMapper mapper = getObjectMapper();
    results.forEach((result) -> {
      try {
        writeJson(log, mapper, reportsDirectory, result.getSerializableResult());
      } catch (Exception e) {
        log.println("No results for: " + result.getFilename());
      }
    });

    return true;
  }

  /**
   * Used for writing a report for a single scan/file
   */
  private static void writeJson(PrintStream log, ObjectMapper mapper,
    FilePath directoryToWrite, SerializableResult result) {
    if (result == null) {
      log.println("No scan result for a file");
      return;
    }

    File jsonFile = new File(
      directoryToWrite.getRemote()
      + "/"
      + new File(result.getFilename()).getName()
      + "-"
      + PROTECODE_FILE_TAG
      + ".json"
    );

    try (OutputStream out = new FileOutputStream(jsonFile)) {
      mapper.writeValue(out, result);
    } catch (Exception e) {
      log.println(e.toString());
    }
  }

  /**
   * Writes an xml file read by Summary Plugin
   *
   * @param run Build run instance for getting the report files
   * @param listener for printing to console
   * @return true if no errors happened
   * @throws IOException File write/access problems
   * @throws InterruptedException Jenkins interrupt
   */
  public static boolean makeSummary(
    Run<?, ?> run,
    TaskListener listener
  ) throws IOException, InterruptedException {

    PrintStream log = listener.getLogger();
    ObjectMapper mapper = getObjectMapper();

    try {
      FilePath[] jsonFiles = UtilitiesFile.reportsDirectory(run).list("*-" + PROTECODE_FILE_TAG + ".json");
      File xmlReportDir = run.getArtifactsDir();

      if (!xmlReportDir.exists()) {
        boolean xmlReportDirCreated = xmlReportDir.mkdirs();
        if (!xmlReportDirCreated) {
          log.println("XML report directory could not be created.");
          throw new IOException("XML report directory could not be created.");
        }
      } else {
        log.println("cannot find log dir: " + xmlReportDir.getAbsolutePath());
      }
      File xmlFile = new File(xmlReportDir, PROTECODE_FILE_TAG + ".xml");

      log.println("Creating xml report to " + xmlFile.getName());

      try (OutputStream out = new BufferedOutputStream(
        new FileOutputStream(xmlFile))) {
        createXmlReport(jsonFiles, mapper, out);
      }
    } catch (NullPointerException e) {
      // NOP
    }
    return true;
  }

  /**
   * Method reads json files from reports folder and makes an xml for Summary Plugin
   *
   * @param jsonFiles array of FilePath objects representing found json
   * @param mapper Xml object mapper
   * @param writeToStream target to write to
   * @throws IOException thrown for file write/access problems
   * @throws InterruptedException Jenkins interrupts
   */
  private static void createXmlReport(final FilePath[] jsonFiles, final ObjectMapper mapper,
    OutputStream writeToStream) throws IOException, InterruptedException {
    // TODO: Evaluate exception handling. 

    try (PrintStream out = new PrintStream(writeToStream, false, "UTF-8")) {
      out.println(
        "<section name=\"Protecode SC analysis result\" fontcolor=\"#000000\">");
      for (FilePath jsonFile : jsonFiles) {
        try (InputStream in = new BufferedInputStream(jsonFile.read())) {
          SerializableResult readResult = mapper.readValue(in, SerializableResult.class);
          Long untriagedVulns = readResult.getUntriagedVulns();
          Long triagedVulns = readResult.getTriagedVulns();
          String verdict = readResult.getVerdict();
          String verdict_detailed = readResult.getDetails();
          String fileName = new File(readResult.getFilename()).getName();
          String title = !"".equals(verdict) ? fileName + " (" + verdict + ")" : fileName;

          out.println("<accordion name =\"" + title + "\">");

          Color color = untriagedVulns > 0L ? Color.RED : Color.GREEN;
          writeField(out, "Verdict", " " + verdict_detailed, color);
          writeField(out, "Untriaged vulnerabilities", " " + untriagedVulns.toString(), Color.BLACK);
          writeField(out, "Triaged vulnerabilities", " " + triagedVulns.toString(), Color.BLACK);
          writeField(out, "Report", "", Color.BLACK,
            "<a target=\"_blank\" href=\""
            + readResult.getReportUrl()
            + "\">View full report in Protecode SC </a>");
          out.println("</accordion>");
        }
      }
      out.println("</section>");
    }
  }

  /**
   * The colours for reports in Jenkins UI
   */
  private static enum Color {
    RED("#ff0000"), GREEN("#00ff00"), YELLOW("#ff9c00"), BLACK("#000000");

    private String value;

    private Color(String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }
  }

  private static void writeField(PrintStream out, String name, String value,
    Color valueColor) {
    writeField(out, name, value, valueColor, null);
  }

  private static void writeField(PrintStream out, String name, String value,
    Color valueColor, String cdata) {
    // TODO: Add a space between the name and value
    out.append("<field name=\"" + name + "\" titlecolor=\"black\" value=\""
      + value + "\" ");
    out.append("detailcolor=\"" + valueColor.getValue() + "\">\n");
    if (cdata != null && !cdata.isEmpty()) {
      out.print("<![CDATA[");
      out.print(cdata);
      out.print("]]>");
    }
    out.append("</field>\n");
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    return mapper;
  }
}
