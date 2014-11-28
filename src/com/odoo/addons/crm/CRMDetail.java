package com.odoo.addons.crm;

import java.util.List;

import odoo.OArguments;
import odoo.ODomain;
import odoo.Odoo;
import odoo.controls.OField;
import odoo.controls.OForm;
import odoo.controls.OSearchableMany2One.DialogListRowViewListener;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.odoo.App;
import com.odoo.addons.crm.CRM.Keys;
import com.odoo.addons.crm.model.CRMLead;
import com.odoo.base.res.ResUsers;
import com.odoo.crm.R;
import com.odoo.orm.OColumn;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OValues;
import com.odoo.support.ODialog;
import com.odoo.support.OUser;
import com.odoo.util.ODate;

public class CRMDetail extends ActionBarActivity implements OnClickListener,
		DialogListRowViewListener {

	private ActionBar actionBar;
	private Keys mKey = null;
	private Integer mId = null;
	private Menu mMenu = null;
	private Context mContext = null;
	private Boolean mEditMode = true;
	private OForm mForm = null;
	private ODataRow mRecord = null;
	private Bundle arg = null;
	private App app = null;
	private OUser user = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.crm_detail_view);
		actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setBackgroundDrawable(new ColorDrawable(getResources()
				.getColor(R.color.theme_primary)));
		actionBar.setHomeAsUpIndicator(R.drawable.ic_action_mark_undone);
		mContext = this;
		app = (App) mContext.getApplicationContext();
		user = OUser.current(mContext);
		initArgs();
		init();
	}

	private void initArgs() {
		arg = getIntent().getExtras();
		if (arg != null) {
			mKey = Keys.valueOf(arg.getString(CRM.KEY_CRM_LEAD_TYPE));
			actionBar.setTitle((mKey == Keys.Leads) ? "Lead" : "Opportunity");
			if (arg.containsKey(OColumn.ROW_ID)) {
				mId = arg.getInt(OColumn.ROW_ID);
			}
		}
	}

	private void init() {
		switch (mKey) {
		case Leads:
			findViewById(R.id.crmLeadDetail).setVisibility(View.VISIBLE);
			mForm = (OForm) findViewById(R.id.crmLeadDetail);
			if (mId != null) {
				mForm.findViewById(R.id.btnConvertToOpportunity)
						.setOnClickListener(this);
				mForm.findViewById(R.id.btnCancelCase).setOnClickListener(this);
				mForm.findViewById(R.id.btnLeadReset).setOnClickListener(this);
			} else {
				mForm.findViewById(R.id.btnLayout).setVisibility(View.GONE);
			}
			OField partner_id = (OField) mForm.findViewById(R.id.partner_id);
			partner_id.setManyToOneSearchableCallbacks(this);
			break;
		case Opportunities:
			findViewById(R.id.crmOppDetail).setVisibility(View.VISIBLE);
			mForm = (OForm) findViewById(R.id.crmOppDetail);
			mForm.findViewById(R.id.btnConvertToQuotation).setOnClickListener(
					this);
			partner_id = (OField) mForm.findViewById(R.id.partner_id_opp);
			partner_id.setManyToOneSearchableCallbacks(this);
			break;
		}
		CRMLead crmLead = new CRMLead(mContext);
		if (mId != null) {
			mRecord = crmLead.select(mId);
			mForm.initForm(mRecord);
		} else {
			mForm.setModel(crmLead);
		}
		mForm.setEditable(mEditMode);
	}

	private void updateMenu(boolean edit_mode) {
		mMenu.findItem(R.id.menu_crm_detail_save).setVisible(edit_mode);
		mMenu.findItem(R.id.menu_crm_detail_edit).setVisible(!edit_mode);
		if (mId == null) {
			mMenu.findItem(R.id.menu_crm_detail_delete).setVisible(false);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		getMenuInflater().inflate(R.menu.menu_crm_detail, menu);
		mMenu = menu;
		updateMenu(mEditMode);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.menu_crm_detail_edit:
			mEditMode = !mEditMode;
			updateMenu(mEditMode);
			mForm.setEditable(mEditMode);
			break;
		case R.id.menu_crm_detail_save:
			mEditMode = false;
			OValues values = mForm.getFormValues();
			if (values != null) {
				updateMenu(mEditMode);
				if (mId != null) {
					switch (mKey) {
					case Leads:
						new CRMLead(mContext).update(values, mId);
						break;
					case Opportunities:
						new CRMLead(mContext).update(values, mId);
						break;
					}
				} else {
					ResUsers users = new ResUsers(mContext);
					Integer user_id = users.selectRowId(user.getUser_id());
					if (user_id == null) {
						OValues userVals = new OValues();
						userVals.put("id", user.getUser_id());
						user_id = users.create(userVals);
					}
					values.put("user_id", user_id);
					values.put("assignee_name", "Me");
					CRMLead.CRMCaseStage stages = new CRMLead.CRMCaseStage(
							mContext);
					List<ODataRow> stage = stages.select(
							"type = ? and name = ?", new String[] { "both",
									"New" });
					if (stage.size() > 0) {
						values.put("stage_id",
								stage.get(0).getInt(OColumn.ROW_ID));
					}
					values.put("create_date",
							ODate.getUTCDate(ODate.DEFAULT_FORMAT));
					switch (mKey) {
					case Leads:
						values.put("type", "lead");
						new CRMLead(mContext).create(values);
						break;
					case Opportunities:
						values.put("type", "opportunity");
						new CRMLead(mContext).create(values);
						break;
					}
				}
				finish();
			}
			break;
		case R.id.menu_crm_detail_delete:
			new CRMLead(mContext).delete(mId);
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		Bundle bundle = new Bundle();
		switch (v.getId()) {
		case R.id.btnConvertToOpportunity:
			CRMConvertToOpp convertToOpportunity = new CRMConvertToOpp();
			if (arg.getInt("id") != 0) {
				if (app.inNetwork()) {
					bundle.putInt("lead_id", mId);
					bundle.putInt("index",
							getIntent().getExtras().getInt("index"));
					convertToOpportunity.setArguments(bundle);
					// startFragment(convertToOpportunity, true);
				} else {
					Toast.makeText(mContext,
							getString(R.string.toast_no_netowrk),
							Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(mContext, getString(R.string.toast_sync_before),
						Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.btnConvertToQuotation:
			if (mRecord.getM2ORecord("partner_id").browse() != null) {
				ConvertToQuotation converToQuotation = new ConvertToQuotation(
						mRecord);
				converToQuotation.execute();
			}
		default:
			break;
		}
	}

	class ConvertToQuotation extends AsyncTask<Void, Void, Void> {
		ODataRow mLead = null;
		ODialog mDialog = null;
		String mToast = null;

		public ConvertToQuotation(ODataRow lead) {
			mLead = lead;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mDialog = new ODialog(mContext, false, "Converting....");
			mDialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					try {
						Odoo odoo = app.getOdoo();
						int version = odoo.getOdooVersion().getVersion_number();
						OArguments args = new OArguments();
						JSONArray fields = new JSONArray();
						fields.put("close");
						if (version == 7)
							fields.put("shop_id");
						fields.put("partner_id");
						args.add(fields);
						JSONObject kwargs = new JSONObject();
						JSONObject context = new JSONObject();
						if (version == 7)
							context.put("stage_type", "opportunity");
						context.put("active_model", "crm.lead");
						context.put("active_id", mLead.getInt("id"));
						context.put("active_ids",
								new JSONArray().put(mLead.getInt("id")));
						kwargs.put("context", context);
						JSONObject response = (JSONObject) odoo.call_kw(
								"crm.make.sale", "default_get",
								new JSONArray().put(fields), kwargs);
						JSONObject result = response.getJSONObject("result");
						JSONObject arguments = new JSONObject();
						arguments.put("partner_id", result.get("partner_id"));
						if (version == 7)
							arguments.put("shop_id", result.get("shop_id"));
						arguments.put("close", false);
						JSONObject newContext = odoo.updateContext(context);
						JSONObject res = odoo.createNew("crm.make.sale",
								arguments);
						int id = res.getInt("result");

						// makeOrder
						OArguments make_order_args = new OArguments();
						make_order_args.add(id);
						make_order_args.add(newContext);
						JSONObject order = odoo.call_kw("crm.make.sale",
								"makeOrder", make_order_args.get());
						if (order.getJSONObject("result").has("res_id")) {
							mToast = "Quotation SO"
									+ order.getJSONObject("result").getInt(
											"res_id") + " created";
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			return null;
		}
	}

	@Override
	public View onDialogListRowGetView(ODataRow data, int position, View view,
			ViewGroup parent) {
		return null;
	}

	@Override
	public ODomain onDialogSearchChange(String filter) {
		ODomain domain = new ODomain();
		domain.add("name", "=ilike", filter + "%");
		return domain;
	}

	@Override
	public void bindDisplayLayoutLoad(ODataRow data, View layout) {
		if (data != null) {
			TextView txvName = (TextView) layout;
			txvName.setText(data.getString("name"));
		}
	}
}
