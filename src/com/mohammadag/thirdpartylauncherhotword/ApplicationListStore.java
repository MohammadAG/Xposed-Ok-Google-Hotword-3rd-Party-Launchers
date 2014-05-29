package com.mohammadag.thirdpartylauncherhotword;

import android.widget.ArrayAdapter;

public class ApplicationListStore {
	private static ApplicationListStore mInstance;
	private ArrayAdapter<?> mAdapter;

	private ApplicationListStore() { };

	public static ApplicationListStore getInstance() {
		if (mInstance == null)
			mInstance = new ApplicationListStore();

		return mInstance;
	}

	public ArrayAdapter<?> getArrayAdapter() {
		return mAdapter;
	}

	public void setAdapter(ArrayAdapter<?> adapter) {
		mAdapter = adapter;
	}
}
