/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.synopsys.protecode.sc.jenkins.interfaces.Listeners;
import com.synopsys.protecode.sc.jenkins.interfaces.Listeners.ResultService;
import com.synopsys.protecode.sc.jenkins.types.Sha1Sum;
import com.synopsys.protecode.sc.jenkins.types.Types;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.file.Files;


/**
 *
 * @author pajunen
 */
public class DemoPlugin {
    private static ProtecodeScService pcs = null;
    private static int id = 0;    
    private static Sha1Sum sha1sum;
    
    public static void main(String[] args) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        // URL url, String group, String name, String password
        try {
            Configuration.instantiate(
                new URL("http://localhost:8000"), 
                "1", 
                "admin", 
                "adminadminadmin"
            );
        } catch (MalformedURLException e) {
        }
        pcs = new ProtecodeScService();
        
        showMenu();
        
        String command = "";
        while (!"0".equals(command)) {
            try {
                command = in.readLine();
                switch(command){
                    case "0":
                        System.exit(0);
                    case "1":
                        showGroups();
                        break;
                    case "2":
                        scanFile();
                        break;
                    case "3":
                        pollOnce();
                        break;
                    case "4":
                        fetchResult();
                        break;
                }
            } catch (IOException ioe) {
                print("Boom, again: ");                
            }
        }
    }
    
    private static void showGroups() {        
        pcs.groups((Types.Groups groups) -> {
            print(groups.toString());
        });
    }
    
    private static void scanFile() throws IOException {
        print("requesting scan");
        byte [] bytes = Files.readAllBytes(Paths.get("/Users/pajunen/vlc.dmg"));        
        pcs.scan("vlc.dmg", bytes, (Types.UploadResponse result) -> {
            print(result.toString());
            id = result.getResults().getId();
        });
    }
    
    private static void pollOnce() throws IOException {
        print("polling");
        pcs.poll(id, (Types.UploadResponse status) -> {
            print("Process status is: " + status);
            sha1sum = new Sha1Sum(status.getResults().getSha1sum());
        });
    }
    
    private static void fetchResult() throws IOException {
        print("fetching");
        pcs.scanResult(sha1sum, (Types.ScanResultResponse result) -> {
        //pcs.scanResult(sha1sum, (String result) -> {
            print(result.toString());
        });
    }
    
    private static void showMenu() {
        print("Welcome to demo plugin");
        print("");
        print("Press 1. to get groups from Protecode SC");
        print("Press 2. to scan vlc file - blocks");
        print("Press 3. to poll");  
        print("Press 4. to fetch result (after fetching status with 3.)");
        print("Press 0. to exit");                
    }
    
    private static void print(String line) {
        System.out.println(line);
    }
}