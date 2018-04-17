package com.csi5175.mobilecommerce.mtprojecttracker;

import android.app.TabActivity;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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

    private int currentTab;

    private AWSCredentialsProvider awsCredentialsProvider;
    private AWSConfiguration awsConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentTab = 0;

        listView1 = (ListView)findViewById(R.id.list1);
        listView2 = (ListView)findViewById(R.id.list2);
        listView3 = (ListView)findViewById(R.id.list3);
        tabHost = getTabHost();
        tabHost.setOnTabChangedListener(this);

        /*
        TabSpec tabSpec1 = tabHost.newTabSpec(TODO_SPEC);
        TabSpec tabSpec2 = tabHost.newTabSpec(COMPLETED_SPEC);
        TabSpec tabSpec3 = tabHost.newTabSpec(ALL_SPEC);


        tabSpec1.setIndicator(LIST1_TAG);
        tabSpec2.setIndicator(LIST2_TAG);
        tabSpec3.setIndicator(LIST3_TAG);

*/
        List<String> list1 = new ArrayList<String>();
        list1.add("MT 1");
        list1.add("MT 2");
        list1.add("MT 3");

        List<String> list2 = new ArrayList<String>();
        list2.add("DONNY 1");
        list2.add("DONNY 2");

        List<String> list3 = new ArrayList<String>();
        list3.add("Jade 1");

        listView1.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, list1));
        listView2.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, list2));
        listView3.setAdapter(new ArrayAdapter(this, android.R.layout.simple_list_item_1, list3));

        tabHost.addTab(tabHost.newTabSpec(TODO_SPEC).setIndicator(LIST1_TAG).setContent(R.id.list1));
        tabHost.addTab(tabHost.newTabSpec(COMPLETED_SPEC).setIndicator(LIST2_TAG).setContent(R.id.list2));
        tabHost.addTab(tabHost.newTabSpec(ALL_SPEC).setIndicator(LIST3_TAG).setContent(R.id.list3));
/*

        tabSpec1.setContent(new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String s) {
                return listView1;
            }
        });

        tabSpec2.setContent(new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String s) {
                return listView2;
            }
        });

        tabSpec3.setContent(new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String s) {
                return listView3;
            }
        });
*/

/*
        tabHost.addTab(tabSpec1);
        tabHost.addTab(tabSpec2);
        tabHost.addTab(tabSpec3);
*/
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
}
