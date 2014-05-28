package com.mohammadag.thirdpartylauncherhotword;

import java.util.HashMap;

import com.google.android.hotword.client.HotwordServiceClient;

import android.app.Activity;
import android.os.Bundle;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {
	private static HotwordServiceClient mHotwordServiceClient;
	private static XC_MethodHook mOnCreateHook;
	private static XC_MethodHook mOnAttachedToWindowHook;
	private static XC_MethodHook mOnDettachedFromWindowHook;
	private static XC_MethodHook mOnPauseHook;
	private static XC_MethodHook mOnResumeHook;

	/* 
	 * The Xperia launcher does not override Activity.onAttachedToWindow et all,
	 * so we work around that by calling the relevant methods in onPause and onResume.
	 */
	private boolean mOnAttachedDoesNotExist = false;
	private boolean mOnDettachedDoesNotExist = false;

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
	}

	private void createHooks() {
		mOnCreateHook = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mHotwordServiceClient = new HotwordServiceClient((Activity) param.thisObject);
			}
		};

		mOnAttachedToWindowHook = new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mHotwordServiceClient.onAttachedToWindow();
				mHotwordServiceClient.requestHotwordDetection(true);
			};
		};

		mOnDettachedFromWindowHook = new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mHotwordServiceClient.onDetachedFromWindow();
				mHotwordServiceClient.requestHotwordDetection(false);
			};
		};

		mOnPauseHook = new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (mOnDettachedDoesNotExist) {
					mHotwordServiceClient.fakeOnDetach();
				}
				mHotwordServiceClient.requestHotwordDetection(false);
			};
		};

		mOnResumeHook = new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (mOnAttachedDoesNotExist) {
					mHotwordServiceClient.fakeOnAttach();
				}
				mHotwordServiceClient.requestHotwordDetection(true);
			};
		};
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (!mActivityMap.containsKey(lpparam.packageName))
			return;

		Class<?> Launcher = XposedHelpers.findClass(mActivityMap.get(lpparam.packageName), lpparam.classLoader);
		hookLauncherClass(Launcher);
	}

	private void hookLauncherClass(Class<?> Launcher) {
		createHooks();
		try {
			XposedHelpers.findAndHookMethod(Launcher, "onCreate", Bundle.class, mOnCreateHook);
		} catch (Throwable t) {
			XposedBridge.log("Failed to add hotword to launcher: " + t.getMessage());
			return;
		}
		try {
			XposedHelpers.findAndHookMethod(Launcher, "onAttachedToWindow", mOnAttachedToWindowHook);
		} catch (Throwable t) {
			mOnAttachedDoesNotExist = true;
		}
		try {
			XposedHelpers.findAndHookMethod(Launcher, "onDetachedFromWindow", mOnDettachedFromWindowHook);
		} catch (Throwable t) {
			mOnDettachedDoesNotExist = true;
		}

		try {
			XposedHelpers.findAndHookMethod(Launcher, "onPause", mOnPauseHook);
			XposedHelpers.findAndHookMethod(Launcher, "onResume", mOnResumeHook);
		} catch (Throwable t) { }
	}
}
