package com.odoo.addons.dashboard;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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
import com.odoo.util.MapUtil;
import com.odoo.util.OControls;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.widgets.bottomsheet.BottomSheet;
import com.odoo.widgets.bottomsheet.BottomSheet.Builder;
import com.odoo.widgets.bottomsheet.BottomSheetListeners.OnSheetActionClickListener;
import com.odoo.widgets.bottomsheet.BottomSheetListeners.OnSheetItemClickListener;

public class Dashboard extends BaseFragment implements
		OdooCalendarDateSelectListener, OnClickListener,
		LoaderCallbacks<Cursor>, OnViewBindListener,
		OnSearchViewChangeListener, OnItemClickListener,
		OnSheetItemClickListener, OnSheetActionClickListener {

	public static final String KEY = Dashboard.class.getSimpleName();
	private OdooCalendar cal;
	private View calendarView = null;
	private ListView dashboardListView = null;
	private OCursorListAdapter mAdapter;
	private String mFilterDate;
	private String mFilter = null;
	private BottomSheet mSheet = null;

	private enum DialogType {
		Event, PhoneCall, Opportunity
	}

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
		scope.main().setOnBackPressCallBack(this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_dashboard, menu);
		MenuItem today = menu.findItem(R.id.menu_dashboard_goto_today);
		today.setIcon(TodayIcon.get(getActivity()).getIcon());
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
			getLoaderManager().initLoader(0, null, Dashboard.this);
		} else {
			mFilterDate = date.getYear() + "-" + date.getMonth() + "-"
					+ date.getDate();
			getLoaderManager().restartLoader(0, null, Dashboard.this);
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
		dashboardListView.setOnItemClickListener(this);
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

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		getLoaderManager().destroyLoader(0);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Cursor cr = mAdapter.getCursor();
		cr.moveToPosition(position);
		String data_type = cr.getString(cr.getColumnIndex("data_type"));
		if (!data_type.equals("seperator")) {
			if (data_type.equals("phone_call")) {
				createDialog(DialogType.PhoneCall, cr);
			}
			if (data_type.equals("event")) {
				createDialog(DialogType.Event, cr);
			}
			if (data_type.equals("opportunity")) {
				createDialog(DialogType.Opportunity, cr);
			}
		}
	}

	private void createDialog(DialogType type, Object data) {
		if (mSheet != null) {
			mSheet.dismiss();
		}
		Cursor cr = (Cursor) data;
		// Creating sheet object
		BottomSheet.Builder builder = new Builder(getActivity());
		builder.listener(this);
		builder.setIconColor(_c(R.color.theme_primary_dark));
		builder.setTextColor(Color.parseColor("#414141"));
		builder.setData(data);
		builder.actionListener(this);
		builder.setActionIcon(R.drawable.ic_action_edit);
		builder.title(cr.getString(cr.getColumnIndex("name")));
		switch (type) {
		case PhoneCall:
			builder.menu(R.menu.menu_dashboard_phonecall);
			break;
		case Event:
			builder.menu(R.menu.menu_dashboard_events);
			break;
		case Opportunity:
			builder.menu(R.menu.menu_dashboard_opportunity);
			break;
		}
		mSheet = builder.create();
		mSheet.show();
	}

	@Override
	public void onItemClick(BottomSheet sheet, MenuItem menu, Object extra) {
		dismissSheet(sheet);
		Cursor cr = (Cursor) extra;
		switch (menu.getItemId()) {
		// Phone call menus
		case R.id.menu_phonecall_call:
			break;
		case R.id.menu_phonecall_reschedule:
			break;
		case R.id.menu_phonecall_all_done:
			break;
		// Event menus
		case R.id.menu_events_location:
			String location = cr.getString(cr.getColumnIndex("location"));
			if (location.equals("false")) {
				Toast.makeText(getActivity(), "No location found !",
						Toast.LENGTH_LONG).show();
			} else {
				MapUtil.redirectToMap(getActivity(), location);
			}
			break;
		case R.id.menu_events_reschedule:
			break;
		case R.id.menu_events_all_done:
			break;
		// Opportunity menus
		case R.id.menu_opp_customer_location:
			String address = cr.getString(cr.getColumnIndex("street")) + ", ";
			address += cr.getString(cr.getColumnIndex("street2")) + ", ";
			address += cr.getString(cr.getColumnIndex("city")) + ", ";
			address += cr.getString(cr.getColumnIndex("zip"));
			address = address.replaceAll("false", "");
			if (TextUtils.isEmpty(address)) {
				Toast.makeText(getActivity(), "No location found !",
						Toast.LENGTH_LONG).show();
			} else {
				MapUtil.redirectToMap(getActivity(), address);
			}
			break;
		case R.id.menu_opp_call_customer:
			break;
		case R.id.menu_opp_lost:
			break;
		case R.id.menu_opp_won:
			break;
		case R.id.menu_opp_reschedule:
			break;
		}
	}

	private void dismissSheet(final BottomSheet sheet) {
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				sheet.dismiss();
			}
		}, 100);
	}

	@Override
	public boolean onBackPressed() {
		if (mSheet != null && mSheet.isShowing()) {
			mSheet.dismiss();
			return false;
		}
		return true;
	}

	@Override
	public void onSheetActionClick(BottomSheet sheet, Object extras) {
		dismissSheet(sheet);
		Cursor cr = (Cursor) extras;
		sheet.dismiss();
	}
}
