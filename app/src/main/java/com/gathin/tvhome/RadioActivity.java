package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.*;
import android.text.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class RadioActivity extends Activity {
    private MediaPlayer player;
    private TextView status;
    private LinearLayout list;
    private final ArrayList<String[]> stations = new ArrayList<>();
    private String filter = "";
    private static final String API = "https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/MY?hidebroken=true&order=votes&reverse=true&limit=500";
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setPadding(28,20,28,16); root.setBackgroundColor(0xff0c1322);
        TextView title = new TextView(this); title.setText("♫  LIVE RADIO MALAYSIA"); title.setTextSize(26); title.setTextColor(-1); root.addView(title);
        status = new TextView(this); status.setText("Loading Malaysian radio stations…"); status.setTextColor(0xffb7c8e3); status.setTextSize(16); root.addView(status);
        EditText search = new EditText(this); search.setSingleLine(true); search.setHint("Search radio station"); search.setTextColor(-1); search.setHintTextColor(0xffaab7c8); root.addView(search);
        search.addTextChangedListener(new TextWatcher(){ public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int before,int count){filter=s.toString().toLowerCase(Locale.ROOT); render();} public void afterTextChanged(Editable e){} });
        ScrollView scroll = new ScrollView(this); list = new LinearLayout(this); list.setOrientation(1); scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Button stop = new Button(this); stop.setText("■ STOP RADIO"); stop.setOnClickListener(v -> {stopPlayer();status.setText("Stopped");}); root.addView(stop);
        setContentView(root); load();
    }
    private void load(){ new Thread(() -> { ArrayList<String[]> found = new ArrayList<>(); try {
        HttpURLConnection c=(HttpURLConnection)new URL(API).openConnection(); c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","GlassTV/3.0");
        try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
        JSONArray arr=new JSONArray(out.toString("UTF-8"));HashSet<String> seen=new HashSet<>();
        for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i);String name=o.optString("name").trim();String url=o.optString("url_resolved",o.optString("url")).trim();if(name.isEmpty()||url.isEmpty()||!url.startsWith("https://"))continue;String key=name.toLowerCase(Locale.ROOT);if(seen.add(key))found.add(new String[]{name,url});}}
        }catch(Exception ignored){}
        runOnUiThread(()->{stations.clear();stations.addAll(found);status.setText(found.isEmpty()?"Station directory unavailable. Check internet and reopen Radio.":found.size()+" Malaysia stations available • Select to play");render();});
    }).start(); }
    private void render(){if(list==null)return;list.removeAllViews();int count=0;for(String[] station:stations){if(!station[0].toLowerCase(Locale.ROOT).contains(filter))continue;Button b=new Button(this);b.setText("▶  "+station[0]);b.setTextColor(-1);b.setTextSize(17);b.setAllCaps(false);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff243850));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,64);p.bottomMargin=8;list.addView(b,p);b.setOnClickListener(v->play(station));if(++count>=250)break;}}
    private void play(String[] s){stopPlayer();status.setText("Connecting to "+s[0]+"…");try{MediaPlayer mp=new MediaPlayer();player=mp;mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());mp.setDataSource(s[1]);mp.setOnPreparedListener(p->{if(player==p){p.start();status.setText("▶ LIVE • "+s[0]);}});mp.setOnErrorListener((p,w,e)->{if(player==p)status.setText("Stream unavailable: "+s[0]);return true;});mp.prepareAsync();}catch(Exception e){stopPlayer();status.setText("Unable to play "+s[0]);}}
    private void stopPlayer(){if(player!=null){try{player.stop();}catch(Exception ignored){}player.release();player=null;}}
    @Override protected void onDestroy(){stopPlayer();super.onDestroy();}
}
