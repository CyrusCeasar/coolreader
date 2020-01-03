package org.coolreader.crengine;

public interface Logger {
	void i(String msg);
	void i(String msg, Exception e);
	void w(String msg);
	void w(String msg, Exception e);
	void e(String msg);
	void e(String msg, Exception e);
	void d(String msg);
	void d(String msg, Exception e);
	void v(String msg);
	void v(String msg, Exception e);
	void setLevel(int level);
}
