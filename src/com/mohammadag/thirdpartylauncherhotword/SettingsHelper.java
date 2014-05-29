package com.mohammadag.thirdpartylauncherhotword;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.widget.TextView;
import de.robv.android.xposed.XSharedPreferences;

public class SettingsHelper {
	private static final String VEL_PACKAGE = "com.google.android.googlequicksearchbox";

	public static final String PACKAGE_NAME = "com.mohammadag.thirdpartylauncherhotword";
	private static final String PREFS_NAME = PACKAGE_NAME + "_preferences";
	public static final String INTENT_SETTINGS_CHANGED = PACKAGE_NAME + ".SETTINGS_CHANGED";

	private XSharedPreferences mXPreferences = null;
	private static SharedPreferences mPreferences = null;
	private Context mContext;

	public SettingsHelper() {
		mXPreferences = new XSharedPreferences(PACKAGE_NAME);
		mXPreferences.makeWorldReadable();
		mXPreferences.reload();
	}

	public SettingsHelper(Context context) {
		mContext = context;
		mPreferences = getWritablePreferences(context);
	}

	public Context getContext() {
		return mContext;
	}

	public Editor edit() {
		return mPreferences.edit();
	}

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	public static SharedPreferences getWritablePreferences(Context context) {
		if (mPreferences == null)
			mPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE);

		return mPreferences;
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		boolean returnResult = defaultValue;
		if (mPreferences != null) {
			returnResult = mPreferences.getBoolean(key, defaultValue);
		} else if (mXPreferences != null) {
			returnResult = mXPreferences.getBoolean(key, defaultValue);
		}
		return returnResult;
	}

	public Set<String> getStringSet(String key, Set<String> defaultValue) {
		Set<String> returnResult = defaultValue;
		if (mPreferences != null) {
			returnResult = mPreferences.getStringSet(key, defaultValue);
		} else if (mXPreferences != null) {
			returnResult = mXPreferences.getStringSet(key, defaultValue);
		}
		return returnResult;
	}

	public boolean contains(String key) {
		if (mPreferences != null)
			return mPreferences.contains(key);
		else if (mXPreferences != null)
			return mXPreferences.contains(key);

		return false;
	}

	public void refreshLaunchers(Context context, TextView statusView) {
		safeAddText(statusView, "Refreshing list of launchers...\n");
		PackageManager pm = context.getPackageManager();
		Intent i = new Intent("android.intent.action.MAIN");
		i.addCategory("android.intent.category.HOME");
		List<ResolveInfo> lst = pm.queryIntentActivities(i, 0);
		if (!lst.isEmpty()) {
			for (ResolveInfo resolveInfo : lst) {
				String packageName = resolveInfo.activityInfo.packageName;
				if (VEL_PACKAGE.equals(packageName))
					continue;

				String activityName = resolveInfo.activityInfo.name;
				LauncherInfo info = new LauncherInfo(packageName, activityName);
				boolean exists = !addLauncher(info);
				safeAddText(statusView, (exists ? "Launcher already exists: "
						: "Adding new launcher: ") + packageName);
			}
		}

		safeAddText(statusView, "\nNew settings will take effect after a reboot (fast or normal)");
	}

	public static void safeAddText(TextView textView, String text) {
		if (textView != null) {
			String newString = "";
			String old = textView.getText().toString();
			if (!TextUtils.isEmpty(old))
				newString = old + "\n";
			textView.setText(newString + text);
		}
	}

	public boolean addLauncher(LauncherInfo info) {
		HashSet<String> stringSet = new HashSet<String>();
		Set<String> old = getLauncherStrings();
		if (old != null)
			stringSet.addAll(old);

		String launcherString = info.infoToString();
		if (!stringSet.contains(launcherString)) {
			stringSet.add(launcherString);
		} else {
			return false;
		}

		edit().putStringSet("launchers", stringSet).commit();
		return true;
	}

	public void removeLauncher(LauncherInfo info) {
		HashSet<String> stringSet = new HashSet<String>();
		Set<String> old = getLauncherStrings();
		if (old != null)
			stringSet.addAll(old);

		String launcherString = info.infoToString();
		if (stringSet.contains(launcherString)) {
			stringSet.remove(launcherString);
		}

		edit().putStringSet("launchers", stringSet).commit();
	}

	private Set<String> getLauncherStrings() {
		return getStringSet("launchers", null);
	}

	public Set<LauncherInfo> getLaunchers() {
		Set<String> launcherStrings = getLauncherStrings();
		if (launcherStrings == null)
			return null;

		HashSet<LauncherInfo> launchers = new HashSet<LauncherInfo>();
		for (String string : launcherStrings)
			launchers.add(LauncherInfo.fromString(string));

		return launchers;
	}
}
