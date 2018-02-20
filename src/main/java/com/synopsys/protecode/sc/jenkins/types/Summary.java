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

package com.synopsys.protecode.sc.jenkins.types;

import lombok.Data;

/*
<section name="Protecode SC analysis result" fontcolor="#000000">
<accordion name ="utorrent.exe (Vulns)">
<field name="Verdict" titlecolor="black" value="Known vulnerabilities were found during the scan!" detailcolor="#ff0000">
</field>
<field name="Vulnerabilities" titlecolor="black" value="10" detailcolor="#000000">
</field>
<field name="Report" titlecolor="black" value="" detailcolor="#000000">
<![CDATA[<a target="_blank" href="https://protecode-sc.com/products/22072908/">View full report in Protecode SC </a>]]></field>
</accordion>
</section>
 */
public @Data class Summary {
  private String filename;
  private String verdict;
  private int untriagedVulnerabilities;
  private int triagedVulnerabilities;
  private String reportUrl;

  
}
