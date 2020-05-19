package com.smartdexsolutions.wallet_tracker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.jayway.jsonpath.JsonPath;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;


public class MainActivityOld extends AppCompatActivity {

    private static final int addWalletResultCode = 15;
    private static final int walletEditorResultCode = 25;

    private AdView mAdView;
    private ListView lvWallets;
    private ArrayList<Wallet> walletsList;
    private ArrayList<Double> lastKnownBalance;
    private ArrayList<Double> lastKnownConversion;
    private WalletsListAdapter walletsAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    public ProgressDialog pd;

    TextView tvTotalBalance;
    TextView tv24Balance;


    Double balance = 0.0;
    String apiURL = "";
    String token = "";
    Double divider = 0.0;
    String cryptoName = "";
    String cryptoImage="";
    Double convVal = 0.0;
    Double convBalance = 0.0;
    Double totalConvBalance = 0.0;
    String convString = "";
    String convTag;
    boolean resetCounter;
    double eurConv;
    double usdConv;
    long today;
    long lastUpdate;
    private UpdateWalletsTask updateTask = null;
    private UpdateCoinTypes updateCoinTypesTask = null;


    NumberFormat formatter = NumberFormat.getInstance();





    List<String> walletsAliasList;
    /*public static RequestQueue requestQueue;*/

    AdminSQLiteOpenHelper mainDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainDb = new AdminSQLiteOpenHelper(this);

        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(2);


        //init imageloader
        File cacheDir = StorageUtils.getCacheDirectory(MainActivityOld.this);
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(MainActivityOld.this)
                .threadPoolSize(3) // default
                .threadPriority(Thread.NORM_PRIORITY - 2) // default
                .tasksProcessingOrder(QueueProcessingType.FIFO) // default
                .denyCacheImageMultipleSizesInMemory()
                .memoryCache(new LruMemoryCache(2 * 1024 * 1024))
                .memoryCacheSize(2 * 1024 * 1024)
                .memoryCacheSizePercentage(13) // default
                .diskCache(new UnlimitedDiskCache(cacheDir)) // default
                .diskCacheSize(50 * 1024 * 1024)
                .diskCacheFileCount(1000)
                .diskCacheFileNameGenerator(new HashCodeFileNameGenerator()) // default
                .imageDownloader(new BaseImageDownloader(MainActivityOld.this)) // default
                .defaultDisplayImageOptions(DisplayImageOptions.createSimple()) // default
                .writeDebugLogs()
                .build();
        ImageLoader.getInstance().init(config);



        //Get user settings
        SharedPreferences settings = getSharedPreferences("settings", Context.MODE_PRIVATE);
        convString = settings.getString("convString", "EUR");
        convTag = settings.getString("convTag", "€");

