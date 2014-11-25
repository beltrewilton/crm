package com.odoo.addons.customers;

import com.odoo.crm.R;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

public class CustomerDetails extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.customer_detail);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle("");
		actionBar.setBackgroundDrawable(new ColorDrawable(Color
				.parseColor("#22000000")));
		actionBar.setHideOnContentScrollEnabled(true);
		actionBar.setHideOffset(10);

	}
}
