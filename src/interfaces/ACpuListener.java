/*
 * ACpuListener.java
 * (interface)
 *
 * Created on 18.6.2008, 9:31:16
 * hold to: KISS, YAGNI
 *
 */

package interfaces;

import java.util.EventObject;
import plugins.cpu.ICPUContext.ICPUListener;

/**
 *
 * @author vbmacher
 */
public interface ACpuListener extends ICPUListener {
    public void frequencyChanged(EventObject evt, float freq);
}
