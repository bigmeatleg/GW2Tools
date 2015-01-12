package com.jinder.gw2tools;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


public class MainActivity extends Activity{
    private static final String db_filename = "gw2.sqlite";
    private static final String LOG_DBNAME = "DATABASE";
    private static final int db_version = 1;

    DBAdapter               db;
    List<GW2Event>          aryEvents = new ArrayList<GW2Event>();
    ArrayAdapter<GW2Event>  adGW2;
    ListView                rows;
    int                     tIndex;
    int                     nTotalRecords;
    int[]                   aryEventDone = new int[20];
    Runnable                procExchange;
    Handler                 timerExchange;
    float                   nCoinsChange;
    JSONObject              jObject;
    String                  strEventDate;
    Context                 context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CopyDatabaseIfNeed();

        LoadPreference();

        Init();

        context = this;
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            StartGetExchange();
        }
    }

    private void LoadPreference() {
        JSONSharedPreference jsonSetting = new JSONSharedPreference();
        strEventDate = getUTCDate();
        try{
            jObject = jsonSetting.loadJSONObject(this, "GW2Tools", "EVENTS");

        } catch (Exception e){
            e.printStackTrace();
        }

        if(jObject.isNull("DATE")){
            EventJson_Init();
        } else {
            String nowDate = getUTCDate();

            try{
               if(jObject.get("DATE").toString().equals(nowDate)){
                   JSONArray aryJson = jObject.getJSONArray("EVENTS");
                   for(int i = 0; i < aryJson.length(); i ++){
                       aryEventDone[i] = aryJson.getInt(i);
                   }
               } else {
                   EventJson_Init();
               }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void EventJson_Init(){
        try{
            jObject.put("DATE", strEventDate);
            JSONArray newEventDone = new JSONArray();
            for(int i =0; i < aryEventDone.length; i ++){
                aryEventDone[i] = 0;
                newEventDone.put(i, 0);
            }
            jObject.put("EVENTS", newEventDone);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void StartGetExchange() {
        timerExchange = new Handler();
        procExchange = new Runnable() {
            @Override
            public void run() {
                UpdateExchange();
                timerExchange.postDelayed(this, 1000*60*5);
            }
        };

        procExchange.run();
    }

    private void UpdateExchange(){
        nCoinsChange = 600000;
        ParseCoin(nCoinsChange);
        String url = "https://api.guildwars2.com/v2/commerce/exchange/coins?quantity=" + nCoinsChange;
        try{
            HttpClient      client = new DefaultHttpClient();
            HttpGet         get = new HttpGet(url);
            HttpResponse    response = client.execute(get);
            HttpEntity      resEntity = response.getEntity();
            if(resEntity != null){
                String result = EntityUtils.toString(resEntity);
                ExchangeParse(result);
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void ParseCoin(float nCoins){
        String strGold = String.format("%3d", (int)(nCoins/10000));
        String strSilver = String.format("%02d", (int)((nCoins % 10000)/100));
        String strCopper = String.format("%02d", (int)(nCoins % 100));

        TextView txtGold = (TextView) findViewById(R.id.txtGold);
        txtGold.setText(strGold);

        TextView txtSilver = (TextView) findViewById(R.id.txtSilver);
        txtSilver.setText(strSilver);

        TextView txtCopper = (TextView) findViewById(R.id.txtCopper);
        txtCopper.setText(strCopper);
    }

    private void ExchangeParse(String jsondata){
        try {
            Log.e("GEM_EXCHANGE", "exchange parse.");
            JSONObject json = new JSONObject(jsondata);
            String strGem = json.getString("quantity");

            TextView txtGem = (TextView) findViewById(R.id.txtGem);
            txtGem.setText(strGem);

        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private void Init() {
        db = new DBAdapter(this, "/data/data/" + this.getPackageName() + "/" + db_filename, db_version);
        db.OpenDB();


        String sql = "SELECT l._title, t._time, l._zone, l._area, l._link, l._tw, l._level, l._index FROM eventtime t " +
                "LEFT OUTER JOIN eventlist l ON l._index=t._index ORDER BY t._time";
        Cursor rs = db.getRecordBySQL(sql, new String[]{});

        if(rs.getCount() > 0){
            while(rs.moveToNext()){
                String strTime = UTC2LocalTime(rs.getString(1));
                long nTime = getTime(rs.getString(1));
                GW2Event item = new GW2Event(rs.getString(0), rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7));
                item.setnTimeValue(nTime);
                item.setEndTime(nTime + (60*15*1000));
                aryEvents.add(item);
                //Log.e("TIMECOUNT", "T:" + nTime + "     " + "O:" + strTime);

            }
        }

        db.CloseDB();

        nTotalRecords = aryEvents.size();
        tIndex = findTimeIndex();
        //Log.e(LOG_DBNAME, "Index:" + tIndex);

        adGW2 = new adList();
        rows = (ListView) findViewById(R.id.lvTime);
        rows.setAdapter(adGW2);
        rows.setOnItemClickListener(new ListViewItemClick());

        final Handler timerHandler = new Handler();
        Runnable reloadList = new Runnable() {
            @Override
            public void run() {
                adGW2.notifyDataSetChanged();
                timerHandler.postDelayed(this, 1000);
            }


        };

        TextView tvGem = (TextView) findViewById(R.id.textView2);
        tvGem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Log.e("GEM_EXCHANGE", "gem view click");
                UpdateExchange();
            }
        });
        reloadList.run();
    }

    public String getUTCDate(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf.format(new Date());
    }

    public String getUTCTime(){
        String strRet = "00:00";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date now = new Date();
        strRet = sdf.format(now);
        return strRet;
    }

    public long getTime(String strDatetime){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        long nRet = 0;
        try{
            Date dateTrans = sdf.parse(strDatetime);
            nRet = dateTrans.getTime();
        } catch (Exception e){
            e.printStackTrace();
        }

        return nRet;
    }

    public String UTC2LocalTime(String UTCTime){
        int gmtOffset = TimeZone.getDefault().getRawOffset();
        SimpleDateFormat sdf_utc = new SimpleDateFormat("HH:mm");

        long nTime = 0;
        try {
            Date utc_time = sdf_utc.parse(UTCTime);
            nTime = utc_time.getTime() + gmtOffset;
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return sdf_utc.format(new Date(nTime));
    }

    private void CopyDatabaseIfNeed() {
        String dstFilePath = "/data/data/" + this.getPackageName() + "/" + db_filename;
        File db_file = new File(dstFilePath);

        if(!db_file.exists()){
            try{
                CopyFile(db_filename, dstFilePath);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    void CopyFile(String assetsname, String dstFilename) throws IOException{
       InputStream m_inputstream = this.getAssets().open(assetsname);
        FileOutputStream m_outputstream = new FileOutputStream(dstFilename);
        byte[] buffer = new byte[1024];
        int readBytes = 0;

        while ((readBytes = m_inputstream.read(buffer)) != -1)
            m_outputstream.write(buffer, 0, readBytes);

        m_outputstream.flush();
        m_outputstream.close();
        m_inputstream.close();
    }

    public class adList extends ArrayAdapter<GW2Event>{
        public adList() {
            super(MainActivity.this, R.layout.rowlayout, aryEvents);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;

            if(itemView == null){
                itemView = getLayoutInflater().inflate(R.layout.rowlayout, parent, false);
            }

            long nNowTimeValue = getNowTimeValue();

            int nPosIndex = position + tIndex;
            if(nNowTimeValue > aryEvents.get(nPosIndex).getEndTime()){
                tIndex = findTimeIndex();
                nPosIndex = position + tIndex;
            }

            if(nPosIndex == nTotalRecords){
                nPosIndex = 0;
                tIndex = 0;
            }

            if(nPosIndex == position){
                strEventDate = getUTCDate();
                EventJson_Init();
            }

            String strTimeRun = DiffTimeCounter(nNowTimeValue, aryEvents.get(nPosIndex).getnTimeValue());
            int nValue = aryEventDone[aryEvents.get(nPosIndex).getnId()];

            TextView m_title  = (TextView) itemView.findViewById(R.id.txtTitle);
            if(nValue == 1){
                m_title.setText(aryEvents.get(nPosIndex).getStrTW() + "-已完成");
            } else {
                m_title.setText(aryEvents.get(nPosIndex).getStrTW());
            }

            if(strTimeRun == "進行中"){
                itemView.setBackgroundColor(Color.RED);
            } else if(nValue == 1){
                itemView.setBackgroundColor(Color.BLUE);
            } else {
                itemView.setBackgroundColor(Color.WHITE);
            }

            TextView m_time = (TextView) itemView.findViewById(R.id.txtTime);
            m_time.setText(strTimeRun);

            TextView m_zone = (TextView) itemView.findViewById(R.id.txtZone);
            m_zone.setText(aryEvents.get(nPosIndex).getStrZone());

            TextView m_area = (TextView) itemView.findViewById(R.id.txtArea);
            m_area.setText(aryEvents.get(nPosIndex).getStrArea());

            TextView m_level = (TextView) itemView.findViewById(R.id.txtLevel);
            m_level.setText("LV:" + aryEvents.get(nPosIndex).getStrLevel());

            TextView m_link = (TextView) itemView.findViewById(R.id.txtLink);
            m_link.setText(UTC2LocalTime(aryEvents.get(nPosIndex).getStrTimeStart()));

            return itemView;
        }

        @Override
        public int getCount() {
            return 14;
        }
    }

    public class ListViewItemClick implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int nIndex = tIndex + position;

            int nID = aryEvents.get(nIndex).getnId();
            int nValue = aryEventDone[nID];
            nValue ^= 0x01;
            aryEventDone[nID] = nValue;

            JSONObject newJson = parseSavingJson();
            JSONSharedPreference saveSetting = new JSONSharedPreference();
            saveSetting.saveJsonObject(context, "GW2Tools", "EVENTS", newJson);
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            return false;
        }
    }

    public JSONObject parseSavingJson(){
        JSONObject retObject = new JSONObject();
        try{
            retObject.put("DATE", strEventDate);
            JSONArray jEventDone = new JSONArray();
            for(int i = 0; i < aryEventDone.length; i ++){
                jEventDone.put(i, aryEventDone[i]);
            }
            retObject.put("EVENTS", jEventDone);
        } catch (Exception e){
            e.printStackTrace();
        }

        return retObject;
    }

    public long getNowTimeValue(){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String strNow = sdf.format(new Date());
        long nNowTimeValue = 0;

        try{
            nNowTimeValue = sdf.parse(strNow).getTime();
        } catch (Exception e){
            e.printStackTrace();
        }

        return nNowTimeValue;
    }

    public String DiffTimeCounter(long nTimeStart, long nTimeEnd){
        long nDiffTime = nTimeEnd - nTimeStart;
        String result = "";
        if(nDiffTime < 0) nDiffTime += (60*60*24*1000);
        if(nDiffTime < 3600000){
            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
            result = sdf.format(new Date(nDiffTime));
        } else if(nDiffTime > (60*60*23*1000)){
            result = "進行中";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            result = sdf.format(new Date(nDiffTime));
        }
        return result;
    }

    public int findTimeIndex(){
        int nTotal =  aryEvents.size();
        SimpleDateFormat nowformat = new SimpleDateFormat("HH:mm");
        nowformat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String strnow = nowformat.format(new Date());
        long nTime = 0;
        try {
            Date newDate = nowformat.parse(strnow);
            nTime = newDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int nIndex = 0;
        for(int i =0; i < nTotal; i ++){
           if(nTime < aryEvents.get(i).getEndTime()){
               nIndex = i;
               break;
           }
        }

        return nIndex;
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
            Log.e("MENU", "menu Settings");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
