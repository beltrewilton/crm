package com.odoo.addons.dashboard;

import java.util.ArrayList;
import java.util.List;

import odoo.controls.fab.FloatingActionButton;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
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

import com.odoo.addons.calendar.CalendarDetail;
import com.odoo.addons.calendar.model.CalendarEvent;
import com.odoo.addons.calendar.providers.events.CalendarEventProvider;
import com.odoo.addons.crm.CRM;
import com.odoo.addons.crm.CRMDetail;
import com.odoo.addons.crm.CRMDetailFrag;
import com.odoo.addons.crm.model.CRMPhoneCall;
import com.odoo.addons.customers.CustomerDetail;
import com.odoo.base.res.ResPartner;
import com.odoo.calendar.SysCal.DateInfo;
import com.odoo.calendar.view.OdooCalendar;
import com.odoo.calendar.view.OdooCalendar.OdooCalendarDateSelectListener;
import com.odoo.crm.R;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.support.AppScope;
import com.odoo.support.fragment.BaseFragment;
import com.odoo.support.fragment.OnSearchViewChangeListener;
import com.odoo.support.fragment.SyncStatusObserverListener;
import com.odoo.support.listview.OCursorListAdapter;
import com.odoo.support.listview.OCursorListAdapter.OnViewBindListener;
import com.odoo.support.listview.OCursorListAdapter.OnViewCreateListener;
import com.odoo.util.IntentUtil;
import com.odoo.util.OControls;
import com.odoo.util.ODate;
import com.odoo.util.drawer.DrawerItem;
import com.odoo.widgets.bottomsheet.BottomSheet;
import com.odoo.widgets.bottomsheet.BottomSheet.Builder;
import com.odoo.widgets.bottomsheet.BottomSheetListeners.OnSheetActionClickListener;
import com.odoo.widgets.bottomsheet.BottomSheetListeners.OnSheetItemClickListener;
import com.odoo.widgets.bottomsheet.BottomSheetListeners.OnSheetMenuCreateListener;
import com.odoo.widgets.snackbar.SnackBar;
import com.odoo.widgets.snackbar.SnackbarBuilder;
import com.odoo.widgets.snackbar.SnackbarBuilder.SnackbarDuration;
import com.odoo.widgets.snackbar.listeners.ActionClickListener;

