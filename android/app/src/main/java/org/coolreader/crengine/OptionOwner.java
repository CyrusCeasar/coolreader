package org.coolreader.crengine;

import android.view.LayoutInflater;

public interface OptionOwner {
	BaseActivity getActivity();
	Properties getProperties();
	LayoutInflater getInflater();
}
