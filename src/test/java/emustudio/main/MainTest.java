/*
 * MainTest.java
 *
 * (c) Copyright 2012, Peter Jakubčo
 */
package emustudio.main;

import java.util.Date;
import junit.framework.TestCase;

/**
 *
 * @author vbmacher
 */
public class MainTest extends TestCase {
    
    public MainTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testPassword() {
        String password = emulib.runtime.Context.SHA1(String.valueOf(Math.random())
                + new Date().toString());
        assertTrue(emulib.runtime.Context.assignPassword(password));
        password = emulib.runtime.Context.SHA1(String.valueOf(Math.random())
                + new Date().toString());
        assertFalse(emulib.runtime.Context.assignPassword(password));
    }
    
    
}
