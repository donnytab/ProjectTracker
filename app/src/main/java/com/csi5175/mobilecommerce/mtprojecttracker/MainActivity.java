package com.csi5175.mobilecommerce.mtprojecttracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.TabActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TabHost;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityHandler;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.*;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends TabActivity implements TabHost.OnTabChangeListener {
    private TabHost tabHost;
    private ListView listView1;
    private ListView listView2;
    private ListView listView3;
    private FloatingActionButton newBtn;
    private FloatingActionButton syncBtn;
    private FloatingActionButton aboutBtn;
    private SearchView searchView;

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
    private  Cursor cursor_todo_search;
    private  Cursor cursor_completed_search;
    private  Cursor cursor_all_search;
    private static MediaPlayer player;

    private static final String PROJECT_DATA_LOCAL_PATH = "/sdcard/DCIM/AWS-S3";

    private int currentTab;
    private static boolean hasMusic = false;

    private AWSCredentialsProvider awsCredentialsProvider;
    private AWSConfiguration awsConfiguration;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                System.exit(0);
        }

        // Create or open database
        sqlDB = openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.CREATE_IF_NECESSARY,null);

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
        newBtn = (FloatingActionButton)findViewById(R.id.new_button);
        syncBtn = (FloatingActionButton)findViewById(R.id.sync_button);
        aboutBtn = (FloatingActionButton)findViewById(R.id.about_button);
        searchView = (SearchView)findViewById(R.id.search_view);
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

        // Play background music
        startMusicService();

        // Show urgent due warning list
        showProjectDueWithinTwoDays();

        listView1.setAdapter(RefreshAdapter(cursor_todo));
        listView2.setAdapter(RefreshAdapter(cursor_completed));
        listView3.setAdapter(RefreshAdapter(cursor_all));

        // Search for course title or project name or description
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText)){
                    // Query based on input text
                    cursor_todo_search = sqlDB.rawQuery("SELECT * FROM " + TABLE_NAME +" WHERE " + COURSE_TITLE +" LIKE '%" + newText
                            + "%' OR "+ PROJECT_NAME +" LIKE '%" + newText + "%' OR " + DESCRIPTION + " LIKE '%" + newText + "%' AND " + STATUS + "='todo'", null);
                    cursor_completed_search = sqlDB.rawQuery("SELECT * FROM " + TABLE_NAME +" WHERE " + COURSE_TITLE +" LIKE '%" + newText
                            + "%' OR "+ PROJECT_NAME +" LIKE '%" + newText + "%' OR " + DESCRIPTION + " LIKE '%" + newText + "%' AND " + STATUS + "='completed'", null);
                    cursor_all_search = sqlDB.rawQuery("SELECT * FROM " + TABLE_NAME +" WHERE " + COURSE_TITLE +" LIKE '%" + newText
                            + "%' OR "+ PROJECT_NAME +" LIKE '%" + newText + "%' OR " + DESCRIPTION + " LIKE '%" + newText + "%' ", null);
                    listView1.setAdapter(RefreshAdapter(cursor_todo_search));
                    listView2.setAdapter(RefreshAdapter(cursor_completed_search));
                    listView3.setAdapter(RefreshAdapter(cursor_all_search));
                    listView1.invalidateViews();
                    listView2.invalidateViews();
                    listView3.invalidateViews();
                }else{
                    // List all notes if input text is empty
                    listView1.setAdapter(RefreshAdapter(cursor_todo));
                    listView2.setAdapter(RefreshAdapter(cursor_completed));
                    listView3.setAdapter(RefreshAdapter(cursor_all));
                    listView1.invalidateViews();
                    listView2.invalidateViews();
                    listView3.invalidateViews();
                }
                return false;
            }
        });

        //enter note_add page
        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(), NewProjectActivity.class);
                i.putExtra(ID, cursor_todo.getString(0));
                i.putExtra(COURSE_TITLE, cursor_todo.getString(1));
                i.putExtra(COURSE_NUM, cursor_todo.getString(2));
                i.putExtra(INSTRUCTOR_NAME, cursor_todo.getString(3));
                i.putExtra(PROJECT_NAME, cursor_todo.getString(4));
                i.putExtra(DESCRIPTION, cursor_todo.getString(5));
                i.putExtra(DUE_DATE, cursor_todo.getString(6));
                i.putExtra(STATUS, cursor_todo.getString(7));
                startActivity(i);
            }
        });

        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(), NewProjectActivity.class);
                i.putExtra(ID, cursor_completed.getString(0));
                i.putExtra(COURSE_TITLE, cursor_completed.getString(1));
                i.putExtra(COURSE_NUM, cursor_completed.getString(2));
                i.putExtra(INSTRUCTOR_NAME, cursor_completed.getString(3));
                i.putExtra(PROJECT_NAME, cursor_completed.getString(4));
                i.putExtra(DESCRIPTION, cursor_completed.getString(5));
                i.putExtra(DUE_DATE, cursor_completed.getString(6));
                i.putExtra(STATUS, cursor_completed.getString(7));
                startActivity(i);
            }
        });

        listView3.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(), NewProjectActivity.class);
                i.putExtra(ID, cursor_all.getString(0));
                i.putExtra(COURSE_TITLE, cursor_all.getString(1));
                i.putExtra(COURSE_NUM, cursor_all.getString(2));
                i.putExtra(INSTRUCTOR_NAME, cursor_all.getString(3));
                i.putExtra(PROJECT_NAME, cursor_all.getString(4));
                i.putExtra(DESCRIPTION, cursor_all.getString(5));
                i.putExtra(DUE_DATE, cursor_all.getString(6));
                i.putExtra(STATUS, cursor_all.getString(7));
                startActivity(i);
            }
        });

        tabHost.addTab(tabHost.newTabSpec(TODO_SPEC).setIndicator(LIST1_TAG).setContent(R.id.list1));
        tabHost.addTab(tabHost.newTabSpec(COMPLETED_SPEC).setIndicator(LIST2_TAG).setContent(R.id.list2));
        tabHost.addTab(tabHost.newTabSpec(ALL_SPEC).setIndicator(LIST3_TAG).setContent(R.id.list3));

        tabHost.setCurrentTab(currentTab);


