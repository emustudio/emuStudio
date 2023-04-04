/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.memory.ssem.gui.actions;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import org.junit.Ignore;
import org.junit.Test;

import javax.swing.table.AbstractTableModel;

import static org.easymock.EasyMock.*;

public class EraseMemoryActionTest {

    //java.lang.IllegalAccessException: no such field: javax.swing.table.AbstractTableModel$$$EasyMock$1.$callback/org.easymock.internal.ClassMockingData/putField
    //java.lang.RuntimeException: java.lang.IllegalAccessException: no such field: javax.swing.table.AbstractTableModel$$$EasyMock$1.$callback/org.easymock.internal.ClassMockingData/putField
    //	at org.easymock.internal.ClassProxyFactory.getCallbackSetter(ClassProxyFactory.java:259)
    //	at org.easymock.internal.ClassProxyFactory.createProxy(ClassProxyFactory.java:192)
    //	at org.easymock.internal.MocksControl.createMock(MocksControl.java:108)
    //	at org.easymock.internal.MocksControl.createMock(MocksControl.java:81)
    //	at org.easymock.IMocksControl.mock(IMocksControl.java:44)
    //	at org.easymock.EasyMock.mock(EasyMock.java:70)
    //	at org.easymock.EasyMock.createMock(EasyMock.java:322)
    //	at net.emustudio.plugins.memory.ssem.gui.actions.EraseMemoryActionTest.testEraseMemoryAction(EraseMemoryActionTest.java:32)
    //	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    //	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
    //	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    //	at java.base/java.lang.reflect.Method.invoke(Method.java:566)
    //	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
    //	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
    //	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
    //	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
    //	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
    //	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
    //	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
    //	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
    //	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
    //	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
    //	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
    //	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
    //	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
    //	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
    //	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
    //	at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
    //	at org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecutor.runTestClass(JUnitTestClassExecutor.java:108)
    //	at org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecutor.execute(JUnitTestClassExecutor.java:58)
    //	at org.gradle.api.internal.tasks.testing.junit.JUnitTestClassExecutor.execute(JUnitTestClassExecutor.java:40)
    //	at org.gradle.api.internal.tasks.testing.junit.AbstractJUnitTestClassProcessor.processTestClass(AbstractJUnitTestClassProcessor.java:60)
    //	at org.gradle.api.internal.tasks.testing.SuiteTestClassProcessor.processTestClass(SuiteTestClassProcessor.java:52)
    //	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    //	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
    //	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    //	at java.base/java.lang.reflect.Method.invoke(Method.java:566)
    //	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:36)
    //	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
    //	at org.gradle.internal.dispatch.ContextClassLoaderDispatch.dispatch(ContextClassLoaderDispatch.java:33)
    //	at org.gradle.internal.dispatch.ProxyDispatchAdapter$DispatchingInvocationHandler.invoke(ProxyDispatchAdapter.java:94)
    //	at com.sun.proxy.$Proxy2.processTestClass(Unknown Source)
    //	at org.gradle.api.internal.tasks.testing.worker.TestWorker$2.run(TestWorker.java:176)
    //	at org.gradle.api.internal.tasks.testing.worker.TestWorker.executeAndMaintainThreadName(TestWorker.java:129)
    //	at org.gradle.api.internal.tasks.testing.worker.TestWorker.execute(TestWorker.java:100)
    //	at org.gradle.api.internal.tasks.testing.worker.TestWorker.execute(TestWorker.java:60)
    //	at org.gradle.process.internal.worker.child.ActionExecutionWorker.execute(ActionExecutionWorker.java:56)
    //	at org.gradle.process.internal.worker.child.SystemApplicationClassLoaderWorker.call(SystemApplicationClassLoaderWorker.java:113)
    //	at org.gradle.process.internal.worker.child.SystemApplicationClassLoaderWorker.call(SystemApplicationClassLoaderWorker.java:65)
    //	at worker.org.gradle.process.internal.worker.GradleWorkerMain.run(GradleWorkerMain.java:69)
    //	at worker.org.gradle.process.internal.worker.GradleWorkerMain.main(GradleWorkerMain.java:74)
    @Test
    @Ignore
    public void testEraseMemoryAction() {
        AbstractTableModel tableModel = createMock(AbstractTableModel.class);
        tableModel.fireTableDataChanged();
        expectLastCall().once();
        replay(tableModel);

        MemoryContext<Byte> memory = createMock(MemoryContext.class);
        memory.clear();
        expectLastCall().once();
        replay(memory);

        EraseMemoryAction action = new EraseMemoryAction(tableModel, memory);
        action.actionPerformed(null);
    }
}
