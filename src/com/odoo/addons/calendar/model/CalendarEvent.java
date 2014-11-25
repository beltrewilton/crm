package com.odoo.addons.calendar.model;

import android.content.Context;
import android.net.Uri;

import com.odoo.addons.calendar.providers.events.CalendarEventProvider;
import com.odoo.orm.OColumn;
import com.odoo.orm.OModel;
import com.odoo.orm.OValues;
import com.odoo.orm.annotations.Odoo;
import com.odoo.orm.types.OBoolean;
import com.odoo.orm.types.ODateTime;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;
import com.odoo.support.provider.OContentProvider;
import com.odoo.util.ODate;

public class CalendarEvent extends OModel {

	Context mContext = null;
	OColumn name = new OColumn("Name", OVarchar.class, 64);
	@Odoo.api.v7
	OColumn date = new OColumn("Start Date", ODateTime.class)
			.setParsePattern(ODate.DEFAULT_FORMAT);
	@Odoo.api.v8
	@Odoo.api.v9alpha
	OColumn start_date = new OColumn("Start Date", ODateTime.class)
			.setParsePattern(ODate.DEFAULT_DATE_FORMAT);
	@Odoo.api.v7
	OColumn date_deadline = new OColumn("Dead Line", ODateTime.class)
			.setParsePattern(ODate.DEFAULT_FORMAT);
	@Odoo.api.v8
	@Odoo.api.v9alpha
	OColumn stop_date = new OColumn("Stop Date", ODateTime.class)
			.setParsePattern(ODate.DEFAULT_DATE_FORMAT);;
	OColumn duration = new OColumn("Duration", OVarchar.class, 32);
	OColumn allday = new OColumn("All Day", OBoolean.class);
	OColumn description = new OColumn("Description", OText.class);
	OColumn location = new OColumn("Location", OText.class);

	@Odoo.Functional(store = true, depends = { "date", "start_date" }, method = "storeStartDate")
	OColumn date_start = new OColumn("Start Date", ODateTime.class)
			.setLocalColumn();

	@Odoo.Functional(store = true, depends = { "date_deadline", "stop_date" }, method = "storeStopDate")
	OColumn date_end = new OColumn("Start Date", ODateTime.class)
			.setLocalColumn();

	OColumn data_type = new OColumn("Data type", OVarchar.class, 34)
			.setLocalColumn().setDefault("event");

	public CalendarEvent(Context context) {
		super(context, "calendar.event");
		mContext = context;
		if (getOdooVersion() != null) {
			int version = getOdooVersion().getVersion_number();
			if (version == 7) {
				setModelName("crm.meeting");
			}
		}
	}

	public String storeStartDate(OValues vals) {
		if (vals.contains("date")) {
			return vals.getString("date");
		}
		return vals.getString("start_date");
	}

	public String storeStopDate(OValues vals) {
		if (vals.contains("date_deadline")) {
			return vals.getString("date_deadline");
		}
		return vals.getString("stop_date");
	}

	@Override
	public OContentProvider getContentProvider() {
		return new CalendarEventProvider();
	}

	public Uri fullCalendarURI() {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority());
		uriBuilder.path(path() + "/full_calendar");
		uriBuilder.scheme("content");
		return uriBuilder.build();
	}
}
