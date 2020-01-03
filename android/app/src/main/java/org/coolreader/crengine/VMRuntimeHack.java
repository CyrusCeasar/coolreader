package org.coolreader.crengine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.util.Log;

public class VMRuntimeHack {
	private Object runtime = null;
	private Method trackAllocation = null;
	private Method trackFree = null;
	private static int totalSize = 0;
	
	public boolean trackAlloc(long size) {
		if (runtime == null)
			return false;
		totalSize += size;
		L.v("trackAlloc(" + size + ")  total=" + totalSize);
		try {
			Object res = trackAllocation.invoke(runtime, Long.valueOf(size));
			return (res instanceof Boolean) ? (Boolean)res : true;
		} catch (IllegalArgumentException e) {
			return false;
		} catch (IllegalAccessException e) {
			return false;
		} catch (InvocationTargetException e) {
			return false;
		}
	}
	public boolean trackFree(long size) {
		if (runtime == null)
			return false;
		totalSize -= size;
		L.v("trackFree(" + size + ")  total=" + totalSize);
		try {
			Object res = trackFree.invoke(runtime, Long.valueOf(size));
			return (res instanceof Boolean) ? (Boolean)res : true;
		} catch (IllegalArgumentException e) {
			return false;
		} catch (IllegalAccessException e) {
			return false;
		} catch (InvocationTargetException e) {
			return false;
		}
	}
	public VMRuntimeHack() {
		if (!DeviceInfo.USE_BITMAP_MEMORY_HACK)
			return;
		boolean success = false;
		try {
			Class<?> cl = Class.forName("dalvik.system.VMRuntime");
			Method getRt = cl.getMethod("getRuntime");
			runtime = getRt.invoke(null);
			trackAllocation = cl.getMethod("trackExternalAllocation", long.class);
			trackFree = cl.getMethod("trackExternalFree", long.class);
			success = true;
		} catch (ClassNotFoundException e) {
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
		if (!success) {
			Log.i("cr3", "VMRuntime hack does not work!");
			runtime = null;
			trackAllocation = null;
			trackFree = null;
		}
	}
}
