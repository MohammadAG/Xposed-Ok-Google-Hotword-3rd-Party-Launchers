package com.mohammadag.thirdpartylauncherhotword;

import java.util.HashMap;
import java.util.Set;

import android.app.Activity;
import android.os.Bundle;

import com.google.android.hotword.client.HotwordServiceClient;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class XposedMod implements IXposedHookZygoteInit {
	private static HotwordServiceClient mHotwordServiceClient;

	private static final HashMap<String, String> mActivityMap = new HashMap<String, String>();

	static {
		mActivityMap.put("com.htc.launcher", "com.htc.launcher.Launcher");
		mActivityMap.put("com.teslacoilsw.launcher", "com.android.launcher2.Launcher");
		mActivityMap.put("com.anddoes.launcher", "com.android.launcher2.Launcher");
		mActivityMap.put("com.sonyericsson.home", "com.sonymobile.home.HomeActivity");
		mActivityMap.put("com.sec.android.app.launcher", "com.android.launcher2.Launcher");
		mActivityMap.put("com.actionlauncher.playstore", "com.chrislacy.actionlauncher.ActionLauncher");
		mActivityMap.put("com.chrislacy.actionlauncher.pro", "com.chrislacy.actionlauncher.ActionLauncher");
		mActivityMap.put("org.adw.launcher", "org.adw.launcherlib.Launcher");
		mActivityMap.put("org.adwfreak.launcher", "org.adw.launcherlib.Launcher");
		mActivityMap.put("com.tul.aviate", "com.tul.aviator.ui.TabbedHomeActivity");
		mActivityMap.put("com.campmobile.launcher", "com.campmobile.launcher.Launcher");
		mActivityMap.put("com.kk.launcher", "com.kk.launcher.Launcher");
		mActivityMap.put("com.android.launcher3", "com.android.launcher3.Launcher");
		mActivityMap.put("com.lge.launcher2", "com.lge.launcher2.Launcher");
		mActivityMap.put("com.bam.android.inspirelauncher", "com.bam.android.inspirelauncher.Launcher");
		mActivityMap.put("com.mobint.hololauncher.hd", "com.mobint.hololauncher.Launcher");
	}

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		SettingsHelper helper = new SettingsHelper();
		Set<LauncherInfo> launchers = helper.getLaunchers();
		if (launchers == null) {
			helper = null;
			return;
		}

		for (LauncherInfo info : launchers) {
			if (mActivityMap.containsKey(info.getPackageName()))
				continue;

			mActivityMap.put(info.getPackageName(), info.getActivityName());
		}

		helper = null;
		launchers = null;

		XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (mActivityMap.containsValue(param.thisObject.getClass().getName()))
					mHotwordServiceClient = new HotwordServiceClient((Activity) param.thisObject);
			};
		});

		XposedHelpers.findAndHookMethod(Activity.class, "onAttachedToWindow", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (mActivityMap.containsValue(param.thisObject.getClass().getName())) {
					if (mHotwordServiceClient != null) {
						mHotwordServiceClient.onAttachedToWindow();
						mHotwordServiceClient.requestHotwordDetection(true);
					}
				}
			};
		});

		XposedHelpers.findAndHookMethod(Activity.class, "onDettachedFromWindow", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (mActivityMap.containsValue(param.thisObject.getClass().getName())) {
					if (mHotwordServiceClient != null) {
						mHotwordServiceClient.onDetachedFromWindow();
						mHotwordServiceClient.requestHotwordDetection(false);
					}
				}
			};
		});

		XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (mActivityMap.containsValue(param.thisObject.getClass().getName())) {
					if (mHotwordServiceClient != null) {
						mHotwordServiceClient.requestHotwordDetection(false);
					}
				}
			};
		});

		XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (mActivityMap.containsValue(param.thisObject.getClass().getName())) {
					if (mHotwordServiceClient != null) {
						mHotwordServiceClient.requestHotwordDetection(false);
					}
				}
			};
		});
	}
}
