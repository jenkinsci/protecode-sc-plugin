/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.synopsys.protecode.sc.jenkins.types.InternalTypes;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import java.util.List;

/**
 *
 * @author pajunen
 */
public class ProtecodeEvaluator {
    public static boolean evaluate(
        List<InternalTypes.FileAndResult> results, 
        AbstractBuild<?, ?> build, 
        BuildListener listener
    ) {
        return results.stream().anyMatch((fileAndResult) -> (!fileAndResult.verdict()));               
    }
}
