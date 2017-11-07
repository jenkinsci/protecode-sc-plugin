/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins;

import com.synopsys.protecode.sc.jenkins.types.Types;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

/**
 *
 * @author pajunen
 */
public class TypesTests {
    
    @Test
    @DisplayName("Scan Id should write and read as same value")
    public void testScanId() {
        Types.ScanId id = new Types.ScanId(18);
        assertEquals(18, id.getId());
    }
}
