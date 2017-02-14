/*******************************************************************************
* Copyright (c) 2016 Synopsys, Inc
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Synopsys, Inc - initial implementation and documentation
*******************************************************************************/

package com.synopsys.protecode.sc.jenkins;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.synopsys.protecode.sc.jenkins.exceptions.ApiAuthenticationException;
import com.synopsys.protecode.sc.jenkins.exceptions.ApiException;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import com.synopsys.protecode.sc.jenkins.HttpApiConnector.PollResult;

import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.Run;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ProtecodeScIntegrator extends Notifier {

    private static class ApiPoller {
        private String id;
        private boolean scanned;
        private HttpApiConnector connector;
        private PollResult result;

        ApiPoller(HttpApiConnector connector, String id) {
            this.connector = connector;

            this.id = id;
        }

        boolean isScanned() {
            return scanned;
        }

        PollResult poll() {

            result = connector.poll(id);
            if (result.isReady()) {
                scanned = true;
            }
            return result;
        }

        public PollResult getResult() {
            return result;
        }

        HttpApiConnector getConnector() {
            return connector;
        }

    }

    private String protecodeScGroup;
    private String credentialsId;
    private String artifactDir;
    private boolean convertToSummary = true;
    private boolean failIfVulns;
    private boolean leaveArtifacts;
    private int scanTimeout;

    @DataBoundConstructor
    public ProtecodeScIntegrator(String credentialsId, String protecodeScGroup,
            boolean failIfVulns, String artifactDir, boolean convertToSummary,
            boolean leaveArtifacts, int scanTimeout) {
        this.credentialsId = credentialsId;
        this.protecodeScGroup = protecodeScGroup;
        this.artifactDir = artifactDir;
        this.convertToSummary = convertToSummary;
        this.failIfVulns = failIfVulns;
        this.leaveArtifacts = leaveArtifacts;
        this.scanTimeout = scanTimeout > 10 ? scanTimeout : 10;
    }

    public String getProtecodeScGroup() {
        return protecodeScGroup;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getArtifactDir() {
        return artifactDir;
    }

    public int getScanTimeout() { return scanTimeout; }

    public boolean isConvertToSummary() {
        return convertToSummary;
    }

    public boolean isFailIfVulns() {
        return failIfVulns;
    }

    public boolean isLeaveArtifacts() {
        return leaveArtifacts;
    }


    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        if (getDescriptor().getProtecodeScHost() == null) {
            listener.error(
                    "Protecode SC host not defined. Configure it to global plugin properties");
            return false;
        }

        return true;
    }

    @SuppressWarnings("serial")
    private static final class FileReader implements FileCallable<File> {

        @Override
        public void checkRoles(RoleChecker arg0) throws SecurityException {
          // intentionally left empty
        }

        @Override
        public File invoke(File f, VirtualChannel channel)
                throws IOException, InterruptedException {
            return f;
        }

    }

    @SuppressWarnings("serial")
    private static class ScanFileFilter implements FileFilter, Serializable {

        @Override
        public boolean accept(File pathname) {
            return pathname.isFile();

        }
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private List<File> getArtifacts(AbstractBuild<?, ?> build,
            BuildListener listener) throws IOException, InterruptedException {
        PrintStream log = listener.getLogger();
        List<File> artifacts = new ArrayList<>();
        if (!StringUtils.isEmpty(artifactDir)) {
            List<FilePath> files = build.getWorkspace().child(artifactDir)
                    .list(new ScanFileFilter());
            if (files != null) {
                for (FilePath file : files) {
                    artifacts.add(file.act(new FileReader()));
                    log.println("Adding file " + file.getName()
                            + " for Protecode SC scan");
                }
            } else {
                log.println(String.format("Could not get additional artifacts from %s", artifactDir));
            }
        }

        List<? extends Run<?, ?>.Artifact> buildArtifacts = build
                .getArtifacts();
        for (Run<?, ?>.Artifact buildArtifact : buildArtifacts) {
            artifacts.add(buildArtifact.getFile());
        }

        return artifacts;
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        PrintStream log = listener.getLogger();
        log.println("Getting Protecode SC host and credentials");
        DescriptorImpl desc = getDescriptor();
        String host = desc.getProtecodeScHost();

        StandardUsernamePasswordCredentials creds = CredentialsMatchers
                .firstOrNull(
                        CredentialsProvider.lookupCredentials(
                                StandardUsernamePasswordCredentials.class,
                                Jenkins.getInstance(), ACL.SYSTEM,
                                new HostnameRequirement(host)),
                        CredentialsMatchers.withId(credentialsId));
        if (creds == null) {
            log.println("No Protecode SC credentials found");
            return false;
        }
        String protecodeScUser = creds.getUsername();
        String protecodeScPass = creds.getPassword().getPlainText();

        log.println("Connecting to Protecode SC host at " + host + " as "
                + protecodeScUser);
        boolean dontCheckCert = desc.isDontCheckCert();
        final List<File> artifacts = getArtifacts(build, listener);
        List<ApiPoller> identifiers = new ArrayList<>();
        for (File artifact : artifacts) {
            final File file = artifact;
            log.println("Scanning artifact " + file.getAbsolutePath());
            Artifact a = new Artifact(file);
            HttpApiConnector connector = new HttpApiConnector(
                    listener.getLogger(), a, host, protecodeScGroup,
                    protecodeScUser, protecodeScPass, dontCheckCert);
            Map<String, String> scanMetadata = ImmutableMap.of("build-id",
                    "" + build.getNumber(), "build-url",
                    build.getAbsoluteUrl());
            try {
                connector.init();
                String protecodeScIdentifier = connector.sendFile(a, scanMetadata);
                identifiers
                        .add(new ApiPoller(connector, protecodeScIdentifier));
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                throw new IOException(e);
            } catch (ApiAuthenticationException e) {
                log.println(e.getMessage());
                log.println("Failed to scan artifact");
                return false;
            } catch (ApiException e) {
                log.println(e.getMessage());
                return false;
            }
        }
        if (identifiers.isEmpty()) {
            log.println("No artifacts to scan!!");
            return false;
        }

        long stop = System.currentTimeMillis() + 1000L * 60 * scanTimeout;
        boolean poll = true;
        log.println("Waiting for scans to complete");
        while (poll) {
            boolean resultsLeft = false;
            for (ApiPoller poller : identifiers) {
                if (!poller.isScanned()) {

                    PollResult p = null;

                    p = poller.poll();

                    if (p != null && !p.isReady()) {
                        resultsLeft = true;
                    }
                }
            }
            if (System.currentTimeMillis() > stop || !resultsLeft) {
                poll = false;
            }
            if (poll) {
                Thread.sleep(10 * 1000);
            }
        }
        File jsonReportDirectory = new File(build.getWorkspace().getRemote(), "reports");
        boolean reportDirCreated = false;
        if (jsonReportDirectory.isDirectory() || jsonReportDirectory.mkdirs()) {
            reportDirCreated = true;
        }
        if (!reportDirCreated) {
            log.println("Report directory could not be created.");
            return false;
        }
        ObjectMapper mapper = getObjectMapper();
        for (ApiPoller poller : identifiers) {
            writeJson(log, mapper, jsonReportDirectory, poller.getResult());

        }
        boolean vulns = false;
        for (ApiPoller poller : identifiers) {
            PollResult r = poller.getResult();
            poller.getConnector().close();
            if (r == null || !r.isOk()) {
                if (r == null) {
                    log.println("No Protecode SC result available");
                }
                vulns = true;
            }
        }
        if (convertToSummary) {
            convertToSummary(log, build);
        }
        if (!leaveArtifacts && !StringUtils.isEmpty(artifactDir)) {
            build.getWorkspace().child(artifactDir).deleteContents();
        }
        return !(vulns && failIfVulns);
    }

    private static final String PROTECODE_FILE_TAG = "protecodesc";

    private static void convertToSummary(PrintStream log,
            AbstractBuild<?, ?> build) throws IOException {
        log.println("Creating xml for summary plugin");
        ObjectMapper mapper = getObjectMapper();
        try {
            File jsonReportDirectory = new File(build.getWorkspace().getRemote(), "reports");
            log.println(
                    "Reading json from " + jsonReportDirectory.getAbsolutePath());
            String[] jsonFiles = findJsonFiles(jsonReportDirectory);
            log.println(jsonFiles.length + " files found");

            File xmlReportDir = build.getArtifactsDir();
            if (!xmlReportDir.exists()) {
                boolean xmlReportDirCreated = xmlReportDir.mkdirs();
                if (!xmlReportDirCreated) {
                    log.println("XML report directory could not be created.");
                    throw new IOException("XML report directory could not be created.");
                }
            }
            File xmlFile = new File(xmlReportDir, PROTECODE_FILE_TAG + ".xml");

            log.println("Creating xml report to " + xmlFile.getName());

            OutputStream out = new BufferedOutputStream(
                    new FileOutputStream(xmlFile));
            createXmlReport(jsonReportDirectory, jsonFiles, mapper, out);
            out.close();
        } catch (NullPointerException e) {
            // NOP
        }
    }

    static void createXmlReport(final File jsonReportDirectory,
            final String[] jsonFiles, final ObjectMapper mapper,
            OutputStream xmlFile) throws IOException {

        PrintStream out = new PrintStream(xmlFile, false, "UTF-8");
        out.println(
                "<section name=\"Protecode SC analysis result\" fontcolor=\"#000000\">");
        for (String jsonFile : jsonFiles) {
            File f = new File(jsonReportDirectory, jsonFile);
            try (InputStream in = new BufferedInputStream(
                    new FileInputStream(f))) {
                ProtecodeSc psc = mapper.readValue(in, ProtecodeSc.class);
                Long exact = psc.getResults().getSummary().getVulnCount()
                        .getExact();
                String verdict = psc.getResults().getSummary().getVerdict()
                        .getShortDesc();
                String verdict_detailed = psc.getResults().getSummary().getVerdict()
                        .getDetailed();
                out.println("<accordion name =\"" + psc.getArtifactName()
                        + " (" + verdict + ")\">");

                Color color = exact > 0L ? Color.RED : Color.GREEN;
                writeField(out, "Verdict", verdict_detailed, color);
                writeField(out, "Vulnerabilities", exact.toString(), Color.BLACK);
                writeField(out, "Report", "", Color.BLACK,
                                "<a target=\"_blank\" href=\""
                                + psc.getResults().getReport_url()
                                + "\">View full report in Protecode SC </a>");
                out.println("</accordion>");

            }
        }
        out.println("</section>");
        out.close();
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

    private static String[] findJsonFiles(File targetDirectory) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(
                new String[] { "*-" + PROTECODE_FILE_TAG + ".json" });
        scanner.setBasedir(targetDirectory);
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    private static void writeJson(PrintStream log, ObjectMapper mapper,
            File workspaceJsonReportDirectory, PollResult result) {
        if (result == null || result.getProtecodeSc() == null) {
            log.println("No scan result!!");
            return;
        }
        File file = new File(workspaceJsonReportDirectory,
                result.getArtifactName() + "-" + PROTECODE_FILE_TAG + ".json");

        try (OutputStream out = new BufferedOutputStream(
                new FileOutputStream(file))) {
            mapper.writeValue(out, result.getProtecodeSc());
        } catch (IOException e) {
            log.println(e.toString());
        }

    }

    @Override
    public DescriptorImpl getDescriptor() {
        return new DescriptorImpl();
    }

    // This indicates to Jenkins that this is an implementation of an extension
    // point.
    @Extension
    public static final class DescriptorImpl
            extends BuildStepDescriptor<Publisher> {

        private String protecodeScHost;
        private boolean dontCheckCert;

        public DescriptorImpl() {
            load();
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project
            // types
            return true;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            protecodeScHost = formData.getString("protecodeScHost");
            dontCheckCert = formData.getBoolean("dontCheckCert");

            save();
            return super.configure(req, formData);
        }

        public ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath Item context) {

            StandardListBoxModel result = new StandardListBoxModel();
            result.withEmptySelection();
            result.withMatching(
                    CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(
                            StandardUsernamePasswordCredentials.class)),
                    CredentialsProvider.lookupCredentials(
                            StandardUsernamePasswordCredentials.class, context,
                            ACL.SYSTEM,
                            new HostnameRequirement(protecodeScHost)));
            return result;
        }

        @Override
        public String getDisplayName() {
            return "Protecode SC";
        }

        public String getProtecodeScHost() {
            return protecodeScHost;
        }

        public boolean isDontCheckCert() {
            return dontCheckCert;
        }

    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

}
