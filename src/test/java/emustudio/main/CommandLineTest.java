/*
 * CommandLineTest.java
 * 
 * Copyright (C) 2012 vbmacher
 * 
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package emustudio.main;

import emustudio.main.CommandLineFactory.CommandLine;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test of CommandLineParser.
 * 
 * @author vbmacher
 */
public class CommandLineTest {
    
    @Test
    public void testCommandLineParserSuccess() throws InvalidCommandLineException {
        String[] args = new String[]{
            "--cOnFig", "computer",
            "--input", "source",
            "--help",
            "--output", "log",
            "--auto",
            "--hash", "some.dot.class",
            "--nogui"
        };
        
        CommandLine commandLine = CommandLineFactory.parseCommandLine(args);
        Assert.assertEquals("computer", commandLine.getConfigName());
        Assert.assertEquals("source", commandLine.getInputFileName());
        Assert.assertEquals("log", commandLine.getOutputFileName());
        Assert.assertEquals("some.dot.class", commandLine.getClassNameForHash());
        Assert.assertTrue(commandLine.helpWanted());
        Assert.assertTrue(commandLine.noGUIWanted());
        Assert.assertTrue(commandLine.autoWanted());
        
        args = new String[]{
            "--cOnFig", "computer",
            "--inpUT", "source",
            "--haSh", "some.dot.class",
            "--noguI"
        };
        
        commandLine = CommandLineFactory.parseCommandLine(args);
        Assert.assertEquals("computer", commandLine.getConfigName());
        Assert.assertEquals("source", commandLine.getInputFileName());
        Assert.assertNull(commandLine.getOutputFileName());
        Assert.assertEquals("some.dot.class", commandLine.getClassNameForHash());
        Assert.assertFalse(commandLine.helpWanted());
        Assert.assertTrue(commandLine.noGUIWanted());
        Assert.assertFalse(commandLine.autoWanted());
    }
    
    @Test(expected = InvalidCommandLineException.class)
    public void testConfigNameFailure() throws InvalidCommandLineException {
        String[] args = new String[] {
            "--cOnFig", 
        };
        CommandLine commandLine = CommandLineFactory.parseCommandLine(args);
    }
    
    @Test(expected = InvalidCommandLineException.class)
    public void testInputFileNameFailure() throws InvalidCommandLineException {
        String[] args = new String[] {
            "--input", 
        };
        CommandLine commandLine = CommandLineFactory.parseCommandLine(args);
    }
    
    @Test(expected = InvalidCommandLineException.class)
    public void testOutputFileNameFailure() throws InvalidCommandLineException {
        String[] args = new String[] {
            "--output",
        };
        CommandLine commandLine = CommandLineFactory.parseCommandLine(args);
    }
    
    @Test(expected = InvalidCommandLineException.class)
    public void testClassToHashFailure() throws InvalidCommandLineException {
        String[] args = new String[] {
            "--hash",
        };
        CommandLine commandLine = CommandLineFactory.parseCommandLine(args);
    }
    
    @Test(expected = InvalidCommandLineException.class)
    public void testUnknownArgument() throws InvalidCommandLineException {
        String[] args = new String[] {
            "--dsfs s",
        };
        CommandLine commandLine = CommandLineFactory.parseCommandLine(args);
    }
    
    
}
