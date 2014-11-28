package com.odoo.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class IntentUtil {

	public static void redirectToMap(Context context, String location) {
		String map = "geo:0,0?q=" + location;
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(map));
		context.startActivity(intent);
	}

	public static void callIntent(Context context, String contact) {
		Intent callIntent = new Intent(Intent.ACTION_CALL);
		callIntent.setData(Uri.parse("tel:" + contact));
		context.startActivity(callIntent);
	}
}
