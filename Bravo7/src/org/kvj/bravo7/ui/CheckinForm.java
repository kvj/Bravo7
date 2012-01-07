package org.kvj.bravo7.ui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.R;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TimePicker;

public class CheckinForm extends SuperActivity {

	private static final String TAG = "CheckinForm";
	private JSONObject template = null;
	private JSONObject editor = null;
	private DatePicker datePicker = null;
	private TimePicker timePicker = null;
	private Button addButton = null;
	private boolean needLocation = false;
	Map<String, View> form = new HashMap<String, View>();
	private byte[] data = null;
	private boolean needData = false;
	private String dataInfo = null;
	private static final int TAKE_PHOTO = Activity.RESULT_FIRST_USER + 1;
	private static final int SELECT_PHOTO = Activity.RESULT_FIRST_USER + 2;
	private static final String photoFile = "/sdcard/bravo7.jpg";

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent dt) {
		Log.i(TAG, "onActivityResult: " + requestCode + ", " + resultCode);
		super.onActivityResult(requestCode, resultCode, dt);
		if (resultCode == Activity.RESULT_OK) {
			try {
				String photoPath = null;
				switch (requestCode) {
				case TAKE_PHOTO:
					photoPath = photoFile;
					break;
				case SELECT_PHOTO:
					Uri selectedImage = dt.getData();
					String[] filePathColumn = { MediaStore.Images.Media.DATA };

					Cursor cursor = getContentResolver().query(selectedImage,
							filePathColumn, null, null, null);
					cursor.moveToFirst();

					int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
					photoPath = cursor.getString(columnIndex);
					cursor.close();
					break;
				}
				if (null != photoPath) {
					notify("Photo will be uploaded");
				} else {
					notify("No photo was selected");
					return;
				}
				ExifInterface exifInterface = new ExifInterface(photoPath);
				File file = new File(photoPath);
				if (!file.exists()) {
					Log.w(TAG, "File not exist: " + photoFile);
					return;
				}
				if (exifInterface != null) {
					// Log.i(TAG,
					// "Orientation: "+exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
					// 0));
					// Log.i(TAG, "TNail: "+exifInterface.hasThumbnail());
					// Log.i(TAG,
					// "Width: "+exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH,
					// 0));
					// Log.i(TAG,
					// "Lengt: "+exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH,
					// 0));
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					byte[] buffer = new byte[1024];
					FileInputStream inputStream = new FileInputStream(file);
					int read = -1;
					while ((read = inputStream.read(buffer)) > 0) {
						stream.write(buffer, 0, read);
					}
					inputStream.close();
					// if(!file.delete()) {
					// Log.w(TAG, "Can't remove file "+photoFile);
					// }
					stream.close();
					data = stream.toByteArray();
					dataInfo = "w:"
							+ exifInterface.getAttributeInt(
									ExifInterface.TAG_IMAGE_WIDTH, 0)
							+ ",h:"
							+ exifInterface.getAttributeInt(
									ExifInterface.TAG_IMAGE_LENGTH, 0);
					Log.i(TAG, "Data: " + data.length + ", info: " + dataInfo);
				}
			} catch (Exception e) {
				Log.e(TAG, "Error processing image", e);
			}
		}
	}

	private void openGallery() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(intent, SELECT_PHOTO);
	}

	private void startCamera() {

		ContentValues values = new ContentValues();
		values.put(Media.TITLE, "Take photo:");

		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		File file = new File(photoFile);
		Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));

		startActivityForResult(i, TAKE_PHOTO);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "On create...: " + savedInstanceState);
		setContentView(R.layout.checkin);
		LinearLayout layout = (LinearLayout) findViewById(R.id.checkin_form_place);
		addButton = (Button) findViewById(R.id.checkin_add_button);
		addButton.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				sendCheckin(true);
				return true;
			}
		});
		addButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendCheckin(false);
			}
		});
		try {
			template = new JSONObject(getIntent().getStringExtra("object"));
			// Log.i(TAG, "Template: "+template);
			setTitle(template.optString("name"));
			editor = template.optJSONObject("editor");
			if (editor == null) {
				editor = new JSONObject();
			}
			Iterator keys = editor.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				JSONObject conf = editor.getJSONObject(key);
				TextView label = new TextView(this);
				label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
				label.setText(key.substring(0, 1).toUpperCase()
						+ key.substring(1) + ":");
				layout.addView(label);
				if ("text".equals(conf.optString("type"))) {
					EditText editText = new EditText(this);
					editText.setSingleLine(true);
					editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
							| InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
					if ("number".equals(conf.optString("format", ""))) {
						editText.setInputType(editText.getInputType()
								| InputType.TYPE_CLASS_NUMBER
								| InputType.TYPE_NUMBER_FLAG_DECIMAL
								| InputType.TYPE_NUMBER_FLAG_SIGNED);
					} else {
						editText.setInputType(editText.getInputType()
								| InputType.TYPE_CLASS_TEXT);
					}
					layout.addView(editText);
					form.put(key, editText);
				} else if ("textarea".equals(conf.optString("type"))) {
					EditText editText = new EditText(this);
					editText.setInputType(InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
							| InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
							| InputType.TYPE_TEXT_FLAG_MULTI_LINE);
					layout.addView(editText);
					form.put(key, editText);
				} else if ("check".equals(conf.optString("type"))) {
					CheckBox checkBox = new CheckBox(this);
					checkBox.setText(conf.optString("label", "check"));
					layout.addView(checkBox);
					form.put(key, checkBox);
				} else if ("location".equals(conf.optString("type"))) {
					needLocation = true;
					CheckBox checkBox = new CheckBox(this);
					checkBox.setChecked(true);
					checkBox.setText(conf.optString("label", "Detect location"));
					layout.addView(checkBox);
					form.put(key, checkBox);
				} else if ("path".equals(conf.optString("type"))) {
					EditText editText = new EditText(this);
					editText.setSingleLine(true);
					editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
							| InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
							| InputType.TYPE_CLASS_NUMBER
							| InputType.TYPE_NUMBER_FLAG_DECIMAL
							| InputType.TYPE_NUMBER_FLAG_SIGNED);
					editText.setText("10");
					layout.addView(editText);
					form.put(key, editText);
				} else if ("image".equals(conf.optString("type"))) {
					needData = true;
					LinearLayout buttons = new LinearLayout(this);
					buttons.setOrientation(LinearLayout.HORIZONTAL);
					LayoutParams buttonLayoutParams = new LayoutParams(
							LayoutParams.FILL_PARENT,
							LayoutParams.WRAP_CONTENT, 1);
					Button imageButton = new Button(this);
					imageButton.setLayoutParams(buttonLayoutParams);
					imageButton.setText("Camera");
					imageButton.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							startCamera();
						}
					});

					Button selectButton = new Button(this);
					selectButton.setLayoutParams(buttonLayoutParams);
					selectButton.setText("Gallery");
					selectButton.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							openGallery();
						}
					});
					buttons.addView(imageButton);
					buttons.addView(selectButton);
					layout.addView(buttons);
					if (savedInstanceState == null) {
						startCamera();
					}
				}
			}
			datePicker = new DatePicker(this);
			timePicker = new TimePicker(this);
			layout.addView(datePicker);
			layout.addView(timePicker);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void sendCheckin(boolean reload) {
		if (template == null) {
			notify("Invalid template");
			return;
		}
		if (needData && (data == null || dataInfo == null)) {
			notify("No media selected");
			return;
		}
		if (controller == null) {
			notify("No connection");
			return;
		}
		JSONObject checkin = new JSONObject();
		try {
			Calendar c = Calendar.getInstance();
			c.set(Calendar.DAY_OF_MONTH, 1);
			c.set(Calendar.YEAR, datePicker.getYear());
			c.set(Calendar.MONTH, datePicker.getMonth());
			c.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
			c.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
			c.set(Calendar.MINUTE, timePicker.getCurrentMinute());
			checkin.putOpt("created", c.getTimeInMillis());
			checkin.putOpt("template", template.optString("id"));
			Iterator keys = editor.keys();
			byte[] checkinData = null;
			long pathInterval = 0;
			while (keys.hasNext()) {
				String key = (String) keys.next();
				JSONObject conf = editor.getJSONObject(key);
				View view = form.get(key);
				if ("image".equals(conf.optString("type", ""))) {
					if (data != null && dataInfo != null) {
						checkin.put(conf.optString("info", "image_info"),
								dataInfo);
						checkinData = data;
						JSONObject info = new JSONObject();
						info.put("name", "image");
						info.put("type", "image/jpeg");
						checkin.put("__data", info);
					}
				}
				if (view != null) {
					if ("location".equals(conf.optString("type", ""))) {
						CheckBox checkBox = (CheckBox) view;
						needLocation = checkBox.isChecked();
					} else if ("path".equals(conf.optString("type", ""))) {
						EditText editText = (EditText) view;
						try {
							pathInterval = 60 * Integer.parseInt(editText.getText().toString());
						} catch (Exception e) {
						}
					} else if (view instanceof CheckBox) {
						CheckBox checkBox = (CheckBox) view;
						checkin.put(
								key,
								checkBox.isChecked() ? conf.optString("value",
										"yes") : "");
					} else {
						EditText editText = (EditText) view;
						checkin.putOpt(key, editText.getText().toString()
								.trim());
					}
				}
			}
			Intent callIntent = getIntent();
			String ref = callIntent.getStringExtra("ref");
			if (null != ref && !"".equals(ref)) {
				JSONArray refs = new JSONArray();
				refs.put(ref);
				checkin.put("refs", refs);
			}
			String attachTo = callIntent.getStringExtra("attach_to");
			if (null != attachTo && !"".equals(attachTo)) {
				checkin.put("attach_to", attachTo);
			}
			String error = controller.addCheckin(checkin, checkinData,
					needLocation, pathInterval, reload);
			if (error != null) {
				notify(error);
			} else {
				notify("Checkin created");
				finish();
				int widgetID = getIntent().getIntExtra("update_widget", -1);
				if (widgetID != -1) {
					ApplicationContext.getInstance().updateWidgets(widgetID);
				}
			}
		} catch (Exception e) {
		}
	}
}
