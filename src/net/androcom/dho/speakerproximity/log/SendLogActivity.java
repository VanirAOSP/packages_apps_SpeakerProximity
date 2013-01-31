/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (C) 2009 Xtralogic, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package net.androcom.dho.speakerproximity.log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.androcom.dho.speakerproximity.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class SendLogActivity extends Activity
{
    public final static String TAG = "SpeakerProximityLog";//$NON-NLS-1$

    public static final String ACTION_SEND_LOG = "net.androcom.dho.speakerproximity.log.intent.action.SEND_LOG";//$NON-NLS-1$
    public static final String EXTRA_SEND_INTENT_ACTION = "net.androcom.dho.speakerproximity.log.intent.extra.SEND_INTENT_ACTION";//$NON-NLS-1$
    public static final String EXTRA_DATA = "net.androcom.dho.speakerproximity.log.intent.extra.DATA";//$NON-NLS-1$
    public static final String EXTRA_ADDITIONAL_INFO = "net.androcom.dho.speakerproximity.log.intent.extra.ADDITIONAL_INFO";//$NON-NLS-1$
    public static final String EXTRA_SHOW_UI = "net.androcom.dho.speakerproximity.log.intent.extra.SHOW_UI";//$NON-NLS-1$
    public static final String EXTRA_FILTER_SPECS = "net.androcom.dho.speakerproximity.log.intent.extra.FILTER_SPECS";//$NON-NLS-1$
    public static final String EXTRA_FORMAT = "net.androcom.dho.speakerproximity.log.intent.extra.FORMAT";//$NON-NLS-1$
    public static final String EXTRA_BUFFER = "net.androcom.dho.speakerproximity.log.intent.extra.BUFFER";//$NON-NLS-1$
   
    final int MAX_LOG_MESSAGE_LENGTH = 100000;
   
    private AlertDialog mMainDialog;
    private Intent mSendIntent;
    private CollectLogTask mCollectLogTask;
    private ProgressDialog mProgressDialog;
    private String mAdditonalInfo;
    private boolean mShowUi;
    private String[] mFilterSpecs;
    private String mFormat;
    private String mBuffer;
   
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
       
        mSendIntent = null;
       
        Intent intent = getIntent();
        if (null != intent){
            String action = intent.getAction();  
            if (ACTION_SEND_LOG.equals(action)){
                String extraSendAction = intent.getStringExtra(EXTRA_SEND_INTENT_ACTION);
                if (extraSendAction == null){
                    Log.e(TAG, "Quiting, EXTRA_SEND_INTENT_ACTION is not supplied");//$NON-NLS-1$
                    finish();
                    return;
                }
               
                mSendIntent = new Intent(extraSendAction);
               
                Uri data = (Uri)intent.getParcelableExtra(EXTRA_DATA);
                if (data != null){
                    mSendIntent.setData(data);
                }
               
                String[] emails = intent.getStringArrayExtra(Intent.EXTRA_EMAIL);
                if (emails != null){
                    mSendIntent.putExtra(Intent.EXTRA_EMAIL, emails);
                }
               
                String[] ccs = intent.getStringArrayExtra(Intent.EXTRA_CC);
                if (ccs != null){
                    mSendIntent.putExtra(Intent.EXTRA_CC, ccs);
                }
               
                String[] bccs = intent.getStringArrayExtra(Intent.EXTRA_BCC);
                if (bccs != null){
                    mSendIntent.putExtra(Intent.EXTRA_BCC, bccs);
                }
               
                String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                if (subject != null){
                    mSendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                }
               
                mAdditonalInfo = intent.getStringExtra(EXTRA_ADDITIONAL_INFO);
                mShowUi = intent.getBooleanExtra(EXTRA_SHOW_UI, false);
                mFilterSpecs = intent.getStringArrayExtra(EXTRA_FILTER_SPECS);
                mFormat = intent.getStringExtra(EXTRA_FORMAT);
                mBuffer = intent.getStringExtra(EXTRA_BUFFER);
            }
        }
       
        if (null == mSendIntent){
            //standalone application
            mShowUi = true;
            mSendIntent = new Intent(Intent.ACTION_SEND);
            mSendIntent.putExtra(Intent.EXTRA_SUBJECT, "SpeakerProximity Android device log");
            mSendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"rac2030+SpeakerProximity@gmail.com"});
            mSendIntent.setType("text/plain");//$NON-NLS-1$
            mAdditonalInfo = "SpeakerProximityVersion: " + getVersionNumber(this) + "\n"
            					+ "Device model: " + Build.MODEL + "\n"
            					+ "Firmware version: " + Build.VERSION.RELEASE + "\n"
            					+ "Kernel version: " + getFormattedKernelVersion() + "\n"
            					+ "Build number: " + Build.DISPLAY + "\n";
            mFormat = "time";
        }
       
        if (mShowUi){
            mMainDialog = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setMessage("This will collect all logs related to SpeakerProximity")
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int whichButton){
                    collectAndSendLog();
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int whichButton){
                    finish();
                }
            })
            .show();
        }
        else{
            collectAndSendLog();
        }
    }
   
    @SuppressWarnings("unchecked")
    void collectAndSendLog(){
        /*Usage: logcat [options] [filterspecs]
        options include:
          -s              Set default filter to silent.
                          Like specifying filterspec '*:s'
          -f <filename>   Log to file. Default to stdout
          -r [<kbytes>]   Rotate log every kbytes. (16 if unspecified). Requires -f
          -n <count>      Sets max number of rotated logs to <count>, default 4
          -v <format>     Sets the log print format, where <format> is one of:

                          brief process tag thread raw time threadtime long

          -c              clear (flush) the entire log and exit
          -d              dump the log and then exit (don't block)
          -g              get the size of the log's ring buffer and exit
          -b <buffer>     request alternate ring buffer
                          ('main' (default), 'radio', 'events')
          -B              output the log in binary
        filterspecs are a series of
          <tag>[:priority]

        where <tag> is a log component tag (or * for all) and priority is:
          V    Verbose
          D    Debug
          I    Info
          W    Warn
          E    Error
          F    Fatal
          S    Silent (supress all output)

        '*' means '*:d' and <tag> by itself means <tag>:v

        If not specified on the commandline, filterspec is set from ANDROID_LOG_TAGS.
        If no filterspec is found, filter defaults to '*:I'


        If not specified with -v, format is set from ANDROID_PRINTF_LOG
        or defaults to "brief"*/

        ArrayList<String> list = new ArrayList<String>();
       
        if (mFormat != null){
            list.add("-v");
            list.add(mFormat);
        }
       
        if (mBuffer != null){
            list.add("-b");
            list.add(mBuffer);
        }

        if (mFilterSpecs != null){
            for (String filterSpec : mFilterSpecs){
                list.add(filterSpec);
            }
        }
       
        mCollectLogTask = (CollectLogTask) new CollectLogTask().execute(list);
    }
   
    private class CollectLogTask extends AsyncTask<ArrayList<String>, Void, StringBuilder>{
        @Override
        protected void onPreExecute(){
            showProgressDialog("Acquiring log from the system...");
        }
       
        @Override
        protected StringBuilder doInBackground(ArrayList<String>... params){
            final StringBuilder log = new StringBuilder();
            try{
                ArrayList<String> commandLine = new ArrayList<String>();
                commandLine.add("logcat");//$NON-NLS-1$
                commandLine.add("-d");//$NON-NLS-1$
                ArrayList<String> arguments = ((params != null) && (params.length > 0)) ? params[0] : null;
                if (null != arguments){
                    commandLine.addAll(arguments);
                }
               
                Process process = Runtime.getRuntime().exec(commandLine.toArray(new String[0]));
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
               
                String line;
                while ((line = bufferedReader.readLine()) != null){
                	if(line.contains("SpeakerProximity")) {
                		log.append(line);
                        log.append(System.getProperty("line.separator"));
                	}
                }
            }
            catch (IOException e){
                Log.e(TAG, "CollectLogTask.doInBackground failed", e);//$NON-NLS-1$
            }

            return log;
        }

        @Override
        protected void onPostExecute(StringBuilder log){
            if (null != log){
                //truncate if necessary
                int keepOffset = Math.max(log.length() - MAX_LOG_MESSAGE_LENGTH, 0);
                if (keepOffset > 0){
                    log.delete(0, keepOffset);
                }
               
                if (mAdditonalInfo != null){
                    log.insert(0, System.getProperty("line.separator"));
                    log.insert(0, mAdditonalInfo);
                }
               
                mSendIntent.putExtra(Intent.EXTRA_TEXT, log.toString());
                startActivity(Intent.createChooser(mSendIntent, "Select an application to send the log"));
                dismissProgressDialog();
                dismissMainDialog();
                finish();
            }
            else{
                dismissProgressDialog();
                showErrorDialog("Failed to get the log from the system.");
            }
        }
    }
   
    void showErrorDialog(String errorMessage){
        new AlertDialog.Builder(this)
        .setTitle(getString(R.string.app_name))
        .setMessage(errorMessage)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
                finish();
            }
        })
        .show();
    }
   
    void dismissMainDialog(){
        if (null != mMainDialog && mMainDialog.isShowing()){
            mMainDialog.dismiss();
            mMainDialog = null;
        }
    }
   
    void showProgressDialog(String message){
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(message);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
            public void onCancel(DialogInterface dialog){
                cancellCollectTask();
                finish();
            }
        });
        mProgressDialog.show();
    }
   
    private void dismissProgressDialog(){
        if (null != mProgressDialog && mProgressDialog.isShowing())
        {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }
   
    void cancellCollectTask(){
        if (mCollectLogTask != null && mCollectLogTask.getStatus() == AsyncTask.Status.RUNNING)
        {
            mCollectLogTask.cancel(true);
            mCollectLogTask = null;
        }
    }
   
    @Override
    protected void onPause(){
        cancellCollectTask();
        dismissProgressDialog();
        dismissMainDialog();
       
        super.onPause();
    }
   
    private static String getVersionNumber(Context context)
    {
        String version = "?";
        try
        {
            PackageInfo packagInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = packagInfo.versionName;
        }
        catch (PackageManager.NameNotFoundException e){};
       
        return version;
    }
   
    private String getFormattedKernelVersion()
    {
        String procVersionStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
            try {
                procVersionStr = reader.readLine();
            } finally {
                reader.close();
            }

            final String PROC_VERSION_REGEX =
                "\\w+\\s+" + /* ignore: Linux */
                "\\w+\\s+" + /* ignore: version */
                "([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
                "\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
                "\\([^)]+\\)\\s+" + /* ignore: (gcc ..) */
                "([^\\s]+)\\s+" + /* group 3: #26 */
                "(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
                "(.+)"; /* group 4: date */

            Pattern p = Pattern.compile(PROC_VERSION_REGEX);
            Matcher m = p.matcher(procVersionStr);

            if (!m.matches()) {
                Log.e(TAG, "Regex did not match on /proc/version: " + procVersionStr);
                return "Unavailable";
            } else if (m.groupCount() < 4) {
                Log.e(TAG, "Regex match on /proc/version only returned " + m.groupCount()
                        + " groups");
                return "Unavailable";
            } else {
                return (new StringBuilder(m.group(1)).append("\n").append(
                        m.group(2)).append(" ").append(m.group(3)).append("\n")
                        .append(m.group(4))).toString();
            }
        } catch (IOException e) {  
            Log.e(TAG,
                "IO Exception when getting kernel version for Device Info screen",
                e);

            return "Unavailable";
        }
    }
}