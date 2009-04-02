/*
 * Main.java
 *
 * Created on Nedeľa, 2007, august 5, 13:08
 *
 * KISS, YAGNI
 */

package architecture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.EventObject;

import plugins.compiler.IMessageReporter;
import plugins.cpu.ICPU;
import plugins.cpu.ICPUContext.ICPUListener;

import runtime.StaticDialogs;
import gui.AutoDialog;
import gui.LoadingDialog;
import gui.OpenArchDialog;
import gui.StudioFrame;

/**
 *
 * @author vbmacher
 */
public class Main {
    public static ArchLoader aloader;
    public static ArchHandler currentArch = null;    
    private static String inputFile = null;
    private static String outputFile = null;    
	private static int result_state;
	private static FileWriter outw = null;

    private static void output_message(String message) throws IOException {
    	if (outw != null) {
    		outw.write(message + "\n");
    		outw.flush();
    	}
    }
    
    /**
     * Method performs emulation automation.
     * It is supposed that currentArch != null.
     */
    private static void auto() {
        AutoDialog adia = new AutoDialog();
        adia.setVisible(true);
        int phase = 1;
        
		try {
			File outf = null;
			
			if (outputFile != null) {
				outf = new File(outputFile);
				outw = new FileWriter(outf);
			}
			output_message("Starting emulation: #" +
					new Date().toString());
			output_message("    Loaded configuration:");
			output_message("        compiler: " +  currentArch.getCompiler().getTitle());
			output_message("        cpu     : " +  currentArch.getCPU().getTitle());
			output_message("        memory  : " +  currentArch.getMemory().getTitle());
			for (int i = 0; i < currentArch.getDevices().length; i++)
				output_message("        dev[" + String.format("%02d", i) + "] : " 
					+  currentArch.getDevices()[i].getTitle());
			output_message("");
			
			if (inputFile != null) {
		        adia.setAction("Compiling input file...", false);
				output_message("#" +(phase++) + " Compiling file: " + inputFile);

				IMessageReporter reporter = new IMessageReporter() {
					@Override
		            public void report(String message, int type) {
						String tt = "      ";
						
						switch (type) {
							case IMessageReporter.TYPE_ERROR:
								tt += "(Error) "; break;
							case IMessageReporter.TYPE_INFO:
								tt += "(Info) "; break;
							case IMessageReporter.TYPE_WARNING:
								tt += "(Warning) "; break;
						}
						try {
							output_message(tt + message);
						} catch (IOException e) {}
					}
					@Override
		            public void report(int row, int col, String message, int type) {
						String tt = "      ["+row + ";" + col +"] ";
						switch (type) {
							case IMessageReporter.TYPE_ERROR:
								tt += "(Error) "; break;
							case IMessageReporter.TYPE_INFO:
								tt += "(Info) "; break;
							case IMessageReporter.TYPE_WARNING:
								tt += "(Warning) "; break;
						}
						try {
							output_message(tt + message);
						} catch (IOException e) { }
		            }
		        };
		        
		        // Initialize compiler
		        currentArch.getCompiler().initialize(currentArch, reporter);

		    	File f = new File(inputFile);
		    	FileReader fileR;
				fileR = new FileReader(f);
				BufferedReader r = new BufferedReader(fileR);

		        String fn = inputFile.substring(0,inputFile.lastIndexOf(".")) + ".hex";				
	    		boolean succ = currentArch.getCompiler().compile(fn, r, 
	        		currentArch.getMemory().getContext());
	    		
	    		if (succ == false) {
	    			StaticDialogs.showErrorMessage("Error: compile process failed!");
					output_message("    => FAILED");
	    			return;
	    		} else {
					output_message("    => DONE");	    			
	    		}
	        
	        	int programStart = currentArch.getCompiler().getProgramStartAddress();
	        	currentArch.getMemory().setProgramStart(programStart);
	        	currentArch.getCPU().reset(programStart);
			}
        	adia.setAction("Running emulation...", true);
			output_message("#" + (phase++) + " Running emulation");
        	
			result_state = ICPU.STATE_STOPPED_NORMAL;
        	final Thread t = new Thread() {
        		public void run() {
        			currentArch.getCPU().execute();
        			// waits till something interrupts this thread
        			try {
						while (true)
							Thread.sleep(0xfffffff);
					} catch (InterruptedException e) {
					}
        		}
        	};
			currentArch.getCPU().getContext().addCPUListener(new ICPUListener() {
				@Override
				public void runChanged(EventObject evt, int state) {
					if (state != ICPU.STATE_RUNNING) {
						result_state = state;
						t.interrupt();
					}
				}
				@Override
				public void stateUpdated(EventObject evt) {}        				
			});
        	t.start();        	
        	t.join();
        	
        	switch (result_state) {
        		case ICPU.STATE_STOPPED_ADDR_FALLOUT:
        			output_message("    => FAILED (address fallout)");
                    break;
        		case ICPU.STATE_STOPPED_BAD_INSTR:
        			output_message("    => FAILED (unknown instruction)");
        			break;
        		case ICPU.STATE_STOPPED_BREAK:
        			output_message("    => DONE (breakpoint stop)");
        			break;
        		case ICPU.STATE_STOPPED_NORMAL:
        			output_message("    => DONE (normal stop)");
        			break;
        	    default:
        			output_message("    => FAILED (invalid state)");
        	        break;
        	}
			output_message("       (instr. position: " +
					String.format("%04Xh", 
							currentArch.getCPU().getInstrPosition())
					+ ")");
        	
        	adia.setAction("Emulation finished.", false);

		} catch (FileNotFoundException e1) {
			StaticDialogs.showErrorMessage("Error: Input file not found!");
		} catch (IOException e2) {
			StaticDialogs.showErrorMessage("Error in writing to output file");
    	} catch (Exception e) {
    		StaticDialogs.showErrorMessage("Error in compile process:\n" + e.toString());
    		e.printStackTrace();
    	} finally {
    		adia.dispose();
    		adia = null;
        	try {
				output_message("\nEmulation finished: #" +
						new Date().toString());
			} catch (IOException e1) {}
    		try {outw.close();} catch (Exception e) {}
    	}
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try { javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName()); }
        catch (javax.swing.UnsupportedLookAndFeelException e) {}
        catch (ClassNotFoundException e) {}
        catch (InstantiationException e) {}
        catch (IllegalAccessException e) {}

