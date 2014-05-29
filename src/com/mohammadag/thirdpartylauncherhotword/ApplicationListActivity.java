package com.mohammadag.thirdpartylauncherhotword;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class ApplicationListActivity extends ListActivity {
	private ArrayList<ApplicationInfo> mAppList = new ArrayList<ApplicationInfo>();
	private ArrayList<ApplicationInfo> mFilteredAppList = new ArrayList<ApplicationInfo>();

	private MenuItem mSearchItem;
	private AppListAdaptor mAppListAdaptor;
	private String mNameFilter;

	private ApplicationListStore mAppListStore = ApplicationListStore.getInstance();

	static class ViewHolder {
		TextView app_name;
		TextView app_package;
		ImageView app_icon;
		int position;
		ApplicationInfo app_info;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
				String pkgName = ((TextView) view.findViewById(R.id.app_package)).getText().toString();
				String label = ((TextView) view.findViewById(R.id.app_name)).getText().toString();
				Intent data = new Intent(ApplicationListActivity.this, ActivityListActivity.class);
				data.putExtra("package_name", pkgName);
				data.putExtra("package_label", label);
				startActivity(data);
			}
		});

		if (mAppListStore.getArrayAdapter() != null) {
			mAppListAdaptor = (AppListAdaptor) mAppListStore.getArrayAdapter();
			mAppListAdaptor.getFilter().filter("");
			getListView().setAdapter(mAppListAdaptor);
			getListView().setFastScrollEnabled(true);
		} else {
			new PrepareAppsAdapterTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	@Override
	public void onBackPressed() {
		setResult(RESULT_CANCELED);
		super.onBackPressed();
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
						mAppListAdaptor.getFilter().filter(mNameFilter);
						findViewById(R.id.action_search).clearFocus();
						return false;
					}

					@Override
					public boolean onQueryTextChange(String newText) {
						mNameFilter = newText;
						mAppListAdaptor.getFilter().filter(mNameFilter);
						return false;
					}

				});
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem menuItem) {
				mAppListAdaptor.getFilter().filter("");
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

	// Handle background loading of apps
	private class PrepareAppsAdapterTask extends AsyncTask<Void,Void,AppListAdaptor> {
		ProgressDialog dialog;

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(ApplicationListActivity.this);
			dialog.setMessage("Loading...");
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setCancelable(false);
			dialog.show();
		}

		@Override
		protected AppListAdaptor doInBackground(Void... params) {
			if (mAppList.size() == 0) {
				loadApps(dialog);
			}
			return null;
		}

		@Override
		protected void onPostExecute(final AppListAdaptor result) {
			mAppListAdaptor = new AppListAdaptor(ApplicationListActivity.this, mAppList);
			mAppListStore.setAdapter(mAppListAdaptor);
			getListView().setAdapter(mAppListAdaptor);
			getListView().setFastScrollEnabled(true);

			try {
				dialog.dismiss();
			} catch (Exception e) {

			}
		}
	}

	class AppListAdaptor extends ArrayAdapter<ApplicationInfo> implements SectionIndexer {
		private Map<String, Integer> alphaIndexer;
		private String[] sections;
		private Filter filter;

		public AppListAdaptor(Context context, List<ApplicationInfo> items) {
			super(context, R.layout.app_list_item, new ArrayList<ApplicationInfo>(items));

			mFilteredAppList.addAll(items);
			filter = new AppListFilter(this);
			alphaIndexer = new HashMap<String, Integer>();
			for(int i = mFilteredAppList.size() - 1; i >= 0; i--)
			{
				ApplicationInfo app = mFilteredAppList.get(i);
				String appName = app.name;
				String firstChar;
				if (appName == null || appName.length() < 1) {
					firstChar = "@";
				} else {
					firstChar = appName.substring(0, 1).toUpperCase(Locale.getDefault());
					if(firstChar.charAt(0) > 'Z' || firstChar.charAt(0) < 'A')
						firstChar = "@";
				}

				alphaIndexer.put(firstChar, i);
			}

			Set<String> sectionLetters = alphaIndexer.keySet();

			// create a list from the set to sort
			List<String> sectionList = new ArrayList<String>(sectionLetters); 

			Collections.sort(sectionList);

			sections = new String[sectionList.size()];
			sectionList.toArray(sections);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				row = getLayoutInflater().inflate(R.layout.app_list_item, parent, false);
			}

			ApplicationInfo app = mFilteredAppList.get(position);

			if (row.getTag() == null) {
				ViewHolder holder = new ViewHolder();
				holder.app_icon = (ImageView) row.findViewById(R.id.app_icon);	
				holder.app_name = (TextView) row.findViewById(R.id.app_name);
				holder.app_package = (TextView) row.findViewById(R.id.app_package);
				holder.position = position;
				holder.app_info = app;
				row.setTag(holder);
			}

			ViewHolder holder = (ViewHolder) row.getTag();

			holder.app_name.setText(app.name == null ? "" : app.name);
			holder.app_package.setTextColor(Color.parseColor("#bebebe"));
			holder.app_package.setText(app.packageName);
			holder.app_icon.setTag(app.packageName);

			new ImageLoader(holder.app_icon, app.packageName).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
					app);

			return row;
		}

		@Override
		public int getPositionForSection(int section) {
			if (section >= sections.length)
				return mFilteredAppList.size() - 1;

			return alphaIndexer.get(sections[section]);
		}

		@Override
		public int getSectionForPosition(int position) {
			// Iterate over the sections to find the closest index
			// that is not greater than the position
			int closestIndex = 0;
			int latestDelta = Integer.MAX_VALUE;

			for (int i = 0; i < sections.length; i++) {
				int current = alphaIndexer.get(sections[i]);
				if (current == position) {
					// If position matches an index, return it immediately
					return i;
				} else if (current < position) {
					// Check if this is closer than the last index we inspected
					int delta = position - current;
					if (delta < latestDelta) {
						closestIndex = i;
						latestDelta = delta;
					}
				}
			}

			return closestIndex;
		}

		@Override
		public Object[] getSections() {
			return sections;
		}

		@Override
		public Filter getFilter() {
			return filter;
		}
	}

	private class AppListFilter extends Filter {
		private AppListAdaptor adaptor;

		AppListFilter(AppListAdaptor adaptor) {
			super();
			this.adaptor = adaptor;
		}

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			// NOTE: this function is *always* called from a background thread, and
			// not the UI thread.

			ArrayList<ApplicationInfo> items = new ArrayList<ApplicationInfo>();
			synchronized (this) {
				items.addAll(mAppList);
			}

			FilterResults result = new FilterResults();
			if (constraint != null && constraint.length() > 0) {
				Pattern regexp = Pattern.compile(constraint.toString(), Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
				for (Iterator<ApplicationInfo> i = items.iterator(); i.hasNext(); ) {
					ApplicationInfo app = i.next();
					if (!regexp.matcher(app.name == null ? "" : app.name).find()
							&& !regexp.matcher(app.packageName).find()) {
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
			mFilteredAppList = (ArrayList<ApplicationInfo>) results.values;
			adaptor.clear();
			adaptor.addAll(mFilteredAppList);
			adaptor.notifyDataSetInvalidated();
		}
	}

	private void loadApps(ProgressDialog dialog) {
		mAppList.clear();

		PackageManager pm = getPackageManager();
		List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(0);
		dialog.setMax(apps.size());
		int i = 1;
		for (ApplicationInfo appInfo : apps) {
			dialog.setProgress(i++);

			if (appInfo == null)
				continue;

			appInfo.name = appInfo.loadLabel(pm).toString();
			Intent intent = pm.getLaunchIntentForPackage(appInfo.packageName);
			if (intent != null)
				mAppList.add(appInfo);
		}

		Collections.sort(mAppList, new Comparator<ApplicationInfo>() {
			@Override
			public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
				if (lhs.name == null) {
					return -1;
				} else if (rhs.name == null) {
					return 1;
				} else {
					return lhs.name.toUpperCase(Locale.getDefault()).compareTo(rhs.name.toUpperCase(Locale.getDefault()));
				}
			}
		});
	}

	class ImageLoader extends AsyncTask<Object, Void, Drawable> {
		private ImageView imageView;
		private String mPackageName;

		public ImageLoader(ImageView view, String packageName) {
			mPackageName = packageName;
			imageView = view;
		}

		@Override
		protected Drawable doInBackground(Object... params) {
			ApplicationInfo info = (ApplicationInfo) params[0];
			return getPackageManager().getApplicationIcon(info);
		}

		@Override
		protected void onPostExecute(Drawable result) {
			super.onPostExecute(result);
			if (imageView.getTag().toString().equals(mPackageName)) {
				imageView.setImageDrawable(result);
			}
		}
	}
}