public class Dashboard extends BaseFragment implements
		OdooCalendarDateSelectListener, OnClickListener,
		LoaderCallbacks<Cursor>, OnViewBindListener,
		OnSearchViewChangeListener, OnItemClickListener,
		OnSheetItemClickListener, OnSheetActionClickListener,
		SyncStatusObserverListener, OnRefreshListener,
		OnSheetMenuCreateListener {

	public static final String KEY = Dashboard.class.getSimpleName();
	private OdooCalendar cal;
	private View calendarView = null;
	private ListView dashboardListView = null;
	private OCursorListAdapter mAdapter;
	private String mFilterDate;
	private String mFilter = null;
	private BottomSheet mSheet = null;
	private FloatingActionButton mFab;
	private Boolean syncRequested = false;

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
		mFab = (FloatingActionButton) view.findViewById(R.id.fabbutton);
		mFab.setOnClickListener(this);
		snackYouAreOffLine();
		setHasSyncStatusObserver(KEY, this, db());
	}

	private void snackYouAreOffLine() {
		if (!inNetwork()) {
			SnackBar.get(getActivity()).text(R.string.toast_you_are_offline)
					.duration(SnackbarDuration.LENGTH_LONG).show();
			hideFAB(4000);
		}
	}

	private void hideFAB(int delay) {
		mFab.setVisibility(View.GONE);
		postDelayed(new Runnable() {

			@Override
			public void run() {
				mFab.setVisibility(View.VISIBLE);
			}
		}, delay);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.menu_dashboard, menu);
		if (getActivity() != null) {
			MenuItem today = menu.findItem(R.id.menu_dashboard_goto_today);
			today.setIcon(TodayIcon.get(getActivity()).getIcon());
		}
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
		menu.add(new DrawerItem(KEY, "Agenda", 0,
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
		mFab.listenTo(dashboardListView);
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
		case R.id.fabbutton:
			BottomSheet.Builder builder = new Builder(getActivity());
			builder.listener(this);
			builder.setIconColor(_c(R.color.theme_secondary_dark));
			builder.setTextColor(Color.parseColor("#414141"));
			ODataRow data = new ODataRow();
			data.put("fab", true);
			builder.setData(data);
			builder.title(_s(R.string.label_new));
			builder.menu(R.menu.menu_dashboard_fab);
			mSheet = builder.create();
			mSheet.show();
			break;
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
					setHasSwipeRefreshView(calendarView, R.id.swipe_container,
							Dashboard.this);
				} else {
					setHasSwipeRefreshView(calendarView,
							R.id.dashboard_no_items, Dashboard.this);
					if (db().isEmptyTable() && !syncRequested) {
						syncRequested = true;
						scope.main().requestSync(
								CalendarEventProvider.AUTHORITY);
					}
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
			String date = "false";
			if (type.equals("phone_call")) {
				date = row.getString("date");
			}
			if (type.equals("event")) {
				icon = R.drawable.ic_action_event;
				if (row.getString("allday").equals("false"))
					date = row.getString("date_start");
			}
			if (type.equals("opportunity")) {
				icon = R.drawable.ic_action_opportunities;
				date = row.getString("date_action");
			}
			OControls.setImage(view, R.id.event_icon, icon);
			OControls.setText(view, R.id.event_name, row.getString("name"));
			if (row.getString("description").equals("false")) {
				row.put("description", "");
			}
			if (!date.equals("false")) {
				date = ODate.getFormattedDate(getActivity(), date, "HH:mm a");
				OControls.setText(view, R.id.event_time, date);
			}
			OControls.setText(view, R.id.event_description,
					row.getString("description"));

			Boolean is_done = row.getString("is_done").equals("1");
			if (is_done) {
				int title_color = (is_done) ? Color.GRAY : Color
						.parseColor("#414141");
				int time_color = (is_done) ? Color.GRAY
						: _c(R.color.theme_secondary_light);
				int desc_color = (is_done) ? Color.GRAY : Color
						.parseColor("#aaaaaa");
				view.findViewById(R.id.event_icon).setBackgroundResource(
						R.drawable.circle_mask);
				OControls.setTextColor(view, R.id.event_name, title_color);
				OControls.setTextColor(view, R.id.event_time, time_color);
				OControls
						.setTextColor(view, R.id.event_description, desc_color);
				OControls.setTextViewStrikeThrough(view, R.id.event_name);
				OControls.setTextViewStrikeThrough(view, R.id.event_time);
				OControls
						.setTextViewStrikeThrough(view, R.id.event_description);

			}
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
		builder.setOnSheetMenuCreateListener(this);
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
		if (extra instanceof ODataRow) {
			switch (menu.getItemId()) {
			case R.id.menu_fab_new_event:
				startActivity(new Intent(getActivity(), CalendarDetail.class));
				break;
			case R.id.menu_fab_new_customer:
				startActivity(new Intent(getActivity(), CustomerDetail.class));
				break;
			case R.id.menu_fab_new_lead:
				Intent lead = new Intent(getActivity(), CRMDetail.class);
				lead.putExtra(CRM.KEY_CRM_LEAD_TYPE, CRM.Keys.Leads.toString());
				startActivity(lead);
				break;
			}

			// fab items
			return;
		}
		Cursor cr = (Cursor) extra;
		String is_done = cr.getString(cr.getColumnIndex("is_done"));
		final OValues vals = new OValues();
		vals.put("is_done", (is_done.equals("0")) ? 1 : 0);
		String done_lable = (is_done.equals("0")) ? "done" : "undone";
		final int row_id = cr.getInt(cr.getColumnIndex(OColumn.ROW_ID));
		switch (menu.getItemId()) {
		case R.id.menu_phonecall_reschedule:
			break;
		// Event menus
		case R.id.menu_events_location:
			String location = cr.getString(cr.getColumnIndex("location"));
			if (location.equals("false")) {
				Toast.makeText(getActivity(), "No location found !",
						Toast.LENGTH_LONG).show();
			} else {
				IntentUtil.redirectToMap(getActivity(), location);
			}
			break;
		case R.id.menu_events_reschedule:
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
				IntentUtil.redirectToMap(getActivity(), address);
			}
			break;
		case R.id.menu_opp_call_customer:
		case R.id.menu_phonecall_call:
			int partner_id = cr.getInt(cr.getColumnIndex("partner_id"));
			if (partner_id != 0) {
				ResPartner pDb = new ResPartner(getActivity());
				String contact = pDb.getContact(partner_id);
				if (contact != null) {
					IntentUtil.callIntent(getActivity(), contact);
				} else {
					Toast.makeText(getActivity(), "No contact found.",
							Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(getActivity(), "No contact found.",
						Toast.LENGTH_LONG).show();
			}
			break;
		case R.id.menu_opp_lost:
			break;
		case R.id.menu_opp_won:
			break;
		case R.id.menu_opp_reschedule:
			break;

		// All done menu
		case R.id.menu_phonecall_all_done:
			final CRMPhoneCall phone_call = new CRMPhoneCall(getActivity());
			phone_call.resolver().update(row_id, vals);
			getLoaderManager().restartLoader(0, null, this);
			SnackBar.get(getActivity()).text("Phone call marked " + done_lable)
					.actionColor(_c(R.color.theme_primary_light))
					.duration(SnackbarDuration.LENGTH_TOO_LONG)
					.withAction("undo", new ActionClickListener() {

						@Override
						public void onActionClicked(SnackbarBuilder snackbar) {
							vals.put("is_done", (vals.getString("is_done")
									.equals("0")) ? 1 : 0);
							mFab.setVisibility(View.VISIBLE);
							phone_call.resolver().update(row_id, vals);
							getLoaderManager().restartLoader(0, null,
									Dashboard.this);
						}
					}).show();
			hideFAB(6000);
			break;
		case R.id.menu_events_all_done:
			db().resolver().update(row_id, vals);
			getLoaderManager().restartLoader(0, null, this);
			SnackBar.get(getActivity()).text("Event marked " + done_lable)
					.actionColor(_c(R.color.theme_primary_light))
					.duration(SnackbarDuration.LENGTH_TOO_LONG)
					.withAction("undo", new ActionClickListener() {

						@Override
						public void onActionClicked(SnackbarBuilder snackbar) {
							mFab.setVisibility(View.VISIBLE);
							vals.put("is_done", (vals.getString("is_done")
									.equals("0")) ? 1 : 0);
							db().resolver().update(row_id, vals);
							getLoaderManager().restartLoader(0, null,
									Dashboard.this);
						}
					}).show();
			hideFAB(6000);
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
	public void onSheetActionClick(BottomSheet sheet, final Object extras) {
		dismissSheet(sheet);
		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				Cursor cr = (Cursor) extras;
				String data_type = cr.getString(cr.getColumnIndex("data_type"));
				int record_id = cr.getInt(cr.getColumnIndex(OColumn.ROW_ID));
				if (data_type.equals("phone_call")) {
				}
				if (data_type.equals("event")) {
				}
				if (data_type.equals("opportunity")) {
					CRMDetailFrag crmDetail = new CRMDetailFrag();
					Bundle bundle = new Bundle();
					bundle.putInt(OColumn.ROW_ID, record_id);
					bundle.putString(CRM.KEY_CRM_LEAD_TYPE,
							CRM.Keys.Opportunities.toString());
					crmDetail.setArguments(bundle);
					startFragment(crmDetail, true);
				}
			}
		}, 250);

	}

	@Override
	public void onResume() {
		super.onResume();
		if (dashboardListView != null)
			onStart();
	}

	@Override
	public void onStart() {
		super.onStart();
		if (dashboardListView != null) {
			getLoaderManager().destroyLoader(0);
			getLoaderManager().restartLoader(0, null, this);
		}
	}

	@Override
	public void onRefresh() {
		snackYouAreOffLine();
		if (inNetwork()) {
			scope.main().requestSync(CalendarEventProvider.AUTHORITY);
		}
	}

	@Override
	public void onStatusChange(Boolean refreshing) {
		if (!refreshing)
			hideRefreshingProgress();
		else
			setSwipeRefreshing(true);
	}

	@Override
	public void onSheetMenuCreate(Menu menu, Object extras) {
		Cursor cr = (Cursor) extras;
		String type = cr.getString(cr.getColumnIndex("data_type"));
		String is_done = cr.getString(cr.getColumnIndex("is_done"));
		if (is_done.equals("0"))
			return;
		MenuItem mark_done = null;
		if (type.equals("event")) {
			mark_done = menu.findItem(R.id.menu_events_all_done);
		}
		if (type.equals("phone_call")) {
			mark_done = menu.findItem(R.id.menu_phonecall_all_done);
		}
		if (mark_done != null) {
			mark_done.setTitle("Mark Undone");
			mark_done.setIcon(R.drawable.ic_action_mark_undone);
		}
	}
}
