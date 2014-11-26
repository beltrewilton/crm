package com.odoo.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class MapUtil {

	public static void redirectToMap(Context context, String location) {
		String map = "geo:0,0?q=" + location;
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(map));
		context.startActivity(intent);
	}
}
