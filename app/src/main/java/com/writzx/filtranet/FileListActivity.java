package com.writzx.filtranet;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

public class FileListActivity extends AppCompatActivity {
    private static final int READ_REQUEST_CODE = 0xee14;
    private static final int WRITE_REQUEST_CODE = 0xee15;

    private static final String TAG = "Filtranet";

    public static final String RESULT_KEY = "com.writzx.filtranet.RESULT_FITEM";
    public static final String FILE_CREATE_MODE_KEY = "com.writzx.filtranet.OPEN_OR_CREATE";

    @IntDef({INDEFINITE_SNACK, SHORT_SNACK, LONG_SNACK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Duration {
    }

    public static final int INDEFINITE_SNACK = Snackbar.LENGTH_INDEFINITE;
    public static final int SHORT_SNACK = Snackbar.LENGTH_SHORT;
    public static final int LONG_SNACK = Snackbar.LENGTH_LONG;

    public static WeakReference<Context> context;
    public ListView fileListView;
    public boolean createMode = false;

    public void selectFileRead() {
        startActivityForResult(FileUtils.newOpenDocumentIntent(), READ_REQUEST_CODE);
    }

    private void selectFileWrite(String fileName, String type) {
        startActivityForResult(FileUtils.newCreateDocumentIntent(fileName, type), WRITE_REQUEST_CODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fileListView = findViewById(R.id.fileListView);

        createMode = getIntent().getBooleanExtra(FILE_CREATE_MODE_KEY, false);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFileRead();
            }
        });

        context = new WeakReference<>((Context) this);

        fileListView.setAdapter(MainActivity.adapter);
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (createMode) {
                    // cannot overwrite existing file
                } else {
                    if (MainActivity.adapter.getItem(position).getType() == ListItem.TYPE_FILE) {
                        FileItem fitem = (FileItem) MainActivity.adapter.getItem(position);

                        if (fitem == null || fitem.getFile() == null) {
                            // todo start file processor to process the file from uri if its present
                            // todo else remove and show error
                        }

                        setResult(RESULT_OK, new Intent().putExtra(RESULT_KEY, fitem));
                        finish();
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent resultData) {
        switch (requestCode) {
            case READ_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = null;
                    if (resultData != null) {
                        ClipData cd = resultData.getClipData();
                        if (cd != null) {
                            Uri[] uris = new Uri[cd.getItemCount()];
                            for (int i = 0; i < uris.length; i++) {
                                uris[i] = cd.getItemAt(i).getUri();

                                FileProcessor.create(uris[i]).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                            }
                        } else {
                            uri = resultData.getData();

                            FileProcessor.create(uri).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                        }
                    }
                }
                break;
            case WRITE_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    // file already created by the system

                    Uri uri = null;
                    if (resultData != null) {
                        ClipData cd = resultData.getClipData();
                        if (cd != null) {
                            // error (cannot create multiple files at once.
                            return;
                        }

                        uri = resultData.getData();
                    }
                }
                break;
        }
    }

    public static void showSnackbar(String msg, @Duration int duration) {
        View listView = ((Activity) FileListActivity.context.get()).getWindow().getDecorView().findViewById(android.R.id.content).findViewById(R.id.fab);

        Snackbar.make(listView, msg, duration).show();
    }
}
