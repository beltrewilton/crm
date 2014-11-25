package com.odoo.addons.calendar.service;

import android.os.Bundle;

import com.odoo.addons.calendar.model.CalendarEvent;
import com.odoo.support.OUser;
import com.odoo.support.service.OSyncAdapter;
import com.odoo.support.service.OSyncService;

public class CalendarService extends OSyncService {

	@Override
	public OSyncAdapter getSyncAdapter() {
		return new OSyncAdapter(getApplicationContext(), new CalendarEvent(
				getApplicationContext()), this, true);
	}

	@Override
	public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {

	}

}