        aloader = new ArchLoader();
        String configName = null;
        boolean auto = false;
        
        // process arguments
        if (args != null && args.length > 1) {
        	int i = 0;
        	while (i < args.length) {
        		String arg = args[i++].toUpperCase();
        		if (arg.equals("-CONFIG")) {
        			// what configuration to load 
        			if (configName != null) {
        				StaticDialogs.showErrorMessage("Error: Config file already defined!");
        				return;
        			}
        			configName = args[i++];
        		} else if (arg.equals("-INPUT")) {
        			// what input file take to compiler
        			if (inputFile != null) {
        				StaticDialogs.showErrorMessage("Error: Input file already defined!");
        				return;
        			}
        			inputFile = args[i++];
        		} else if (arg.equals("-OUTPUT")) {
        			// what output file take for emuStudio messages during
        			// automation process. This option has a meaning
        			// only if the "-auto" option is set too.
        			if (outputFile != null) {
        				StaticDialogs.showErrorMessage("Error: Output file already defined!");
        				return;
        			}
        			outputFile = args[i++];
        		} else if (arg.equals("-AUTO")) {
        			auto = true;
        		} else {
        			StaticDialogs.showErrorMessage("Error: Invalid command line argument!\n" +
        					"(" + arg + ")");
        			return;
        		}
        	}
        }
        
        if (configName == null) {
        	OpenArchDialog odi = new OpenArchDialog();
        	odi.setVisible(true);
        	if (odi.getOK()) configName = odi.getArchName();
        	if (configName == null) return;
        }

        LoadingDialog splash = new LoadingDialog();
        splash.setVisible(true);
        try {
        	if (auto)
        		currentArch = aloader.load(configName, true);
        	else
            	currentArch = aloader.load(configName, false);
        } catch(Error er) {
            String h = er.getLocalizedMessage();
            if (h == null || h.equals("")) h = "Unknown error";
            StaticDialogs.showErrorMessage("Fatal Error: " + h);
            currentArch = null;
        }
        splash.dispose();
        splash = null;
        
        if (currentArch != null) {
        	if (!auto) {
        		if (inputFile != null)
        			new StudioFrame(inputFile).setVisible(true);
        		else
        			new StudioFrame().setVisible(true);
        	} else {
        		auto();
            	currentArch.destroy();
            	System.exit(0);
        	}
        }
        else {
        	System.exit(0);
        }        
    }
    
}
