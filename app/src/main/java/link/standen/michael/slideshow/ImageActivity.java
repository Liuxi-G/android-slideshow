package link.standen.michael.slideshow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.Collections;

import link.standen.michael.slideshow.listener.OnSwipeTouchListener;
import link.standen.michael.slideshow.model.FileItem;
import link.standen.michael.slideshow.util.FileItemHelper;

/**
 * A full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ImageActivity extends BaseActivity {

	private static final String TAG = "ImageActivity";

	private int imagePosition;
	private int firstImagePosition;

	private static boolean STOP_ON_COMPLETE;
	private static boolean RANDOM_ORDER;
	private static int SLIDESHOW_DELAY;

	private final Handler mSlideshowHandler = new Handler();
	private final Runnable mSlideshowRunnable = new Runnable() {
		@Override
		public void run() {
			nextImage();
			if (!(STOP_ON_COMPLETE && imagePosition == firstImagePosition)) {
				mSlideshowHandler.postDelayed(mSlideshowRunnable, SLIDESHOW_DELAY);
			} else {
				show();
			}
		}
	};

	/**
	 * Some older devices needs a small delay between UI widget updates
	 * and a change of the status and navigation bar.
	 */
	private static final int UI_ANIMATION_DELAY = 300;
	private final Handler mHideHandler = new Handler();
	private View mContentView;
	private final Runnable mHidePart2Runnable = new Runnable() {
		@SuppressLint("InlinedApi")
		@Override
		public void run() {
			// Delayed removal of status and navigation bar

			// Note that some of these constants are new as of API 16 (Jelly Bean)
			// and API 19 (KitKat). It is safe to use them, as they are inlined
			// at compile-time and do nothing on earlier devices.
			mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

			// Start slideshow
			startSlideshow();
		}
	};
	private View mControlsView;
	private final Runnable mShowPart2Runnable = new Runnable() {
		@Override
		public void run() {
			// Delayed display of UI elements
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.show();
			}
			mControlsView.setVisibility(View.VISIBLE);
		}
	};
	private boolean mVisible;
	private final Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			hide();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_image);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		mVisible = true;
		mControlsView = findViewById(R.id.fullscreen_content_controls);
		mContentView = findViewById(R.id.fullscreen_content);

		// Load preferences
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		SLIDESHOW_DELAY = (int) Float.parseFloat(preferences.getString("slide_delay", "3")) * 1000;
		STOP_ON_COMPLETE = preferences.getBoolean("stop_on_complete", false);
		RANDOM_ORDER = preferences.getBoolean("random_order", false);

		// Gesture / click detection
		mContentView.setOnTouchListener(new OnSwipeTouchListener(this) {
			@Override
			public void onClick() {
				toggle();
			}

			@Override
			public void onSwipeLeft() {
				nextImage();
			}

			@Override
			public void onSwipeRight() {
				previousImage();
			}
		});

		// Set up image list
		currentPath = getIntent().getStringExtra("currentPath");
		imagePosition = getIntent().getIntExtra("imagePosition", -1);
		//TODO -1 check

		fileList = new FileItemHelper(this).getFileList(currentPath);
		if (RANDOM_ORDER){
			Collections.shuffle(fileList);
			// Call nextImage to ensure an image file is selected as the first file
			nextImage();
		} else {
			// This is in the else as nextImage contains a call to loadImage and we don't want to
			// duplicate the call
			loadImage();
		}
		firstImagePosition = imagePosition;

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isStoragePermissionGranted()) {
					deleteImage();
				}
			}
		});
	}

	/**
	 * Show the next image.
	 */
	private void nextImage(){
		//TODO What if loop?
		do {
			imagePosition++;
			if (imagePosition >= fileList.size()){
				imagePosition = 0;
			}
		} while (!testCurrentIsImage());
		loadImage();
	}

	/**
	 * Show the previous image.
	 */
	private void previousImage(){
		//TODO What if loop?
		do {
			imagePosition--;
			if (imagePosition < 0){
				imagePosition = fileList.size() - 1;
			}
		} while (!testCurrentIsImage());
		loadImage();
	}

	/**
	 * Tests if the current file item is an image.
	 * @return True if image, false otherwise.
     */
	private boolean testCurrentIsImage(){
		FileItem item = fileList.get(imagePosition);
		return new FileItemHelper(this).isImage(item);
	}

	private void loadImage(){
		//TODO boundary test
		FileItem item = fileList.get(imagePosition);
		setTitle(item.getName());
		((ImageView)findViewById(R.id.fullscreen_content)).setImageBitmap(BitmapFactory.decodeFile(item.getPath()));
	}

	/**
	 * Delete the current image
	 */
	private void deleteImage(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.delete_dialog_title);
		builder.setMessage(R.string.delete_dialog_message);
		builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				FileItem item = fileList.get(imagePosition);
				if (new File(item.getPath()).delete()) {
					fileList.remove(item);
					Toast.makeText(ImageActivity.this, R.string.image_deleted, Toast.LENGTH_SHORT).show();
					// Show next image
					imagePosition--;
					nextImage();
				} else {
					Toast.makeText(ImageActivity.this, R.string.image_not_deleted, Toast.LENGTH_SHORT).show();
				}
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	private void toggle() {
		if (mVisible) {
			hide();
		} else {
			show();
		}
	}

	private void hide() {

		// Hide UI first
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.hide();
		}
		mControlsView.setVisibility(View.GONE);
		mVisible = false;

		// Schedule a runnable to remove the status and navigation bar after a delay
		mHideHandler.removeCallbacks(mShowPart2Runnable);
		mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
	}

	private void show() {
		// Stop slideshow
		stopSlideshow();

		// Show the system bar
		mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
		mVisible = true;

		// Schedule a runnable to display UI elements after a delay
		mHideHandler.removeCallbacks(mHidePart2Runnable);
		mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
	}

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	/**
	 * Starts the slideshow
	 */
	private void startSlideshow(){
		mSlideshowHandler.removeCallbacks(mSlideshowRunnable);
		mSlideshowHandler.postDelayed(mSlideshowRunnable, SLIDESHOW_DELAY);
	}

	/**
	 * Stops the slideshow
	 */
	private void stopSlideshow(){
		mSlideshowHandler.removeCallbacks(mSlideshowRunnable);
	}



	/**
	 * Permissions checker
	 */
	public boolean isStoragePermissionGranted() {
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
				Log.v(TAG,"Permission is granted");
				return true;
			} else {
				Log.v(TAG,"Permission is revoked");
				requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
				return false;
			}
		} else { //permission is automatically granted on sdk<23 upon installation
			Log.v(TAG,"Permission is granted");
			return true;
		}
	}

	/**
	 * Permissions handler
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		Log.v(TAG,"Permission: " + permissions[0] + " was " + grantResults[0]);
		if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
			deleteImage();
		}
	}
}
