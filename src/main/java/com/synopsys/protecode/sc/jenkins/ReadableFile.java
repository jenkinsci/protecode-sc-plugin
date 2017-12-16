/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template filePath, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import hudson.FilePath;
import java.io.*;
import lombok.*;
import okhttp3.MediaType;
//import okhttp3.RequestBody;
import okio.BufferedSink;


/**
 *
 * @author pajunen
 */
public class ReadableFile {
    @Getter @Setter private FilePath filePath = null;

    public ReadableFile(String path) {       
        this.filePath = new FilePath(new File(path));
    }
    
    public ReadableFile (FilePath file){        
        this.filePath = file;
    }
    
    public ReadableFile(File file) {
        this.filePath = new FilePath(file);
    }
    
    public String name() {
        return filePath.getName();
    }
    
    public InputStream read() throws IOException, InterruptedException {
        return filePath.read();
    }
    
    public boolean isDirectory() throws IOException, InterruptedException {
        return filePath.isDirectory();
    }
}
