package org.eatabrick.dbxtest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileStatus;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

public class MainActivity extends FragmentActivity implements DbxFile.Listener {
  private static final String TAG = "MainActivity";
  private static final String APP_KEY = "17575fpwsxe3zao";
  private static final String APP_SECRET = "adnyhzwnstd3kzk";

  private static final int DROPBOX_LINK = 0;

  private DbxAccountManager mDbxManager;
  private DbxFile mFile;
  private TextView mText;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main);

    mDbxManager = DbxAccountManager.getInstance(getApplicationContext(), APP_KEY, APP_SECRET);
    mText = (TextView) findViewById(R.id.message);
  }

  @Override public void onResume() {
    super.onResume();
    loadFile();
  }

  @Override public void onPause() {
    super.onPause();
    releaseFile();
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == DROPBOX_LINK) {
      if (resultCode == Activity.RESULT_OK) {
        Log.d(TAG, "Dropbox link success");
        invalidateOptionsMenu();
        loadFile();
      } else {
        Log.d(TAG, "Dropbox link failure");
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);

    if (mDbxManager.hasLinkedAccount()) {
      MenuItem unlink = menu.add("Unlink Account");
      unlink.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        @Override public boolean onMenuItemClick(MenuItem item) {
          releaseFile();
          mDbxManager.unlink();
          invalidateOptionsMenu();
          return true;
        }
      });
    } else {
      MenuItem link = menu.add("Link Account");
      link.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        @Override public boolean onMenuItemClick(MenuItem item) {
          mDbxManager.startLink((Activity) MainActivity.this, DROPBOX_LINK);
          return true;
        }
      });
    }

    return true;
  }

  @Override public void onFileChange(DbxFile file) {
    Log.d(TAG, "File changed");

    if (mFile != null) {
      try {
        DbxFileStatus n = mFile.getNewerStatus();
        if (n != null && n.isCached) mFile.update();
      } catch (DbxException e) {
        // TODO real handling
        e.printStackTrace();
      }
    }

    setText();
  }

  private void setText() {
    try {
      DbxFileStatus s = mFile.getSyncStatus();
      if (s.isCached) {
        mText.setText(mFile.readString());
      }
    } catch (DbxException e) {
      // TODO real handling
      e.printStackTrace();
    } catch (IOException e) {
      // TODO real handling
      e.printStackTrace();
    }
  }

  private void releaseFile() {
    if (mFile != null) {
      mFile.removeListener(this);
      mFile.close();
      mFile = null;
    }

    mText.setText("");
  }

  private void loadFile() {
    if (mDbxManager.hasLinkedAccount()) {
      try {
        DbxFileSystem fs = DbxFileSystem.forAccount(mDbxManager.getLinkedAccount());
        mFile = fs.open(new DbxPath("test.txt"));
        mFile.addListener(this);
        setText();
      } catch (DbxException e) {
        // TODO real handling
        e.printStackTrace();
      }
    } else {
      releaseFile();
    }
  }
}