        //ADS
        MobileAds.initialize(this, "ca-app-pub-4423602901888526~9390836418");
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //Initializations
        resetCounter = false;
        tvTotalBalance = (TextView) findViewById(R.id.tvTotalBalance);
        tv24Balance = (TextView) findViewById(R.id.tvTotal24Balance);
        lvWallets = (ListView) findViewById(R.id.lvWallets);
        walletsList = new ArrayList<>();
        lastKnownBalance = new ArrayList<>();
        lastKnownConversion = new ArrayList<>();
        walletsAliasList = new ArrayList<>();
        //mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        walletsAdapter = new WalletsListAdapter(this, R.layout.adapter_lvwallets, walletsList);
        lvWallets.setAdapter(walletsAdapter);
        View footerView =  ((LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.lvwallets_footer, null, false);
        lvWallets.addFooterView(footerView, null, false);



        updateCoinTypes();



        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                        updatelvWallets();
            }
        });

        updatelvWallets();



        lvWallets.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent walletEditorActivity = new Intent(MainActivityOld.this, WalletEditor.class);
                walletEditorActivity.putExtra("walletAlias", walletsAliasList.get(position).toString());
                startActivityForResult(walletEditorActivity, walletEditorResultCode);
            }
        });

        ImageButton lvAddMoreBtn = (ImageButton) findViewById(R.id.btn_add_more);
        lvAddMoreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAddWalletActivity();
            }
        });


    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    protected boolean wasLaunchedFromRecents() {
        return (getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY;
    }
    protected void onStart(){
        super.onStart();
        if (wasLaunchedFromRecents()) {
            if (isNetworkAvailable()) {
                updatelvWallets();
            }
        }
    }

    protected void onResume(){
        super.onResume();
        //lastKnownBalance.clear();
        //lastKnownConversion.clear();


    }

    public void startAddWalletActivity(){
        Intent addWalletActivity = new Intent(MainActivityOld.this, AddWallet.class);
        startActivityForResult(addWalletActivity, addWalletResultCode);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == addWalletResultCode) {
            if (resultCode == RESULT_OK) {
                    updatelvWallets();
            }
        }
        if (requestCode == walletEditorResultCode) {
            if (resultCode == RESULT_OK) {
                updatelvWallets();
            }
        }

    }




    public void updateCoinTypes(){
        if (updateCoinTypesTask == null) {
            updateCoinTypesTask = new UpdateCoinTypes();
            updateCoinTypesTask.execute();
        }
    }



    public void updatelvWallets() {
        if (updateTask == null) {
            updateTask = new UpdateWalletsTask();
            updateTask.execute();
        }
    }


    public class UpdateWalletsTask extends AsyncTask<Void, Void, Boolean> {

        protected void onPreExecute(){

            mSwipeRefreshLayout.setRefreshing(true);
            walletsList.clear();
            walletsAliasList.clear();
            walletsAdapter.notifyDataSetChanged();

            /*pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();*/

        }

        @Override
        protected Boolean doInBackground(Void... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            HttpURLConnection connection2= null;
            BufferedReader reader2 = null;
            totalConvBalance = 0.0;
            int i = -1;



            Cursor allData = mainDb.getAllDataWallets();
            if (allData.getCount() == 0)
                return false;
            else {

                while (allData.moveToNext()) {
                    String walletTypeName = allData.getString(1);
                    Cursor walletType = mainDb.getWalletType(walletTypeName);
                    if (walletType.moveToFirst()) {
                        i++;
                        apiURL = walletType.getString(0) + allData.getString(2);
                        if(walletTypeName.equals("LBRY"))
                            apiURL = apiURL + "/balance";
                        token = walletType.getString(1);
                        cryptoName = walletType.getString(2);
                        divider = walletType.getDouble(3);
                        cryptoImage = walletType.getString(5);
                    }
                    //connection to get wallet balance
                    try {
                        URL url = new URL(apiURL);
                        connection =  (HttpURLConnection) url.openConnection();
                        connection.connect();
                        InputStream stream = connection.getInputStream();
                        reader = new BufferedReader(new InputStreamReader(stream));
                        StringBuffer buffer = new StringBuffer();
                        String line = "";
                        while ((line = reader.readLine()) != null) {
                            buffer.append(line+"\n");
                            Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)
                        }
                        List<Object> list = JsonPath.parse(buffer.toString()).read(token);
                        if(list.size() == 0){
                            balance = 0.0;
                        }
                        else {
                            Double balanceDoub = Double.valueOf(list.get(0).toString());
                            balance = balanceDoub / divider;
                            Log.i("Balance:", balance.toString());
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        /*if (i < lastKnownBalance.size())
                            balance = lastKnownBalance.get(i+1);
                        else*/
                        balance=0.0;
                    } catch (IOException e) {
                        e.printStackTrace();
                        balance=0.0;
                    } finally {
                        if (connection != null) {
                            connection.disconnect();
                        }
                        try {
                            if (reader != null) {
                                reader.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }


                    //connection to get wallet conversion to eur/usd

                    try {
                        URL url2 = new URL("https://api.coinmarketcap.com/v1/ticker/" + cryptoName + "?convert=EUR");
                        connection2 = (HttpURLConnection) url2.openConnection();
                        connection2.connect();
                        InputStream stream2 = connection2.getInputStream();
                        reader2 = new BufferedReader(new InputStreamReader(stream2));
                        StringBuffer buffer2 = new StringBuffer();
                        String line2 = "";
                        while ((line2 = reader2.readLine()) != null) {
                            buffer2.append(line2+"\n");
                            Log.d("Response: ", "> " + line2);   //here u ll get whole response...... :-)
                        }
                        String bufferResult = "{'table':" + buffer2.toString() + "}";
                        List<String> eurList = JsonPath.parse(bufferResult).read("$..price_eur");
                        List<String> usdList = JsonPath.parse(bufferResult).read("$..price_usd");
                        eurConv = Double.valueOf(eurList.get(0).toString())/Double.valueOf(usdList.get(0).toString());
                        usdConv = Double.valueOf(usdList.get(0).toString())/Double.valueOf(eurList.get(0).toString());
                        if (convString.equals("EUR"))
                        convVal = Double.valueOf(eurList.get(0).toString());
                        else
                        convVal = Double.valueOf(usdList.get(0).toString());
                        Log.i("Balance:", convVal.toString());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        convVal = 0.0;
                    } catch (IOException e) {
                        e.printStackTrace();
                        convVal = 0.0;
                    } finally {
                        if (connection2 != null) {
                            connection2.disconnect();
                        }
                        try {
                            if (reader2 != null) {
                                reader2.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    /*lastKnownBalance.add(i,balance);
                    lastKnownConversion.add(i,convVal);*/


                    //balance = Double.parseDouble(new DecimalFormat("##.##").format(balance));
                    BigDecimal balanceD = new BigDecimal(balance);
                    balanceD = balanceD.setScale(2, RoundingMode.HALF_UP);
                    String balanceSTR = formatter.format(balanceD);

                    convBalance = balance*convVal;
                    totalConvBalance = totalConvBalance + convBalance;
                    BigDecimal convBalanceD = new BigDecimal(convBalance);
                    convBalanceD = convBalanceD.setScale(2, RoundingMode.HALF_UP);
                    String convBalanceSTR = formatter.format(convBalanceD);

                    Integer wColor = 0;

                    //int coinID = getResources().getIdentifier("coin_" + cryptoImage, "drawable", getPackageName());
                    String coinStr = "coin_" + cryptoImage;

                    Wallet newWallet = new Wallet(wColor,coinStr,allData.getString(0), balanceSTR, convBalanceSTR + convTag);
                    walletsAliasList.add(allData.getString(0));
                    walletsList.add(newWallet);





                }

                mainDb.close();
                return true;

            }






        }

        protected void onPostExecute(Boolean walletsAv) {

            SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();


            walletsAdapter.notifyDataSetChanged();
            BigDecimal totalBalanceD = new BigDecimal(totalConvBalance);
            totalBalanceD = totalBalanceD.setScale(2, RoundingMode.HALF_UP);
            String totalBalanceSTR = formatter.format(totalBalanceD);
            Double balance24Hour = 0.0;
            Double balance24Result = 0.0;


            today = System.currentTimeMillis();
            lastUpdate =  settings.getLong("lastupdate24", System.currentTimeMillis());

            if(((today - lastUpdate) > (1000*60*60*24)) || today == lastUpdate || resetCounter){
                if(convString.equals("EUR")) {
                    editor.putFloat("balance24eur", totalBalanceD.floatValue());
                    editor.putFloat("balance24usd", (totalBalanceD.floatValue())*(float)usdConv);
                }
                else {
                    editor.putFloat("balance24usd", totalBalanceD.floatValue());
                    editor.putFloat("balance24eur", (totalBalanceD.floatValue())*(float)eurConv);
                }
                editor.putLong("lastupdate24", System.currentTimeMillis());
                editor.putString("bCurrency", convString);
                editor.commit();
                resetCounter = false;
            }

            if(convString.equals("EUR"))
                balance24Hour = (double) settings.getFloat("balance24eur", 0.0f);
            else
                balance24Hour = (double) settings.getFloat("balance24usd", 0.0f);
            balance24Result = totalConvBalance - balance24Hour;

            BigDecimal balance24ResultD = new BigDecimal(balance24Result);
            balance24ResultD = balance24ResultD.setScale(2, RoundingMode.HALF_UP);
            String balance24STR = formatter.format(balance24ResultD);
            if(balance24Result > 0.00) {
                tv24Balance.setText("+" + balance24STR + convTag);
                tv24Balance.setTextColor(Color.parseColor("#00800E"));
            }
            else if(balance24Result < 0.00){
                tv24Balance.setText(balance24STR + convTag);
                tv24Balance.setTextColor(Color.parseColor("#B70000"));
            }
            else
                tv24Balance.setTextColor(Color.parseColor("#000000"));
            tvTotalBalance.setText(totalBalanceSTR + convTag);
            if(!walletsAv){
                Toast.makeText(MainActivityOld.this, "No wallets available!", Toast.LENGTH_SHORT).show();
            }
            mSwipeRefreshLayout.setRefreshing(false);
            updateTask = null;




            /*if (pd.isShowing()){
                pd.dismiss();
            }*/
        }

    }


    public void mResetCounter(View v){

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Reset change-counter")
                .setMessage("Are you sure you want to reset the change-counter?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resetCounter = true;
                        updatelvWallets();
                    }
                })
                .show();
        Button buttonPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        buttonPositive.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        Button buttonNegative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        buttonNegative.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        MenuInflater inflater = getMenuInflater();
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        inflater.inflate(R.menu.mainmenu, menu);
        /* Return true so that the menu is displayed in the Toolbar */
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_addwallet){
            startAddWalletActivity();
            return true;
        }

        if(id == R.id.action_updatecoins){
            updateCoinTypes();
            return true;
        }

        if(id == R.id.action_about){
            Intent aboutActivity = new Intent(this, About.class);
            startActivity(aboutActivity);
            return true;
        }

        if(id == R.id.action_currency_eur){
            if(convString != "EUR"){
                convTag = "€";
                convString = "EUR";
                SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("convTag", "€");
                editor.putString("convString", "EUR");
                editor.commit();

                updatelvWallets();
        }

            return true;
        }

        if(id == R.id.action_currency_usd){
            if(convString != "USD") {
                convTag = "$";
                convString = "USD";
                SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("convTag", "$");
                editor.putString("convString", "USD");
                editor.commit();

                updatelvWallets();
            }
            return true;
        }





        return super.onOptionsItemSelected(item);
    }

    public class UpdateCoinTypes extends AsyncTask<Void, Void, Boolean>{


        @Override
        protected Boolean doInBackground(Void... voids) {
            URLConnection connection = null;
            BufferedReader reader = null;
            String jsonStr = null;


            try {
                URL url = new URL("http://smartdexsolutions.net/wallettracker/coinbase/getdata.php");
                connection =  url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");

                }


                jsonStr = buffer.toString();


                Log.d("JSON Response: ", "> " + jsonStr);

                if (jsonStr != null) {

                    //Now that we received the data, reset the internal db
                    mainDb.resetWalletTypes();


                    // Getting JSON Array node
                    JSONArray coinTypes = new JSONArray(jsonStr);

                    // looping through All Wallet Types
                    for (int i = 0; i < coinTypes.length(); i++) {
                        JSONObject c = coinTypes.getJSONObject(i);

                        String walletType = c.getString("WalletType");
                        String apiURL = c.getString("apiURL");
                        String tokenString = c.getString("tokenString");
                        String cryptoName = c.getString("cryptoName");
                        Double divider = c.getDouble("divider");
                        String cryptoSymbol = c.getString("cryptoSymbol");
                        String cryptoImage = c.getString("cryptoImage");

                        ContentValues contentValues = new ContentValues();

                        contentValues.put("WalletType", walletType);
                        contentValues.put("apiURL", apiURL);
                        contentValues.put("tokenString", tokenString);
                        contentValues.put("cryptoName", cryptoName);
                        contentValues.put("divider", divider);
                        contentValues.put("cryptoSymbol", cryptoSymbol);
                        contentValues.put("cryptoImage", cryptoImage);
                        mainDb.insertWalletTypes(contentValues);


                    }
                    return true;


                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return false;



        }

        protected void onPostExecute(Boolean coinTypesUpdated) {
            if(coinTypesUpdated) {
                Toast.makeText(MainActivityOld.this, "WT Coinbase updated!", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(MainActivityOld.this, "Wallet Tracker couldn't connect to server.", Toast.LENGTH_SHORT).show();
            }
            updateCoinTypesTask = null;

        }


    }






}
