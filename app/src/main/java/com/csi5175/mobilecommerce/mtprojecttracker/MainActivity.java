package com.csi5175.mobilecommerce.mtprojecttracker;

import android.annotation.SuppressLint;
import android.app.TabActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.*;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.util.LengthCheckInputStream;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends TabActivity implements TabHost.OnTabChangeListener {
    private TabHost tabHost;
    private ListView listView1;
    private ListView listView2;
    private ListView listView3;

    private static final String TODO_SPEC = "todoSpec";
    private static final String COMPLETED_SPEC = "completedSpec";
    private static final String ALL_SPEC = "allSpec";
    private static final String LIST1_TAG = "TO DO";
    private static final String LIST2_TAG = "COMPLETED";
    private static final String LIST3_TAG = "ALL";

    private static final String DATABASE_NAME = "s3db";
    private static final String TABLE_NAME = "project";
    public static final String ID = "_id";
    public static final String COURSE_TITLE = "title";
    public static final String COURSE_NUM = "course_num";
    public static final String INSTRUCTOR_NAME = "instructor_name";
    public static final String PROJECT_NAME = "project_name";
    public static final String DESCRIPTION = "description";
    public static final String DUE_DATE = "due";
    public static final String STATUS = "status";
    private SQLiteDatabase sqlDB;
    private Cursor cursor_all;
    private Cursor cursor_todo;
    private Cursor cursor_completed;

    private int currentTab;

    private AWSCredentialsProvider awsCredentialsProvider;
    private AWSConfiguration awsConfiguration;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create or open database
        sqlDB = openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.CREATE_IF_NECESSARY,null);

//        sqlDB.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
//        sqlDB.delete(TABLE_NAME, null, null);

        // Create table
        try {
            sqlDB.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COURSE_TITLE + " TEXT NOT NULL,"
                    + COURSE_NUM + " TEXT NOT NULL,"
                    + INSTRUCTOR_NAME + " TEXT NOT NULL,"
                    + PROJECT_NAME + " TEXT NOT NULL,"
                    + DESCRIPTION + " TEXT NOT NULL,"
                    + DUE_DATE + " TEXT NOT NULL,"
                    + STATUS + " TEXT NOT NULL);");
        }catch(Exception e){
        }

        cursor_all = sqlDB.query(TABLE_NAME, null, null, null, null, null, null);
        cursor_todo = sqlDB.query(TABLE_NAME, null, STATUS+"=?", new String[]{"todo"}, null, null, null);
        cursor_completed = sqlDB.query(TABLE_NAME, null, STATUS+"=?", new String[]{"completed"}, null, null, null);


        currentTab = 0;

        listView1 = (ListView)findViewById(R.id.list1);
        listView2 = (ListView)findViewById(R.id.list2);
        listView3 = (ListView)findViewById(R.id.list3);
        tabHost = getTabHost();
        tabHost.setOnTabChangedListener(this);

        List<String> list1 = new ArrayList<String>();
        list1.add("MT 1");
        list1.add("MT 2");
        list1.add("MT 3");

        List<String> list2 = new ArrayList<String>();
        list2.add("DONNY 1");
        list2.add("DONNY 2");

        List<String> list3 = new ArrayList<String>();
        list3.add("Jade 1");

//        listView1.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, list1));
//        listView2.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, list2));
//        listView3.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, list3));
        listView1.setAdapter(RefreshAdapter(cursor_todo));
        listView2.setAdapter(RefreshAdapter(cursor_completed));
        listView3.setAdapter(RefreshAdapter(cursor_all));

        tabHost.addTab(tabHost.newTabSpec(TODO_SPEC).setIndicator(LIST1_TAG).setContent(R.id.list1));
        tabHost.addTab(tabHost.newTabSpec(COMPLETED_SPEC).setIndicator(LIST2_TAG).setContent(R.id.list2));
        tabHost.addTab(tabHost.newTabSpec(ALL_SPEC).setIndicator(LIST3_TAG).setContent(R.id.list3));

        tabHost.setCurrentTab(currentTab);

        AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {

                // Obtain the reference to the AWSCredentialsProvider and AWSConfiguration objects
                awsCredentialsProvider = AWSMobileClient.getInstance().getCredentialsProvider();
                awsConfiguration = AWSMobileClient.getInstance().getConfiguration();

                // Use IdentityManager#getUserID to fetch the identity id.
                IdentityManager.getDefaultIdentityManager().getUserID(new IdentityHandler() {
                    @Override
                    public void onIdentityId(String identityId) {
                        Log.d("YourMainActivity", "Identity ID = " + identityId);

                        // Use IdentityManager#getCachedUserID to
                        //  fetch the locally cached identity id.
                        final String cachedIdentityId =
                                IdentityManager.getDefaultIdentityManager().getCachedUserID();
                    }

                    @Override
                    public void handleError(Exception exception) {
                        Log.d("YourMainActivity", "Error in retrieving the identity" + exception);
                    }
                });
            }
        }).execute();


        uploadWithTransferUtility();
    }


    private void uploadWithTransferUtility() {

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();

        File file = new File("/sdcard/DCIM/","s3Key.txt");
        try {
//            file.mkdir();
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        TransferObserver uploadObserver =
                transferUtility.upload(
                        "s3Key.txt", file);

        // Attach a listener to the observer to get state update and progress notifications
        uploadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed upload.
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                Log.d("YourActivity", "ID:" + id + " bytesCurrent: " + bytesCurrent
                        + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
            }

        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == uploadObserver.getState()) {
            // Handle a completed upload.
        }

        Log.d("YourActivity", "Bytes Transferrred: " + uploadObserver.getBytesTransferred());
        Log.d("YourActivity", "Bytes Total: " + uploadObserver.getBytesTotal());
    }

    @Override
    public void onTabChanged(String tabName) {
        if(tabName.equals("COMPLETED")) {

        } else {

        }
    }

    private SimpleCursorAdapter RefreshAdapter(Cursor c){
        SimpleCursorAdapter listAdapter = new SimpleCursorAdapter(this,
                R.layout.project_list,
                c,
                new String[]{PROJECT_NAME, DUE_DATE},
                new int[]{R.id.project_name, R.id.project_due},
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        return  listAdapter;
    }
}
