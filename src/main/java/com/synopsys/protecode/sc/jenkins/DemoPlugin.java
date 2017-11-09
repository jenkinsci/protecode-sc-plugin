/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

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
    private static ProtecodeSc pcs = null;
    
    public static void main(String[] args) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        // URL url, String group, String name, String password
        try {
            Configuration.instantiate(
                new URL("http://localhost:8000"), 
                "Demo_group", 
                "admin", 
                "adminadminadmin"
            );
        } catch (MalformedURLException e) {
        }
        pcs = new ProtecodeSc();
        
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
        print("bytes read");
        pcs.scan("vlc.dmg", bytes, (Types.ScanId result) -> {
            print(result.toString());
        });
    }
    
    private static void showMenu() {
        print("Welcome to demo plugin");
        print("");
        print("Press 1. to get groups from Protecode SC");
        print("Press 2. to scan vlc file - blocks");
        print("Press 0. to exit");                
    }
    
    private static void print(String line) {
        System.out.println(line);
    }
}