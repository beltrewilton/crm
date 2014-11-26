package com.odoo.addons.dashboard;

import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import com.odoo.crm.R;

public class TodayIcon {

	private Context mContext;
	private Resources mRes;
	private TextPaint mPaint = new TextPaint();
	private Rect mBounds = new Rect();
	private Canvas mCanvas = new Canvas();
	private Bitmap mDefaultIcon;

	public TodayIcon(Context context) {
		mContext = context;
		mRes = mContext.getResources();
	}

	private int date() {
		return Calendar.getInstance(Locale.getDefault()).get(
				Calendar.DAY_OF_MONTH);
	}

	public static TodayIcon get(Context context) {
		return new TodayIcon(context);
	}

	public Drawable getIcon() {
		mPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
		mPaint.setColor(Color.WHITE);
		mPaint.setTextAlign(Align.CENTER);
		mPaint.setAntiAlias(true);
		mPaint.setTextSize(mRes.getDimension(R.dimen.text_size_xxsmall));
		mDefaultIcon = BitmapFactory.decodeResource(mRes,
				R.drawable.ic_action_goto_today);
		Bitmap bmp = generate(mDefaultIcon.getWidth(), mDefaultIcon.getHeight());
		return new BitmapDrawable(mRes, bmp);
	}

	private Bitmap generate(int width, int height) {
		final String date = (date() < 10) ? "0" + date() + "" : date() + "";
		final Canvas c = mCanvas;
		final Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		c.setBitmap(bitmap);
		c.drawBitmap(mDefaultIcon, 0, 0, null);
		c.drawColor(Color.TRANSPARENT);
		mPaint.getTextBounds(date, 0, 2, mBounds);
		c.drawText(date, 0, 2, width / 2, 7 + height / 2
				+ (mBounds.bottom - mBounds.top) / 2, mPaint);
		return bitmap;
	}
}
