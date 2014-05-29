package com.mohammadag.thirdpartylauncherhotword;

public class LauncherInfo {

	private String mPackageName;
	private String mActivityName;

	public LauncherInfo(String packageName, String activityName) {
		mPackageName = packageName;
		mActivityName = activityName;
	}

	public String infoToString() {
		return mPackageName + "//" + mActivityName;
	}

	public static LauncherInfo fromString(String string) {
		String[] array = string.split("//");
		return new LauncherInfo(array[0], array[1]);
	}

	public String getPackageName() {
		return mPackageName;
	}

	public String getActivityName() {
		return mActivityName;
	}
}
