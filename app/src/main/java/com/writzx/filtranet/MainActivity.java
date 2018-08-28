package com.writzx.filtranet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.IntDef;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;

//    private static final int RC_OPEN_READ = 2;
//    private static final int RC_OPEN_WRITE = 2;
//    private static final String LAST_RETURNED_DOCUMENT_URI = "LAST_RETURNED_DOCUMENT_URI";
//    private static final String LAST_RETURNED_DOCUMENT_TREE_URI = "LAST_RETURNED_DOCUMENT_TREE_URI";

    @IntDef({INDEFINITE_SNACK, SHORT_SNACK, LONG_SNACK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Duration {
    }

    public static final int INDEFINITE_SNACK = Snackbar.LENGTH_INDEFINITE;
    public static final int SHORT_SNACK = Snackbar.LENGTH_SHORT;
    public static final int LONG_SNACK = Snackbar.LENGTH_LONG;


    private static final String TAG = "Filtranet";

    public static WeakReference<Context> context;

    public ListView fileListView;

    public static FileItemAdapter adapter;

    public void selectFileRead() {
        startActivityForResult(FileUtils.createOpenDocumentIntent(), READ_REQUEST_CODE);
    }

    private void selectFileWrite(String fileName) {
        startActivityForResult(FileUtils.createOpenDocumentIntent(fileName), WRITE_REQUEST_CODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fileListView = findViewById(R.id.fileListView);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                selectFileWrite("fuckingFile.txt");
//                selectFileRead();

                selectFileRead();
            }
        });

        // console = findViewById(R.id.console);

        context = new WeakReference<>((Context) this);

        adapter = new FileItemAdapter(this, new ArrayList<ListItem>());

        fileListView.setAdapter(adapter);

        showSnackbar("SOMETHING TO SHOW", LONG_SNACK);
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
                        uri = resultData.getData();

                        FileProcessor fp1 = new FileProcessor(uri);
                        fp1.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                    }
                }
                break;
            case WRITE_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = null;
                    if (resultData != null) {
                        uri = resultData.getData();
                        // Log.i(TAG, "Uri: " + uri.toString());
                        // writeFile(uri);
                    }
                }
                break;
        }
    }

    public static void showSnackbar(String msg, @Duration int duration) {
        View listView = ((Activity) MainActivity.context.get()).getWindow().getDecorView().findViewById(android.R.id.content).findViewById(R.id.fab);

        Snackbar.make(listView, msg, Snackbar.LENGTH_LONG).show();
    }

    FileOutputStream openFileWrite(Uri uri) {
//        OutputStream outputStream = null;
//        try {
//            outputStream = getContentResolver().openOutputStream(uri, "w");
//        } catch (IOException ex) {
//            Log.e(TAG, "URI Error: Could not resolve stream!");
//            return;
//        }
//
//        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
//
//        try {
//            writer.write("THIS IS A FUCKING NEW FILE CREATED BY THE APP FILTRANET!");
//            writer.flush();
//            writer.close();
//            // outputStream.close();
//        } catch (IOException ex) {
//            Log.e(TAG, "File Error: Could not open file!");
//            return;
//        }

        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
            return pfd != null ? new FileOutputStream(pfd.getFileDescriptor()) : null;
        } catch (IOException ex) {
            Log.e(TAG, "URI Error: Could not resolve stream!");
            return null;
        }
//        Log.i(TAG, "File Write successful!");
    }
}
