package com.mohammadag.thirdpartylauncherhotword;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityListActivity extends ListActivity {
	private String mPackageName;
	private String mFriendlyPackageName;
	private SettingsHelper mSettingsHelper;

	private ArrayList<String> mActivityList = new ArrayList<String>();
	private ArrayList<String> mFilteredActivityList = new ArrayList<String>();

	private MenuItem mSearchItem;
	private ActivityListAdapter mActivityListAdapter;
	private String mNameFilter;
	private Set<LauncherInfo> mLaunchers;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSettingsHelper = new SettingsHelper(getApplicationContext());
		mLaunchers = mSettingsHelper.getLaunchers();

		Intent intent = getIntent();
		mPackageName = intent.getStringExtra("package_name");
		mFriendlyPackageName = intent.getStringExtra("package_label");

		loadActivitesForPackage(mPackageName);

		float scale = getResources().getDisplayMetrics().density;
		int padding = (int) (8 * scale + 0.5f);
		getListView().setPadding(padding * 2, padding, padding * 2, padding);
		getListView().setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);

		setTitle(mFriendlyPackageName);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_picker_menu, menu);

		mSearchItem = menu.findItem(R.id.action_search);
		final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
		searchView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

		mSearchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem menuItem) {
				searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

					@Override
					public boolean onQueryTextSubmit(String query) {
						mNameFilter = query;
						mActivityListAdapter.getFilter().filter(mNameFilter);
						findViewById(R.id.action_search).clearFocus();
						return false;
					}

					@Override
					public boolean onQueryTextChange(String newText) {
						mNameFilter = newText;
						mActivityListAdapter.getFilter().filter(mNameFilter);
						return false;
					}

				});
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem menuItem) {
				mActivityListAdapter.getFilter().filter("");
				return true;
			}
		});

		return true;
	}

	@Override
	public boolean onSearchRequested() {
		mSearchItem.expandActionView();
		return super.onSearchRequested();
	}

	private void refreshList() {
		mLaunchers = mSettingsHelper.getLaunchers();
		mActivityListAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		if (position == 0) {
			PackageManager pm = getPackageManager();
			PackageInfo info;
			try {
				info = pm.getPackageInfo(mPackageName, PackageManager.GET_ACTIVITIES);
				ActivityInfo[] acList = info.activities;
				for (ActivityInfo acInfo : acList) {
					LauncherInfo lInfo = new LauncherInfo(acInfo.packageName, acInfo.name);
					boolean exists = !mSettingsHelper.addLauncher(lInfo);
					if (exists) mSettingsHelper.removeLauncher(lInfo);
				}
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			refreshList();
			return;
		}
		String activityName = ((TextView) view.findViewById(android.R.id.text1)).getText().toString();
		LauncherInfo info = new LauncherInfo(mPackageName, activityName);
		if (isEnabled(activityName)) {
			mSettingsHelper.removeLauncher(info);
		} else {
			mSettingsHelper.addLauncher(info);
		}

		refreshList();
	}

	public boolean isEnabled(String activityName) {
		for (LauncherInfo info : mLaunchers) {
			if (info.getActivityName().equals(activityName))
				return true;
		}

		return false;
	}

	private void loadActivitesForPackage(String packageName) {
		try {
			mActivityList.clear();

			PackageManager pm = getPackageManager();
			PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			ActivityInfo[] list = info.activities;
			getActionBar().setIcon(info.applicationInfo.loadIcon(pm));

			mActivityList.add(getString(R.string.all_the_wonderful_activities));

			if (list == null) {
				Toast.makeText(this, "No activities", Toast.LENGTH_SHORT).show();
				finish();
				return;
			}

			for (int i = 0; i < list.length; i++) {
				mActivityList.add(list[i].name);
			}

			mActivityListAdapter = new ActivityListAdapter(this, mActivityList);

			getListView().setAdapter(mActivityListAdapter);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			finish();
		}
	}

	class ActivityListAdapter extends ArrayAdapter<String> implements Filterable {
		private Filter filter;

		public ActivityListAdapter(Context context, List<String> items) {
			super(context, 0, items);
			mFilteredActivityList.addAll(items);
			filter = new ActivityListFilter(this);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// Load or reuse the view for this row
			View row = convertView;
			if (row == null) {
				row = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
			}

			String activityName = mFilteredActivityList.get(position);

			TextView textView = (TextView) row.findViewById(android.R.id.text1);
			textView.setText(activityName);
			boolean isEnabled = ActivityListActivity.this.isEnabled(activityName);

			textView.setTextColor(isEnabled ? Color.parseColor("#33b5e5") : Color.BLACK);

			if (position == 0) {
				textView.setTypeface(null, Typeface.BOLD);
			} else {
				textView.setTypeface(null, Typeface.NORMAL);
			}

			return row;
		}

		@Override
		public Filter getFilter() {
			return filter;
		}
	}

	private class ActivityListFilter extends Filter {

		private ActivityListAdapter adaptor;

		ActivityListFilter(ActivityListAdapter adaptor) {
			super();
			this.adaptor = adaptor;
		}

		@SuppressLint("WorldReadableFiles")
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			// NOTE: this function is *always* called from a background thread, and
			// not the UI thread.

			ArrayList<String> items = new ArrayList<String>();
			synchronized (this) {
				items.addAll(mActivityList);
			}

			FilterResults result = new FilterResults();
			if (constraint != null && constraint.length() > 0) {
				Pattern regexp = Pattern.compile(constraint.toString(), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
				for (Iterator<String> i = items.iterator(); i.hasNext(); ) {
					String name = i.next();
					if (!regexp.matcher(name == null ? "" : name).find()) {
						i.remove();
					}
				}
			}

			result.values = items;
			result.count = items.size();

			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
			// NOTE: this function is *always* called from the UI thread.
			mFilteredActivityList = (ArrayList<String>) results.values;
			adaptor.clear();
			adaptor.addAll(mFilteredActivityList);
			adaptor.notifyDataSetInvalidated();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
