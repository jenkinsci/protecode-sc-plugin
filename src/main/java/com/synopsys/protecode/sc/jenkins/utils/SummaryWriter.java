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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synopsys.protecode.sc.jenkins.types.HttpTypes;
import com.synopsys.protecode.sc.jenkins.types.InternalTypes.FileAndResult;
import hudson.FilePath;
import java.io.*;
import java.util.List;

public class SummaryWriter {
  
  public static void createXmlReport(List<FileAndResult> results, final ObjectMapper mapper,
    String xmlFile) throws IOException, InterruptedException {
    
    OutputStream outFile = new BufferedOutputStream(new FileOutputStream(xmlFile));
    
    try (PrintStream out = new PrintStream(outFile, false, "UTF-8")) {
      out.println(
        "<section name=\"Protecode SC analysis result\" fontcolor=\"#000000\">");
      for (FileAndResult result : results) {
        
        Long exact = result.getResultResponse().getResults().getSummary().getVulnCount()
            .getExact();
        String verdict = result.getResultResponse().getResults().getSummary().getVerdict()
          .getShortDesc();
        String verdict_detailed = result.getResultResponse().getResults().getSummary().getVerdict()
          .getDetailed();

        out.println("<accordion name =\"" + result.getFilename() + " (" + verdict + ")\">");

        Color color = exact > 0L ? Color.RED : Color.GREEN;

        writeField(out, "Verdict", verdict_detailed, color);
        writeField(out, "Vulnerabilities", exact.toString(), Color.BLACK);
        writeField(out, "Report", "", Color.BLACK,
          "<a target=\"_blank\" href=\""
            + result.getResultResponse().getResults().getReport_url()
            + "\">View full report in Protecode SC </a>");
        out.println("</accordion>");
      }
      out.println("</section>");
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
}
