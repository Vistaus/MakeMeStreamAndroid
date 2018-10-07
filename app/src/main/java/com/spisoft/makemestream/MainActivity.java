package com.spisoft.makemestream;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private String mUri;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getIntent().getData()!=null)
        mUri = getIntent().getData().toString();
        if(mUri!=null)
        Log.d("streamdebug","all "+getIntent().getData().toString());
        Log.d("streamdebug","all "+getIntent().toString());
        if(mUri == null && getIntent().getClipData()!=null && getIntent().getClipData().getItemCount() >0)
            mUri = getIntent().getClipData().getItemAt(0).getText().toString();
        if(mUri!=null)
            mUri = mUri.replace("/watch/","/embed/");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        try {
            final JSONArray array = new JSONArray(PreferenceManager.getDefaultSharedPreferences(this).getString("tvs","[]"));
            ListView lv = findViewById(R.id.list);

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
                    new Thread(){
                        public void run(){
                            if(mUri == null)
                                return;
                            // Create a new HttpClient and Post Header

                            HttpClient httpclient = new DefaultHttpClient();
                            HttpPost httppost = null;
                            try {
                                httppost = new HttpPost(array.getJSONObject(position).getString("server")+"/sendlink.php");
                                Log.d("streamdebug","sending to "+array.getJSONObject(position).getString("server")+"/sendlink.php");
                                Log.d("streamdebug","ui  "+array.getJSONObject(position).getString("uid"));
                                try {
                                    // Add your data
                                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                                    nameValuePairs.add(new BasicNameValuePair("uid", array.getJSONObject(position).getString("uid")));
                                    nameValuePairs.add(new BasicNameValuePair("url", mUri.toString()));
                                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                                    Log.d("streamdebug", "bla ");
                                    // Execute HTTP Post Request
                                    HttpResponse response = httpclient.execute(httppost);
                                    BufferedReader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                                    StringBuilder total = new StringBuilder();
                                    String line;
                                    while ((line = r.readLine()) != null) {
                                        total.append(line).append('\n');
                                    }
                                    Log.d("streamdebug", "res "+total);
                                    view.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            MainActivity.this.finish();
                                        }
                                    });

                                } catch (ClientProtocolException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                    }.start();
                }
            });
            final BaseAdapter adapter = new BaseAdapter() {
                @Override
                public int getCount() {
                    return array.length();
                }

                @Override
                public Object getItem(int position) {
                    try {
                        return array.get(position);
                    } catch (JSONException e) {
                        return null;
                    }
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView tv;
                    if (convertView == null)
                        tv = new TextView(MainActivity.this);
                    else
                        tv = (TextView) convertView;
                    tv.setTextSize(20);
                    tv.setPadding(20,30,20,30);
                    try {
                        tv.setText(array.getJSONObject(position).getString("name"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return tv;
                }
            };
            lv.setAdapter(adapter);
            lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    array.remove(position);
                    PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString("tvs",array.toString()).commit();
                    adapter.notifyDataSetChanged();
                    return true;
                }
            });
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final View v = LayoutInflater.from(MainActivity.this).inflate(R.layout.add_new_tv, null);
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                            .setView(v)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String addr = ((TextView)v.findViewById(R.id.make_me_stream_server)).getText().toString();
                                    String uid = ((TextView)v.findViewById(R.id.tv_uid)).getText().toString();
                                    String name = ((TextView)v.findViewById(R.id.tv_name)).getText().toString();
                                    if(name.isEmpty())
                                        name = uid;
                                    if(uid.isEmpty())
                                        return;
                                    if(!addr.startsWith("http"))
                                        addr = "https://"+addr;
                                    JSONObject object = new JSONObject();
                                    try {
                                        object.put("name",name);
                                        object.put("server",addr);
                                        object.put("uid",uid);
                                        array.put(object);
                                        PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString("tvs",array.toString()).commit();
                                        adapter.notifyDataSetChanged();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.cancel,null).show();

                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
