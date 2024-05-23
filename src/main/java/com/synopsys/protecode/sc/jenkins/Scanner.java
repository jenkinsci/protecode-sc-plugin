/** *****************************************************************************
 * Copyright (c) 2018 Synopsys, Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Synopsys, Inc - initial implementation and documentation
 ****************************************************************************** */
package com.synopsys.protecode.sc.jenkins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.synopsys.protecode.sc.jenkins.Scanner;
import com.synopsys.protecode.sc.jenkins.exceptions.ScanException;
import com.synopsys.protecode.sc.jenkins.interfaces.Listeners;
import com.synopsys.protecode.sc.jenkins.interfaces.Listeners.ScanService;
import com.synopsys.protecode.sc.jenkins.types.*;
import com.synopsys.protecode.sc.jenkins.utils.JenkinsConsoler;
import com.synopsys.protecode.sc.jenkins.utils.UtilitiesFile;
import static com.synopsys.protecode.sc.jenkins.utils.UtilitiesFile.ZIP_FILE_PREFIX;
import com.synopsys.protecode.sc.jenkins.utils.UtilitiesGeneral;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.MediaType;

/**
 * The main logic class for operating with BDBA
 */
public class Scanner {

  // Used in the scan process
  private List<FileResult> results = new ArrayList<>();

  private final BuildVerdict verdict;
  private String protecodeScGroup;
  private final JenkinsConsoler console;
  private final TaskListener listener;
  private final ProtecodeScService service;
  private final Run<?, ?> run;
  private final int scanTimeout;
  private final FilePath workspace;
  private String directoryToScan;
  private final boolean scanOnlyArtifacts;
  private final boolean includeSubdirectories;
  private final boolean endAfterSendingFiles;
  private final String pattern;
  private final String protecodeScanName;
  private final String customHeader;
  private final boolean dontZipFiles;
  private final boolean failIfVulns;

  private static final String NO_ERROR = "";

  private boolean zippingInUse = false;
  private static final Logger LOGGER = Logger.getLogger(Scanner.class.getName());

  public Scanner(
    BuildVerdict verdict,
    String protecodeScGroup,
    ProtecodeScService serv,
    Run<?, ?> run,
    int scanTimeout,
    FilePath workspace,
    TaskListener listener,
    String directoryToScan,
    boolean scanOnlyArtifacts,
    boolean includeSubdirectories,
    boolean endAfterSendingFiles,
    String pattern,
    String protecodeScanName,
    String customHeader,
    boolean dontZipFiles,
    boolean failIfVulns
  ) {
    this.verdict = verdict;
    this.protecodeScGroup = protecodeScGroup;
    this.console = JenkinsConsoler.getInstance();
    this.listener = listener;
    this.service = serv;
    this.run = run;
    this.scanTimeout = scanTimeout;
    this.workspace = workspace;
    this.directoryToScan = directoryToScan;
    this.endAfterSendingFiles = endAfterSendingFiles;
    this.scanOnlyArtifacts = scanOnlyArtifacts;
    this.includeSubdirectories = includeSubdirectories;
    this.pattern = pattern;
    this.protecodeScanName = protecodeScanName;
    this.customHeader = customHeader;
    this.dontZipFiles = dontZipFiles;
    this.failIfVulns = failIfVulns;
  }

  public void readEnvironmentVariables() throws IOException, InterruptedException {
    Map<String, String> envMap = run.getEnvironment(listener);
    try {
      String groupVarName = UtilitiesGeneral.parseEnvironmentVariable(protecodeScGroup);
      protecodeScGroup = envMap.getOrDefault(groupVarName, protecodeScGroup);
      console.log("Upload group value from env var: " + groupVarName + "=" + protecodeScGroup);
    } catch (ParseException e) {}

    try {
      String scanDirVarName = UtilitiesGeneral.parseEnvironmentVariable(directoryToScan);
      directoryToScan = envMap.getOrDefault(scanDirVarName, directoryToScan);
      console.log("Directory to scan value from env var: " + scanDirVarName + "=" + directoryToScan);
    } catch (ParseException e) {}
  }

