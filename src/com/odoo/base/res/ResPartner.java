/*
 * Odoo, Open Source Management Solution
 * Copyright (C) 2012-today Odoo SA (<http:www.odoo.com>)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * 
 */

package com.odoo.base.res;

import org.json.JSONArray;

import android.content.Context;
import android.database.Cursor;

import com.odoo.base.res.providers.partners.PartnersProvider;
import com.odoo.orm.OColumn;
import com.odoo.orm.OColumn.RelationType;
import com.odoo.orm.ODataRow;
import com.odoo.orm.OModel;
import com.odoo.orm.OValues;
import com.odoo.orm.annotations.Odoo;
import com.odoo.orm.types.OBlob;
import com.odoo.orm.types.OBoolean;
import com.odoo.orm.types.OText;
import com.odoo.orm.types.OVarchar;
import com.odoo.support.provider.OContentProvider;
import com.odoo.util.CursorUtils;

/**
 * The Class ResPartner.
 */
public class ResPartner extends OModel {
	Context mContext = null;

	OColumn name = new OColumn("Name", OText.class);
	OColumn is_company = new OColumn("Is Company", OBoolean.class)
			.setDefault(false);
	OColumn image_small = new OColumn("Image", OBlob.class).setDefault(false);
	OColumn large_image = new OColumn("Large Image", OBlob.class).setDefault(
			false).setLocalColumn();
	OColumn street = new OColumn("Street", OText.class);
	OColumn street2 = new OColumn("Street2", OText.class);
	OColumn city = new OColumn("City", OText.class);
	OColumn zip = new OColumn("Zip", OVarchar.class, 10);
	OColumn website = new OColumn("Website", OText.class);
	OColumn phone = new OColumn("Phone", OText.class);
	OColumn mobile = new OColumn("Mobile", OText.class);
	OColumn email = new OColumn("Email", OText.class);
	OColumn company_id = new OColumn("Company", ResCompany.class,
			RelationType.ManyToOne).addDomain("is_company", "=", true);
	OColumn parent_id = new OColumn("Related Company", ResPartner.class,
			RelationType.ManyToOne);
	OColumn country_id = new OColumn("Country", ResCountry.class,
			RelationType.ManyToOne);
	@Odoo.Functional(store = true, depends = { "parent_id" }, method = "storeCompanyName")
	OColumn company_name = new OColumn("Compnay Name", OVarchar.class, 100)
			.setLocalColumn();

	public ResPartner(Context context) {
		super(context, "res.partner");
		mContext = context;
	}

	public String storeCompanyName(OValues vals) {
		try {
			if (!vals.getString("parent_id").equals("false")) {
				JSONArray parent_id = new JSONArray(vals.getString("parent_id"));
				return parent_id.getString(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getAddress(Cursor cr) {
		String add = "";
		ODataRow row = CursorUtils.toDataRow(cr);
		if (!row.getString("street").equals("false"))
			add += row.getString("street") + ", ";
		if (!row.getString("street2").equals("false"))
			add += add + "\n" + row.getString("street2") + ", ";
		if (!row.getString("city").equals("false"))
			add += add + row.getString("city") + " - ";
		if (!row.getString("zip").equals("false"))
			add += add + row.getString("zip") + " ";
		return add;
	}

	public String getContact(int row_id) {
		ODataRow row = select(row_id);
		if (row != null) {
			if (!row.getString("phone").equals("false")) {
				return row.getString("phone");
			}
			if (!row.getString("mobile").equals("false")) {
				return row.getString("mobile");
			}
		}
		return null;
	}

	@Override
	public OContentProvider getContentProvider() {
		return new PartnersProvider();
	}

}
