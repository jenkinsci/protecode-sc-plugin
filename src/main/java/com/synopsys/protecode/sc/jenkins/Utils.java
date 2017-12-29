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
package com.synopsys.protecode.sc.jenkins;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.File;
import java.io.FileFilter;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Utils {
    
    private Utils(){
        // don't instantiate me...
    }
    
    private static class ScanFileFilter implements FileFilter, Serializable {
        @Override
        public boolean accept(File pathname) {
            return pathname.isFile();
        }
    }
    
    public static List<ReadableFile> getFiles(String fileDirectory, FilePath workspace, Run<?, ?> run,
            TaskListener listener) throws IOException, InterruptedException {
        // add '/' to end if absent
        if (!fileDirectory.endsWith("/")) {
            fileDirectory = fileDirectory + "/";
        }
        if (!fileDirectory.startsWith("./")) {
            fileDirectory = "./" + fileDirectory;
        }
        PrintStream log = listener.getLogger();
        List<ReadableFile> readableFiles = new ArrayList<>();
        log.println("Reading from directory: " + fileDirectory);
        if (!StringUtils.isEmpty(fileDirectory)) {
            @SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
            List<FilePath> files = workspace.child(fileDirectory)
                    .list(new ScanFileFilter());
            if (!files.isEmpty()) {
                for (FilePath file : files) {
                    readableFiles.add(new ReadableFile(file));
                    log.println("Adding file " + file.getName()
                            + " for Protecode SC scan");
                }
            } else {
                log.println(
                    String.format(
                        "Could not get files to scan from %s", 
                        fileDirectory
                    )
                );
            }
        } else {
            log.print("Directory empty, no files to scan with ProtecodeSC");
        }

        List<? extends Run<?, ?>.Artifact> buildArtifacts = run.getArtifacts();
        for (Run<?, ?>.Artifact buildArtifact : buildArtifacts) {
            readableFiles.add(new ReadableFile(buildArtifact.getFile()));
        }
        
        return readableFiles;
    }         
    
    public static String replaceSpaceWithUnderscore(String line) {
        // TODO, use something which is certainly not used in other files. Underscore isn't good.
        return line.replace(" ", "_");
    }
    
    public static boolean makeDirectory(String name, FilePath workspace, TaskListener listener) {        
        PrintStream log = listener.getLogger();        
        FilePath jsonReportDirectory = workspace.child("reports");
        try {
            jsonReportDirectory.mkdirs();
            if (!jsonReportDirectory.isDirectory()) {
                log.println("Report directory could not be created.");
                return false;
            }
        } catch (IOException | InterruptedException e) {
            return false;
        }
        return true;
    }
}
