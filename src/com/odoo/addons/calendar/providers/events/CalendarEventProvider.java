package com.odoo.addons.calendar.providers.events;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;

import com.odoo.addons.calendar.model.CalendarEvent;
import com.odoo.addons.crm.CRM;
import com.odoo.addons.crm.model.CRMLead;
import com.odoo.addons.crm.model.CRMPhoneCall;
import com.odoo.orm.OColumn;
import com.odoo.orm.OModel;
import com.odoo.support.provider.OContentProvider;

public class CalendarEventProvider extends OContentProvider {
	public static String AUTHORITY = "com.odoo.addons.calendar.providers.events";
	public static final String PATH = "calendar_events";
	public static final Uri CONTENT_URI = OContentProvider.buildURI(AUTHORITY,
			PATH);
	public static final int FULL_CALENDAR = 114;

	@Override
	public OModel model(Context context) {
		return new CalendarEvent(context);
	}

	@Override
	public String authority() {
		return CalendarEventProvider.AUTHORITY;
	}

	@Override
	public String path() {
		return CalendarEventProvider.PATH;
	}

	@Override
	public Uri uri() {
		return CalendarEventProvider.CONTENT_URI;
	}

	@Override
	public boolean onCreate() {
		boolean state = super.onCreate();
		addURI(AUTHORITY, PATH + "/full_calendar", FULL_CALENDAR);
		return state;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sort) {
		int code = matchURI(uri);
		if (code == FULL_CALENDAR) {
			uri = getModel().uri();
			MatrixCursor event_seperator = new MatrixCursor(new String[] {
					OColumn.ROW_ID, "data_type", "name" });
			Cursor events = super.query(uri, projection, selection,
					selectionArgs, sort);
			if (events.getCount() > 0)
				event_seperator.addRow(new String[] { "0", "seperator",
						"Events" });

			// Getting Phone Calls
			CRMPhoneCall phone = new CRMPhoneCall(getContext());
			String phone_whr = "date(date) = ?";
			List<String> phone_args = new ArrayList<String>();
			phone_args.add(selectionArgs[0]);
			if (selectionArgs.length > 2) {
				phone_whr += " and (name like ? or description like ?)";
				phone_args.add(selectionArgs[2]);
				phone_args.add(selectionArgs[2]);
			}
			MatrixCursor phone_seperator = new MatrixCursor(new String[] {
					OColumn.ROW_ID, "data_type", "name" });
			Cursor phone_calls = getContext().getContentResolver().query(
					phone.uri(), phone.projection(), phone_whr,
					phone_args.toArray(new String[phone_args.size()]), null);
			if (phone_calls.getCount() > 0) {
				phone_seperator.addRow(new String[] { "0", "seperator",
						"Phone Calls" });
			}

			// Getting opportunities
			CRMLead crm = new CRMLead(getContext());
			MatrixCursor opp_seperator = new MatrixCursor(new String[] {
					OColumn.ROW_ID, "data_type", "name" });
			String crm_whr = "(date(create_date) = ? or date(date_action) = ?) and type = ?";
			List<String> crm_args = new ArrayList<String>();
			crm_args.add(selectionArgs[0]);
			crm_args.add(selectionArgs[0]);
			crm_args.add(CRM.KEY_OPPORTUNITY);
			if (selectionArgs.length > 2) {
				crm_whr += " and (name like ? or description like ?)";
				crm_args.add(selectionArgs[2]);
				crm_args.add(selectionArgs[2]);
			}
			crm_whr += " and type = ?";
			crm_args.add("opportunity");
			Cursor crm_datas = getContext().getContentResolver().query(
					crm.uri(), crm.projection(), crm_whr,
					crm_args.toArray(new String[crm_args.size()]), null);
			if (crm_datas.getCount() > 0) {
				opp_seperator.addRow(new String[] { "0", "seperator",
						"Opportunities" });
			}
			// Merging cursors
			MergeCursor mergedData = new MergeCursor(new Cursor[] {
					event_seperator, events, phone_seperator, phone_calls,
					opp_seperator, crm_datas });
			return mergedData;
		}
		return super.query(uri, projection, selection, selectionArgs, sort);
	}
}
