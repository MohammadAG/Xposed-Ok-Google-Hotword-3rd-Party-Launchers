package com.mohammadag.thirdpartylauncherhotword;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class RefreshLaunchersActivity extends Activity {
	private static final String PACKAGE_NAME = RefreshLaunchersActivity.class.getPackage().getName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_refresh);

		SettingsHelper helper = new SettingsHelper(getApplicationContext());
		helper.refreshLaunchers(this, (TextView) findViewById(R.id.textView1));

		findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(RefreshLaunchersActivity.this, ApplicationListActivity.class));
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.toggle_launcher_icon:
			PackageManager pm = getPackageManager();
			ComponentName cmp = new ComponentName(getApplicationContext(), PACKAGE_NAME + ".Activity-Launcher");
			pm.setComponentEnabledSetting(
					cmp,
					pm.getComponentEnabledSetting(cmp) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ? 
							PackageManager.COMPONENT_ENABLED_STATE_ENABLED : 
								PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
								PackageManager.DONT_KILL_APP
					);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