//        uploadWithTransferUtility();

        // To NewProject activity
        newBtn.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),NewProjectActivity.class);
                startActivity(i);
            }
        });

        // About button listener
        aboutBtn.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] aboutArray = {"Shenyong Guan", "sguan044@uottawa.ca", "Wendong Yuan", "wyuan011@uottawa.ca"};

                // Show dialog
                final AlertDialog.Builder aboutDialog = new AlertDialog.Builder(MainActivity.this);

                aboutDialog.setTitle("ABOUT US");
                aboutDialog.setItems(aboutArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });

                aboutDialog.show();
            }
        });

        // Sync listener
        syncBtn.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Sync project files to Amazon S3
                syncAWS();
            }
        });

        listView1.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, final long id) {
                final CharSequence[] optionList = {"Summary", "Delete"};

                // Show option menu
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setItems(optionList, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // summary option
                        if(optionList[i].equals("Summary")) {
                            cursor_todo.moveToPosition(position);
                            String course_title = cursor_todo.getString(1);
                            String project_name = cursor_todo.getString(4);
                            String due_date = cursor_todo.getString(6);
                            String status = cursor_todo.getString(7);

                            String summary = "Course Title : " + course_title + "\n" +
                                             "Project Name : " + project_name + "\n" +
                                             "Due Date : " + due_date + "\n" +
                                             "Status : " + status;

                            // Show dialog
                            final AlertDialog.Builder warningListDialog = new AlertDialog.Builder(MainActivity.this);
                            warningListDialog.setTitle("SUMMARY");
                            warningListDialog.setMessage(summary);
                            warningListDialog.show();
                        }

                        // Delete option
                        if(optionList[i].equals("Delete")) {
                            cursor_todo.moveToPosition(position);
                            String id = cursor_todo.getString(0);
                            if(!id.equals("")) {
                                sqlDB.delete(TABLE_NAME, ID + "=" + id, null);
                            }
                            Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                            startActivity(intent);
                        }
                    }
                });
                builder.show();
                return true;
            }
        });

        listView2.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, final long id) {
                final CharSequence[] optionList = {"Summary", "Delete"};

                // Show option menu
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setItems(optionList, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // summary option
                        if(optionList[i].equals("Summary")) {
                            cursor_completed.moveToPosition(position);
                            String course_title = cursor_completed.getString(1);
                            String project_name = cursor_completed.getString(4);
                            String due_date = cursor_completed.getString(6);
                            String status = cursor_completed.getString(7);

                            String summary = "Course Title : " + course_title + "\n" +
                                    "Project Name : " + project_name + "\n" +
                                    "Due Date : " + due_date + "\n" +
                                    "Status : " + status;

                            // Show dialog
                            final AlertDialog.Builder warningListDialog = new AlertDialog.Builder(MainActivity.this);
                            warningListDialog.setTitle("SUMMARY");
                            warningListDialog.setMessage(summary);
                            warningListDialog.show();
                        }

                        // Delete option
                        if(optionList[i].equals("Delete")) {
                            cursor_completed.moveToPosition(position);
                            String id = cursor_completed.getString(0);
                            if(!id.equals("")) {
                                sqlDB.delete(TABLE_NAME, ID + "=" + id, null);
                            }
                            Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                            startActivity(intent);
                        }
                    }
                });
                builder.show();
                return true;
            }
        });

        listView3.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, final long id) {
                final CharSequence[] optionList = {"Summary", "Delete"};

                // Show option menu
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setItems(optionList, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // summary option
                        if(optionList[i].equals("Summary")) {
                            cursor_all.moveToPosition(position);
                            String course_title = cursor_all.getString(1);
                            String project_name = cursor_all.getString(4);
                            String due_date = cursor_all.getString(6);
                            String status = cursor_all.getString(7);

                            String summary = "Course Title : " + course_title + "\n" +
                                    "Project Name : " + project_name + "\n" +
                                    "Due Date : " + due_date + "\n" +
                                    "Status : " + status;

                            // Show dialog
                            final AlertDialog.Builder warningListDialog = new AlertDialog.Builder(MainActivity.this);
                            warningListDialog.setTitle("SUMMARY");
                            warningListDialog.setMessage(summary);
                            warningListDialog.show();
                        }

                        // Delete option
                        if(optionList[i].equals("Delete")) {
                            cursor_all.moveToPosition(position);
                            String id = cursor_all.getString(0);
                            if(!id.equals("")) {
                                sqlDB.delete(TABLE_NAME, ID + "=" + id, null);
                            }
                            Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                            startActivity(intent);
                        }
                    }
                });
                builder.show();
                return true;
            }
        });

    }

    private void syncAWS() {
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

        serializeDataWithTransferUtility();
    }


    private void serializeDataWithTransferUtility() {
        File dir = new File(PROJECT_DATA_LOCAL_PATH);
        dir.mkdir();
        if(!dir.exists() || !dir.isDirectory()) {
            Toast.makeText(getApplicationContext(),"Failed to synchronize projects : invalid storage path",Toast.LENGTH_SHORT).show();
            return;
        }

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();

        ArrayList<HashMap<String, String>> awsList = new ArrayList<HashMap<String, String>>();


        // Clear previous project data
        for(File oldFile : dir.listFiles()) {
            if(oldFile.isFile()) {
                oldFile.delete();
            }
        }

        // Retrieve all project data
        if(cursor_all.moveToFirst()) {
            do {
                HashMap<String, String> projectMap = new HashMap<String, String>();
                projectMap.put(ID, cursor_all.getString(0));
                projectMap.put(COURSE_TITLE, cursor_all.getString(1));
                projectMap.put(COURSE_NUM, cursor_all.getString(2));
                projectMap.put(INSTRUCTOR_NAME, cursor_all.getString(3));
                projectMap.put(PROJECT_NAME, cursor_all.getString(4));
                projectMap.put(DESCRIPTION, cursor_all.getString(5));
                projectMap.put(DUE_DATE, cursor_all.getString(6));
                projectMap.put(STATUS, cursor_all.getString(7));

                awsList.add(projectMap);
            } while (cursor_all.moveToNext());
        }

        for(HashMap<String, String> dataItem : awsList) {
            String fileName = dataItem.get(ID) + "-" + dataItem.get(COURSE_TITLE).replace(" ", "_") + "-"
                    + dataItem.get(PROJECT_NAME).replace(" ", "_") + ".txt";

            File projectTxt = new File(PROJECT_DATA_LOCAL_PATH, fileName);
            try {
                projectTxt.createNewFile();

                BufferedWriter bfw = new BufferedWriter(new FileWriter(projectTxt, false));
                bfw.write(dataItem.toString());
                bfw.newLine();
                bfw.flush();
                bfw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            TransferObserver uploadObserver = transferUtility.upload(fileName, projectTxt);

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


            // check for the state and progress in the observer.
            if (TransferState.COMPLETED == uploadObserver.getState()) {
                // Handle a completed upload.
            }

            Log.d("YourActivity", "Bytes Transferrred: " + uploadObserver.getBytesTransferred());
            Log.d("YourActivity", "Bytes Total: " + uploadObserver.getBytesTotal());
        }

        Toast.makeText(getApplicationContext(),"Sync Finished",Toast.LENGTH_SHORT).show();
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

    private boolean isDueWithinTwoDays(String dueDate) {
        final DateFormat dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG);
        try {
            Calendar calendar = Calendar.getInstance();
            Date due = dateFormat.parse(dueDate);
            Date currentDate = calendar.getTime();
            long diffMillisecond = due.getTime() - currentDate.getTime();
            long diffDay = diffMillisecond/(1000 * 60 * 60 * 24);

            return (diffDay <= 2) && (diffDay >=0);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showProjectDueWithinTwoDays() {
        ArrayList<String> urgentDueList = new ArrayList<>();

        if(cursor_all.moveToFirst()) {
            do {
                if(isDueWithinTwoDays(cursor_all.getString(6))) {
                    String warningListItem = cursor_all.getString(4) + "         " + cursor_all.getString(6);

                    urgentDueList.add(warningListItem);
                }

            } while (cursor_all.moveToNext());
        }

        // Show dialog
        final AlertDialog.Builder warningListDialog = new AlertDialog.Builder(MainActivity.this);
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, urgentDueList);


        warningListDialog.setTitle("WARNING : DUE WITHIN 2 DAYS");
        warningListDialog.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });

        warningListDialog.show();
    }

    // Play background music
    private void startMusicService() {
        if(!hasMusic) {
            try
            {
                player = MediaPlayer.create(this, R.raw.zayn);
                player.start();
                Log.e("music", "music starts...");
            } catch (Exception e) {
                e.printStackTrace();
            }
            hasMusic = true;
        }
    }

    // Stop background music
    private void stopMusicService() {
        if(hasMusic) {
            player.release();
            Log.e("music", "music ends...");
            hasMusic = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMusicService();
        sqlDB.close();
    }
}