  /**
   * The logic.
   *
   * Add a suppress since the executor workspace is already checked and valid.
   * SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
   *
   * @return List of FileResults
   * @throws IOException File operations will throw this in cases of not found etc
   * @throws InterruptedException Jenkins build interruption
   */
  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public Optional<List<FileResult>> doPerform() throws InterruptedException, IOException {
    readEnvironmentVariables();
    FilePath directory = workspace.child(directoryToScan);
    List<FilePath> files = new ArrayList<>(); // so not to cause npe if no files were fonud
    FilePath zip = null;
    long start = 0;

    if (!UtilitiesGeneral.isUrl(directoryToScan)) {
      if (scanOnlyArtifacts) {
        LOGGER.finer("Scanning only artifacts");
        files = UtilitiesFile.getArtifacts(
          run,
          UtilitiesFile.patternOrAll(pattern),
          directory
        );
      } else {
        LOGGER.finer("Scanning all in directory");
        //@SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
        files = UtilitiesFile.getFiles(
          directory,
          includeSubdirectories,
          UtilitiesFile.patternOrAll(pattern),
          run,
          listener
        );
      }
      verdict.setFilesFound(files.size());
      LOGGER.info("files found: " + files.size());
      console.log("Found " + files.size() + " files to send");

      if (files.isEmpty()) {
        return Optional.empty();
      }
      Optional<String> zipName = Optional.empty();
      if (files.size() > Configuration.MAXIMUM_UNZIPPED_FILE_AMOUNT
        && !dontZipFiles) {
        LOGGER.log(Level.INFO, "Files count: {0}, attempting to zip to executor workspace root", files.size());
        try {
          zipName = Optional.of(workspace + "/" + ZIP_FILE_PREFIX + protecodeScanName);
          zip = UtilitiesFile.packageFiles(
            workspace,
            files,
            zipName.get()
          );
          LOGGER.log(Level.INFO, "Zip size: {0} bits.", zip.length());
          zippingInUse = true;
          files.clear();
          files.add(zip);
        } catch (Exception e) {
          zippingInUse = false;
          console.log("Could not zip files, sending one-by-one.");
          LOGGER.log(Level.INFO, "Couldn't zip files, sending them one-by-one. Error: {0}", e.getMessage());
        }
      }

      // Send files and wait for all http responses
      start = System.currentTimeMillis();
      console.log("Upload began at " + UtilitiesGeneral.timestamp() + ".");
      if (zipName.isPresent()) {
        if (files.size() != 1) {
          // For some obscure cases where the zip could not be made, but no exception was raised.
          zippingInUse = false;
        }
      }
      sendFiles(files, zipName);

    } else {
      LOGGER.log(Level.WARNING, "Gettign from URL");
      console.log("Fetching file from URL: " + directoryToScan);
      ObjectReader reader = new ObjectMapper().readerFor(Map.class);
      start = System.currentTimeMillis();

      Map<String, String> map = new HashMap<>();
      if (customHeader.length() > 0)
      {
          map = reader.readValue(customHeader);
      }
      verdict.setFilesFound(1);
      service.scanFetchFromUrl(
        protecodeScGroup,
        directoryToScan,
        map,
        new ScanService() {
        @Override
        public void setError(String reason) {
          // TODO: use Optional
          console.error(reason);
          addUploadResponse(directoryToScan, null, reason);
        }

        @Override
        public void processUploadResult(HttpTypes.UploadResponse result) {
          addUploadResponse(directoryToScan, result, NO_ERROR);
        }
      }
      );
    }

    waitForUploadResponses(files.size());

    if (endAfterSendingFiles) {
      return Optional.empty();
    }

    console.log("Upload of files completed at " + UtilitiesGeneral.timestamp() + ".");
    long time = (System.currentTimeMillis() - start) / 1000;
    LOGGER.log(Level.INFO, "Uploading files to " + Configuration.TOOL_NAME + " took: {0} seconds", time);

    // start polling for reponses to scans
    poll(run);
    if (zip != null && zip.exists()) {
      LOGGER.info("removing zip file.");
      zip.delete();
    }
    return Optional.of(results);
  }

  public boolean zippingInUse() {
    return zippingInUse;
  }

  /**
   * Called by the lamdas given to upload rest calls
   *
   * @param response The responses fetched from Protecode SC
   */
  private void addUploadResponse(String name, HttpTypes.UploadResponse response, String error) {
    // TODO: compare the sha1sum and send again if incorrect
    if (NO_ERROR.equals(error)) {
      results.add(new FileResult(name, response, zippingInUse));
      console.logPure("Uploaded: " + name + ": " + "\n\t" + response.getResults().getReport_url());
    } else {
      // TODO, if en error which will stop the build from happening we should stop the build.
      results.add(new FileResult(name, error));
    }
  }

