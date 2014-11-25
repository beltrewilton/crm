package com.odoo.addons.dashboard;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.odoo.addons.calendar.model.CalendarEvent;
import com.odoo.calendar.SysCal.DateInfo;
import com.odoo.calendar.view.OdooCalendar;
import com.odoo.calendar.view.OdooCalendar.OdooCalendarDateSelectListener;
import com.odoo.crm.R;
import com.odoo.orm.ODataRow;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.fragment.OnSearchViewChangeListener;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.support.listview.OCursorListAdapter.OnViewBindListener;
import com.odoo.support.listview.OCursorListAdapter.OnViewCreateListener;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;

public class Dashboard extends BaseFragment implements
		OdooCalendarDateSelectListener, OnClickListener,
		LoaderCallbacks<Cursor>, OnViewBindListener, OnSearchViewChangeListener {

	public static final String KEY = Dashboard.class.getSimpleName();
	private OdooCalendar cal;
	private View calendarView = null;
	private ListView dashboardListView = null;
	private OCursorListAdapter mAdapter;
	private String mFilterDate;
	private String mFilter = null;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		return inflater.inflate(R.layout.dashbaord, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		cal = (OdooCalendar) view.findViewById(R.id.dashboard_calendar);
		cal.setOdooCalendarDateSelectListener(this);
		scope = new AppScope(this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_dashboard, menu);
		setHasSearchView(this, menu, R.id.menu_search);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_dashboard_goto_today:
			cal.goToToday();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public Object databaseHelper(Context context) {
		return new CalendarEvent(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		List<DrawerItem> menu = new ArrayList<DrawerItem>();
		menu.add(new DrawerItem(KEY, "Dashboard", 0,
				R.drawable.ic_action_dashboard, obj()));
		return menu;
	}

	private Fragment obj() {
		Dashboard dash = new Dashboard();
		dash.setArguments(new Bundle());
		return dash;
	}

	@Override
	public View getEventsView(ViewGroup parent, DateInfo date) {
		calendarView = LayoutInflater.from(getActivity()).inflate(
				R.layout.dashboard_items, parent, false);
		calendarView.findViewById(R.id.dashboard_no_item_view)
				.setOnClickListener(this);
		dashboardListView = (ListView) calendarView
				.findViewById(R.id.items_container);
		initAdapter();
		if (mFilterDate == null) {
			mFilterDate = date.getYear() + "-" + date.getMonth() + "-"
					+ date.getDate();
			getLoaderManager().initLoader(0, null, this);
		} else {
			mFilterDate = date.getYear() + "-" + date.getMonth() + "-"
					+ date.getDate();
			getLoaderManager().restartLoader(0, null, this);
		}
		return calendarView;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.dashboard_no_item_view:
			Toast.makeText(getActivity(),
					"TODO: Selection dialog for add event.", Toast.LENGTH_LONG)
					.show();
			break;
		}
	}

	private void initAdapter() {
		mAdapter = new OCursorListAdapter(getActivity(), null,
				R.layout.dashboard_item_view);
		mAdapter.setOnViewCreateListener(new OnViewCreateListener() {

			@Override
			public View onViewCreated(Context context, ViewGroup view,
					Cursor cr, int position) {
				String data_type = cr.getString(cr.getColumnIndex("data_type"));
				if (data_type.equals("seperator")) {
					return LayoutInflater.from(getActivity()).inflate(
							R.layout.dashboard_item_divider, view, false);
				}
				return LayoutInflater.from(getActivity()).inflate(
						R.layout.dashboard_item_view, view, false);
			}
		});
		mAdapter.setOnViewBindListener(this);
		dashboardListView.setAdapter(mAdapter);
		mAdapter.changeCursor(null);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle data) {
		String where = "(date(date_start) = ? or date(date_end) = ?) ";
		List<String> args = new ArrayList<String>();
		args.add(mFilterDate);
		args.add(mFilterDate);
		if (mFilter != null) {
			where += " and (name like ? or description like ?)";
			args.add("%" + mFilter + "%");
			args.add("%" + mFilter + "%");
		}
		CalendarEvent db = (CalendarEvent) db();
		return new CursorLoader(getActivity(), db.fullCalendarURI(),
				db.projection(), where, args.toArray(new String[args.size()]),
				null);

	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, final Cursor cr) {
		mAdapter.changeCursor(cr);
		OControls.setVisible(calendarView, R.id.dashboard_progress);
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				if (cr.getCount() > 0) {
					OControls.setGone(calendarView, R.id.dashboard_progress);
					OControls.setVisible(calendarView, R.id.items_container);
					OControls.setGone(calendarView, R.id.dashboard_no_items);
				} else {
					OControls.setGone(calendarView, R.id.dashboard_progress);
					OControls.setVisible(calendarView, R.id.dashboard_no_items);
				}
			}
		}, 300);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.changeCursor(null);
	}

	@Override
	public void onViewBind(View view, Cursor cursor, ODataRow row) {
		String type = row.getString("data_type");
		if (type.equals("seperator")) {
			OControls.setText(view, R.id.list_seperator, row.getString("name"));
		} else {
			int icon = R.drawable.ic_action_phone;
			if (type.equals("event")) {
				icon = R.drawable.ic_action_event;
			}
			if (type.equals("opportunity")) {
				icon = R.drawable.ic_action_opportunities;
			}
			OControls.setImage(view, R.id.event_icon, icon);
			OControls.setText(view, R.id.event_name, row.getString("name"));
			if (row.getString("description").equals("false")) {
				row.put("description", "");
			}
			OControls.setText(view, R.id.event_description,
					row.getString("description"));
		}
	}

	@Override
	public boolean onSearchViewTextChange(String newFilter) {
		mFilter = newFilter;
		getLoaderManager().restartLoader(0, null, this);
		return true;
	}

	@Override
	public void onSearchViewClose() {
		// Nothing to do
	}
}
