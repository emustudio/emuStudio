/**
 * Architecture.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */
package architecture;

import java.util.ArrayList;
import java.util.Hashtable;

import plugins.IPlugin;
import plugins.compiler.ICompiler;
import plugins.cpu.ICPU;
import plugins.device.IDevice;
import plugins.device.IDeviceContext;
import plugins.memory.IMemory;

public class Architecture {
	private long cpuHash;
	private long memoryHash;
	private long compilerHash;
	
	private ICPU cpu;
	private ICompiler compiler;
	private IMemory memory;
	
	private Hashtable<Long,IDevice> devices;
	private IDevice[] devicesArray;

	private Hashtable<IPlugin,ArrayList<IPlugin>> connections;
	
	public Architecture(ICPU cpu, long cpuHash, IMemory memory,
			long memoryHash, ICompiler compiler, long compilerHash,
			Hashtable<Long,IDevice> devices, IDevice[] devicesArray,
			Hashtable<IPlugin,ArrayList<IPlugin>> connections) {
		this.cpu = cpu;
		this.cpuHash = cpuHash;
		this.memory = memory;
		this.memoryHash = memoryHash;
		this.compiler = compiler;
		this.compilerHash = compilerHash;
		this.devices = devices;
		this.devicesArray = devicesArray;
		this.connections = connections;
    }
	
	public ICPU      getCPU()          { return cpu;          }
	public ICompiler getCompiler()     { return compiler;     }
	public IMemory   getMemory()       { return memory;       }
	
	public IPlugin   getPlugin(long hash) {
		if (hash == cpuHash) return cpu;
		if (hash == memoryHash) return memory;
		if (hash == compilerHash) return compiler; 
		return devices.get(hash);
	}
	
	public IDevice[] getAllDevices()   { return devicesArray; }
	
	public IDevice getDevice(int index) {
		return devicesArray[index];
	}

	public int getDeviceCount() {
		return devices.size();
	}
	
	/**
	 * Method determine if plugins <code>plugin1</code> 
	 * and <code>plugin2</code> are connected. Used for
	 * determining connections between cpu,memory and devices.
	 * 
	 * @param plugin1  Plugin1
	 * @param plugin2  Plugin2
	 * @return true if plugins 1 and 2 are connected; false otherwise
	 */
    public boolean isConnected(IPlugin plugin1, IPlugin plugin2) {
    	// at first suppose that plugin1 is female
		ArrayList<IPlugin> males = connections.get(plugin1);
		if ((males != null) && males.contains(plugin2)) return true;
		
		// now suppose plugin2 is female
		males = connections.get(plugin2);
		if ((males != null) && males.contains(plugin1)) return true;
    	return false;
    }

    /**
     * Method returns devices that represent male plugs in the
     * connection, for specified female.
     * 
     * @param female  Female plug
     * @return all male plug contexts for specified female
     */
    public IDeviceContext[] getMales(IDevice female) {
    	ArrayList<IPlugin> males = connections.get(female); 
    	if (males == null) return null;
    	
    	ArrayList<IDeviceContext> maleDevices = new ArrayList<IDeviceContext>();
    	
    	for (int i = 0; i < males.size(); i++) {
    		IPlugin male = males.get(i);
    		if (male instanceof IDevice)
    			maleDevices.add(((IDevice)male).getNextContext());
    	}
    	return (IDeviceContext[])maleDevices.toArray(new IDeviceContext[0]);
    }
    
    /**
     * Perform reset of all plugins 
     */
    public void resetPlugins() {
    	compiler.reset();
    	cpu.reset();
    	memory.reset();
        for (int i=0; i < devicesArray.length; i++)
        	devicesArray[i].reset();
    }
}