  private void sendFiles(List<FilePath> filesToScan, Optional<String> zipName) throws IOException, InterruptedException {
    // TODO: Use zipnaming correctly.
    for (FilePath file : filesToScan) {
      final String jobName;
      if (zippingInUse) {
        jobName = protecodeScanName;
      } else {
        jobName = file.getName();
      }

      LOGGER.log(Level.INFO, "Sending file: {0}", jobName);
      service.scan(this.protecodeScGroup,
        jobName,
        new StreamRequestBody(
          MediaType.parse("application/octet-stream"),
          file
        ),
        new Listeners.ScanService() {
        @Override
        public void processUploadResult(HttpTypes.UploadResponse result) {
          addUploadResponse(jobName, result, NO_ERROR);
        }

        @Override
        public void setError(String reason) {
          // TODO: use Optional
          if (reason.toLowerCase().contains("unexpected end of stream")) {
            LOGGER.log(Level.WARNING, "RECEIVED UNEXPECTED END OF STREAM: {0}", reason);
            console.error(Configuration.TOOL_NAME + " reported that the file did not arrive properly. Please check you network.\n"
              + "This is usually seen when the socket between Jenkins and the BDBA instance has connection\n"
              + "problems. One possibility to fix this is to make sure you don't use WLAN, the network\n"
              + "has enough bandwidth and is reliable.");
          } else {
            console.error("while sending files: " + reason);
          }
          // TODO: Maybe use listener.error to stop writing for more results if we get error
          // perhaps?
          addUploadResponse(jobName, null, reason);
        }
      }
      );
      Thread.sleep(500); // we don't want to overload anything
    }
  }

  /**
   * TODO clean up depth, move logic to other methods
   *
   * @param listener
   */
  private void poll(Run<?, ?> run) throws InterruptedException {
    if (results.stream().allMatch((fileAndResult) -> (fileAndResult.errorIsSet()))) {
      console.log("No results found. Perhaps no uploads were succesfull.");
      return;
    }
    // TODO: Make better timeout, which encapsulates the whole step
    long endAt = System.currentTimeMillis() + (this.scanTimeout * 60L * 1000L);
    // use shortened variable to distinguish from possibly null service
    console.log("Waiting for results from " + Configuration.TOOL_NAME);
    do {
      if (System.currentTimeMillis() > endAt) {
        String message = "Timeout while fetching files";
        listener.error(message);
        if (failIfVulns) {
          throw new ScanException(message);
        }
      }
      for (FileResult result : results) {
        if (!result.errorIsSet()
          && !result.hasScanResponse()
          && result.uploadHTTPStatus() == 200) {
          if ("R".equals(result.getState())) {
            if (!result.isResultBeingFetched()) {
              result.setResultBeingFetched(true);
              service.scanResult(result.getUploadResponse().getResults().getSha1sum(),
                new Listeners.ResultService() {
                @Override
                public void setScanResult(HttpTypes.ScanResultResponse scanResult) {
                  console.logPure("Received results for file: " + result.getFilename());
                  result.setResultResponse(scanResult);
                }

                @Override
                public void setError(String reason) {
                  console.logPure("Received " + Configuration.TOOL_NAME + " scan result ERROR for file: "
                    + result.getFilename());
                  result.setError(reason);
                }
              }
              );
            }
          } else {
            service.poll(// TODO: Use pretty annotation in type "product_id"
              result.getUploadResponse().getResults().getProduct_id(),
              new Listeners.PollService() {
              @Override
              public void setScanStatus(HttpTypes.UploadResponse status) {
                result.setUploadResponse(status);
              }

              @Override
              public void setError(String reason) {
                console.error("scan status ERROR: " + result.getFilename() + ": " + result.getState() + ": " + reason);
                result.setError(reason);
              }
            }
            );
          }
        }

        Thread.sleep(150); // we don't want to overload the network with bulk requests
      }
      if (allNotReady()) {
        Thread.sleep(10 * 1000);
      }
    } while (allNotReady());
    console.log("Received all results from " + Configuration.TOOL_NAME);
  }

  /**
   * Simple check to determine does every scan have a result or an error
   *
   * @return true if all results have been fetched
   */
  private boolean allNotReady() {
    return results.stream().anyMatch((fileResult) -> (!fileResult.hasScanResponse()));
  }

  /**
   * Waits until all upload results are in. Returns only then.
   *
   * @param fileCount How many files were uploaded
   * @param log for printing to Jenkins build console.
   */
  private void waitForUploadResponses(int fileCount) {
    boolean waitForResponses = true;
    // TODO: Add timeout since some files get no reponse from BDBA
    while (waitForResponses) {
      try {
        Thread.sleep(30 * 1000);
        if (results.size() >= fileCount) {
          waitForResponses = false;
        }
      } catch (InterruptedException ie) {
        waitForResponses = false;
        console.log("Interrupted while waiting for upload responses from " + Configuration.TOOL_NAME);
      }
    }
  }
}
