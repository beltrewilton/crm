package com.odoo.addons.customers;

import odoo.ODomain;
import odoo.Odoo;
import odoo.controls.OForm;

import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;

import com.odoo.App;
import com.odoo.base.res.ResPartner;
import com.odoo.crm.R;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OFieldsHelper;
import com.odoo.orm.OValues;
import com.odoo.util.Base64Helper;
import com.odoo.util.OControls;

public class CustomerDetail extends ActionBarActivity {

	private Integer mId = null;
	private OForm mForm = null;
	private Boolean mEditMode = false;
	private ODataRow mRecord = null;
	private View mView = null;
	private ResPartner customer = null;
	private App app;
	private BigImageLoader imageLoader = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.customer_detail);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		mView = findViewById(R.id.parallaxScrollView);
		app = (App) getApplicationContext();
		init();
	}

	private void init() {
		initArgs();
		OControls.setVisible(mView, R.id.odooFormRes);
		mForm = (OForm) mView.findViewById(R.id.odooFormRes);
		customer = new ResPartner(this);
		if (mId != null) {
			mRecord = customer.select(mId);
			mForm.initForm(mRecord);
			OControls.setText(mView, android.R.id.title,
					mRecord.getString("name"));
			String image = mRecord.getString("large_image");
			if (!image.equals("false")) {
				OControls.setImage(mView, android.R.id.icon,
						Base64Helper.getBitmapImage(this, image));
			} else {
				image = mRecord.getString("image_small");
				if (!image.equals("false")) {
					OControls.setImage(mView, android.R.id.icon,
							Base64Helper.getBitmapImage(this, image));
				}
				if (app.inNetwork()) {
					imageLoader = new BigImageLoader();
					imageLoader.execute(mRecord.getInt("id"));
				}
			}
		} else {
			mForm.setModel(customer);
			mForm.setEditable(mEditMode);
		}
	}

	private void initArgs() {
		Bundle args = getIntent().getExtras();
		if (args != null && args.containsKey(OColumn.ROW_ID)) {
			mId = args.getInt(OColumn.ROW_ID);
		} else
			mEditMode = true;
	}

	private class BigImageLoader extends AsyncTask<Integer, Void, String> {

		private int customer_id = -1;

		@Override
		protected String doInBackground(Integer... params) {
			String image = null;
			try {
				customer_id = params[0];
				Thread.sleep(300);
				Odoo odoo = app.getOdoo();
				ODomain domain = new ODomain();
				domain.add("id", "=", params[0]);
				JSONObject result = odoo.search_read(customer.getModelName(),
						new OFieldsHelper(new String[] { "image" }).get(),
						domain.get());
				JSONObject records = result.getJSONArray("records")
						.getJSONObject(0);
				if (!records.getString("image").equals("false")) {
					image = records.getString("image");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return image;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (result != null) {
				OValues vals = new OValues();
				vals.put("large_image", result);
				customer.resolver().update(customer.selectRowId(customer_id),
						vals);
				if (!result.equals("false")) {
					OControls.setImage(mView, android.R.id.icon, Base64Helper
							.getBitmapImage(CustomerDetail.this, result));
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (imageLoader != null) {
			imageLoader.cancel(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		}
		return true;
	}
}
