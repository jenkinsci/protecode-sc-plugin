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
import hudson.model.BuildListener;
import hudson.model.Run;
import java.io.File;
import java.io.FileFilter;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Utils {
    
    private Utils(){
        // don't instantiate me...
    }
    
    private static final Logger LOGGER = Logger.getLogger(ProtecodeScService.class.getName());      
    
    public static void log(String toLog) {
        LOGGER.log(Level.ALL, toLog);
    }
    
    private static class ScanFileFilter implements FileFilter, Serializable {
        @Override
        public boolean accept(File pathname) {
            return pathname.isFile();

        }
    }
    
    public static List<ReadableFile> getFiles(String fileDirectory, AbstractBuild<?, ?> build,
            BuildListener listener) throws IOException, InterruptedException {
        // TODO, make sure the path works, add / to end and so forth
        PrintStream log = listener.getLogger();
        List<ReadableFile> readableFiles = new ArrayList<>();
        log.println("Reading from directory: " + fileDirectory);
        if (!StringUtils.isEmpty(fileDirectory)) {
            @SuppressWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
            List<FilePath> files = build.getWorkspace().child(fileDirectory)
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

        List<? extends Run<?, ?>.Artifact> buildArtifacts = build
                .getArtifacts();
        for (Run<?, ?>.Artifact buildArtifact : buildArtifacts) {
            readableFiles.add(new ReadableFile(buildArtifact.getFile()));
        }
        
        return readableFiles;
    }         
    
    public static String replaceSpaceWithPlus(String line) {
        return line.replaceAll(" ", "+");
    }
    
    public static boolean makeDirectory(AbstractBuild<?, ?> build, String name, BuildListener listener) {        
        PrintStream log = listener.getLogger();        
        FilePath jsonReportDirectory = build.getWorkspace().child("reports");
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
