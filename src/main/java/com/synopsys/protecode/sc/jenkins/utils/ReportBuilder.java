 /*******************************************************************************
  * Copyright (c) 2017 Synopsys, Inc
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Synopsys, Inc - initial implementation and documentation
  *******************************************************************************/
package com.synopsys.protecode.sc.jenkins.utils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes.Results;
import com.synopsys.protecode.sc.jenkins.types.InternalTypes.FileAndResult;
import com.synopsys.protecode.sc.jenkins.types.InternalTypes.SerializableResult;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.*;
import java.util.List;


public class ReportBuilder {
  
  private static final String PROTECODE_FILE_TAG = "protecodesc";
  
  public static boolean report(
    List<FileAndResult> results,
    TaskListener listener,
    String reportDirectory,
    FilePath workspace
  ) {
    
    if (!UtilitiesFile.makeDirectory(reportDirectory, workspace, listener)) {
      listener.error("Couldn't create report directory.");
      return false;
    }
    FilePath jsonReportDirectory = workspace.child(reportDirectory);
    
    PrintStream log = listener.getLogger();
    ObjectMapper mapper = getObjectMapper();
    results.forEach((result) -> {
      try {
        writeJson(log, mapper, jsonReportDirectory, result.getSerializableResult());
      } catch (Exception e) {
        log.println("No results for: " + result.getFilename());
      }
    });
    
    return true;
  }
  
  public static boolean makeSummary(
    List<FileAndResult> results,
    Run<?, ?> run,
    TaskListener listener,
    String reportDirectory,
    FilePath workspace
  ) throws IOException, InterruptedException {
    @SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    FilePath jsonReportDirectory = workspace.child(reportDirectory);
    
    PrintStream log = listener.getLogger();
    
    log.println("Creating xml for summary plugin");
    ObjectMapper mapper = getObjectMapper();
    try {
      FilePath[] jsonFiles = jsonReportDirectory.list("*-" + PROTECODE_FILE_TAG + ".json");
      log.println(jsonFiles.length + " report files found");
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
  
  private static void createXmlReport(final FilePath[] jsonFiles, final ObjectMapper mapper,
    OutputStream xmlFile) throws IOException, InterruptedException {
    
    try (PrintStream out = new PrintStream(xmlFile, false, "UTF-8")) {
      out.println(
        "<section name=\"Protecode SC analysis result\" fontcolor=\"#000000\">");
      for (FilePath jsonFile : jsonFiles) {
        try (InputStream in = new BufferedInputStream(jsonFile.read())) {
          Results readResult = mapper.readValue(in, Results.class);
          Long exact = readResult.getSummary().getVulnCount()
            .getExact();
          String verdict = readResult.getSummary().getVerdict()
            .getShortDesc();
          String verdict_detailed = readResult.getSummary().getVerdict()
            .getDetailed();
          
          // TODO: Provide name more nicely
          String fileName = jsonFile.getName().substring(
            0,
            jsonFile.getName().indexOf(PROTECODE_FILE_TAG) - 1
          );
          out.println("<accordion name =\"" + fileName + " (" + verdict + ")\">");
          
          Color color = exact > 0L ? Color.RED : Color.GREEN;
          writeField(out, "Verdict", verdict_detailed, color);
          writeField(out, "Vulnerabilities", exact.toString(), Color.BLACK);
          writeField(out, "Report", "", Color.BLACK,
            "<a target=\"_blank\" href=\""
              + readResult.getReport_url()
              + "\">View full report in Protecode SC </a>");
          out.println("</accordion>");
        }
      }
      out.println("</section>");
    }
  }
  
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
  
  private static void writeJson(PrintStream log, ObjectMapper mapper,
    FilePath workspaceJsonReportDirectory, SerializableResult result) {
    if (result.getResults() == null) {
      log.println("No scan result for file: " + result.getFilename());
      return;
    }
    FilePath jsonFile = workspaceJsonReportDirectory.child(
      result.getFilename()+ "-" + PROTECODE_FILE_TAG + ".json"
    );
    
    try (OutputStream out = new BufferedOutputStream(jsonFile.write())) {
      mapper.writeValue(out, result.getResults());
    } catch (IOException | InterruptedException e) {
      log.println(e.toString());
    }
  }
}
