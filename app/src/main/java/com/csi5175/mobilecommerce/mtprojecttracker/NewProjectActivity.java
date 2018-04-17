package com.csi5175.mobilecommerce.mtprojecttracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.support.v4.app.DialogFragment;


import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;


public class NewProjectActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener{
    EditText title;
    EditText due;
    EditText course_num;
    EditText instructor_name;
    EditText project_name;
    EditText description;
    Button save;
    Button back;
    Button selectTime;

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
    public static final Integer CONTENT_MAXLINE = 18;
    public static final Integer CONTENT_MINLINE = 3;
    private SQLiteDatabase sqlDB;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.project_detail);

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


        title = (EditText)findViewById(R.id.editText6);
        due=(EditText)findViewById(R.id.new_due);
        course_num = (EditText)findViewById(R.id.new_course_num);
        instructor_name = (EditText)findViewById(R.id.new_instructor_name);
        project_name = (EditText)findViewById(R.id.new_project_name);
        description=(EditText)findViewById(R.id.editText7);
        save = (Button)findViewById(R.id.button5);
        selectTime = (Button)findViewById(R.id.select_time);

        // Show note details
        Intent i = getIntent();
        final String idString = i.getStringExtra(ID);
        if(i.getStringExtra(PROJECT_NAME)!=null && i.getStringExtra(DUE_DATE)!=null){
            String titleString = i.getStringExtra(COURSE_TITLE);
            String dueString = i.getStringExtra(DUE_DATE);
            String courseNumString = i.getStringExtra(COURSE_NUM);
            String instructorString = i.getStringExtra(INSTRUCTOR_NAME);
            String projectNameString = i.getStringExtra(PROJECT_NAME);
            String contextString = i.getStringExtra(DESCRIPTION);
            String statusString = i.getStringExtra(STATUS);;

            title.setText(titleString);
            due.setText(dueString);
            course_num.setText(courseNumString);
            instructor_name.setText(instructorString);
            project_name.setText(projectNameString);
            description.setText(contextString);



//            if (i.getStringExtra(PATH).equals("null")) {
//                img.setVisibility(View.GONE);
//                context.setMaxLines(CONTENT_MAXLINE);
//            } else {
//                img.setVisibility(View.VISIBLE);
//                Bitmap bitmap = BitmapFactory.decodeFile(i.getStringExtra(PATH));
//                img.setImageBitmap(bitmap);
//                context.setMaxLines(CONTENT_MINLINE);
//            }
        }
//        due.setText(getTime());

        // Back button
        back=(Button)findViewById(R.id.button6);
        back.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(i);
            }
        });

        // Save button
        save.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!title.getText().toString().equals("") && !description.getText().toString().equals("")) {

                    String titleString = title.getText().toString();
                    String timeString = due.getText().toString();
                    String courseNumString = course_num.getText().toString();
                    String instructorString = instructor_name.getText().toString();
                    String projectNameString = project_name.getText().toString();
                    String contextString = description.getText().toString();


                    String statusString = "todo";

                    //title text ,time text, context text
                    ContentValues cv = new ContentValues();
                    cv.put(COURSE_TITLE, titleString);
                    cv.put(DUE_DATE, timeString);
                    cv.put(COURSE_NUM, courseNumString);
                    cv.put(INSTRUCTOR_NAME, instructorString);
                    cv.put(PROJECT_NAME, projectNameString);
                    cv.put(DESCRIPTION, contextString);
                    cv.put(STATUS, statusString);

                    if(idString==null) {
                        sqlDB.insertOrThrow(TABLE_NAME,null,cv);
                    }
                    else {
                        sqlDB.update(TABLE_NAME, cv, ID + " = "+idString,null);
                    }
                    Intent i= new Intent(getApplicationContext(),MainActivity.class);
                    startActivity(i);
                }
                else{
                    Toast.makeText(getApplicationContext(),"title or content can not empty",Toast.LENGTH_SHORT).show();
                }
            }
        });


        // Select time button
        selectTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent i = new Intent(getApplicationContext(),MainActivity.class);
//                startActivity(i);
                DatePickerFragment mDatePicker = new DatePickerFragment();
                mDatePicker.show(getSupportFragmentManager(), "Select time");
            }
        });
    }

    // Get current system time for notes
    private String getTime() {
        //get Time
        long time=System.currentTimeMillis();
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d1=new Date(time);
        String t=format.format(d1);
        return t;
    }

    // Get current system time for notes
    private String getImgTime() {
        //get Img Time
        long time=System.currentTimeMillis();
        SimpleDateFormat format=new SimpleDateFormat("yyyyMMdd-HHmmss");
        Date d1=new Date(time);
        String t=format.format(d1);
        return t;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar cal = new GregorianCalendar(year, month, day);
        setDate(cal);
    }

    private void setDate(final Calendar calendar) {
        final DateFormat dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
        ((TextView) findViewById(R.id.new_due)).setText(dateFormat.format(calendar.getTime()));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sqlDB.close();
    }
}

