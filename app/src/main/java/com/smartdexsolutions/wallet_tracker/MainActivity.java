package com.smartdexsolutions.wallet_tracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.omadahealth.lollipin.lib.PinCompatActivity;
import com.github.omadahealth.lollipin.lib.managers.AppLock;
import com.github.omadahealth.lollipin.lib.managers.LockManager;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;
import com.smartdexsolutions.wallet_tracker.util.IabHelper;
import com.smartdexsolutions.wallet_tracker.util.IabResult;
import com.smartdexsolutions.wallet_tracker.util.Inventory;
import com.smartdexsolutions.wallet_tracker.util.Purchase;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends PinCompatActivity implements PurchaseInterface {

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
    TextView tvETHPrice;
    TextView tvBTCPrice;
    View vWalletColor;

    PieChart pieChart;

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
    int[] walletColors = { R.color.darkred, R.color.darkgreen, R.color.darkblue, R.color.darkyellow, R.color.darkorange, R.color.purple, R.color.darkgrey,  R.color.greenblue};
    boolean resetCounter;
    double eurConv;
    double usdConv;
    long today;
    long lastUpdate;
    //private UpdateWalletsTask updateTask = null;
    private UpdateCoinTypes updateCoinTypesTask = null;




    boolean pincodeEnabled = false;

    Integer delayWallets = 0;
    Integer delayListView = 0;

    RequestQueue queue;


    NumberFormat formatter = NumberFormat.getInstance();

    int numberDecimals = 2;

    //PIN Management
    private static final int REQUEST_CODE_ENABLE = 11;
    private static final int REQUEST_FIRST_RUN_PIN = 12;


    //Purchases
    Boolean adsRemoved = false;
    static final int RC_REQUEST = 10001;
    IabHelper mHelper;
    String TAG = "Purchases";
    String payload = "wtdefpayloadstring990044";
    static final String SKU_REMOVE_ADS = "remove_ads";
    Activity purchaseActivity;
    private PurchaseInterface purchaseInterface;
    boolean iabReady = false;

    //ADS
    private InterstitialAd mInterstitialAd;



    List<String> walletsAliasList;
    /*public static RequestQueue requestQueue;*/

    AdminSQLiteOpenHelper mainDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainDb = new AdminSQLiteOpenHelper(this);

        formatter.setMaximumFractionDigits(4);
        formatter.setMinimumFractionDigits(2);

        purchaseInterface = (PurchaseInterface) this;






        //init imageloader
        File cacheDir = StorageUtils.getCacheDirectory(MainActivity.this);
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(MainActivity.this)
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
                .imageDownloader(new BaseImageDownloader(MainActivity.this)) // default
                .defaultDisplayImageOptions(DisplayImageOptions.createSimple()) // default
                .writeDebugLogs()
                .build();
        ImageLoader.getInstance().init(config);



        //Get user settings
        SharedPreferences settings = getSharedPreferences("settings", Context.MODE_PRIVATE);
        convString = settings.getString("convString", "EUR");
        convTag = settings.getString("convTag", "€");
        numberDecimals = settings.getInt("numberDecimals", 2);
        pincodeEnabled = settings.getBoolean("pincodeEnabled", false);
        adsRemoved = settings.getBoolean("adsRemoved", false);



        //Purchases
        String base64EncodedPublicKey;

        base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAg3Ys3oEhsrp/xly8aojnA3xgqmXngVaH/DltTYX9Pd+SZqGb84ONjZHHPHDIBU/Q51owP5mhrsAKwrPx7a3cNSRCKdZQhVcwJ6ZyRV2huJNokTfkf2pb4cGcJxoWDHeooGN2VOJWAwdl1r5gRrocfsgb2BecIxaV1tT7xdmAqhmrXXcNPMFCvsLTyxLP4QSbcI7reLfoFC0jzzwDHofiEHnnYwpgsDj/6h57yCbgQbrFN4d6u3LSJbglQQACsQCXMPgmXl1Jlj8mIeD6DfIZY9sZdxxzz5dMJwohm7LngMhDCRSjU9op0LJfX3HjIm7fUF74wxZMNCcG0R/cQRPDAQIDAQAB";

        // compute your public key and store it in base64EncodedPublicKey
        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    Log.d(TAG, "Problem setting up in-app billing: " + result);
                    return;
                }else
                {
                    iabReady = true;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }
            }
        });

    // Listener that's called when we finish querying the items and subscriptions we own


        //PIN management
        LockManager lockManager = LockManager.getInstance();
        if(pincodeEnabled){
            lockManager.enableAppLock(this, CustomPinActivity.class);
            lockManager.getAppLock().setLogoId(R.drawable.ic_wt_locked_lite);
            lockManager.getAppLock().setShouldShowForgot(false);
            lockManager.getAppLock().setTimeout(120 * 1000);
        }
        else
            lockManager.disableAppLock();





        //ADS
        if(!adsRemoved) {
            MobileAds.initialize(this, "ca-app-pub-4423602901888526~9390836418");
            mAdView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);

            mInterstitialAd = new InterstitialAd(this);
            mInterstitialAd.setAdUnitId("ca-app-pub-4423602901888526/4917395080");
            mInterstitialAd.loadAd(new AdRequest.Builder().build());

            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    // Load the next interstitial.
                    mInterstitialAd.loadAd(new AdRequest.Builder().build());
                }

            });
        }

        //Initializations
        resetCounter = false;
        tvTotalBalance = (TextView) findViewById(R.id.tvTotalBalance);
        tv24Balance = (TextView) findViewById(R.id.tvTotal24Balance);
        tvETHPrice = (TextView) findViewById(R.id.tvETHPrice);
        tvBTCPrice = (TextView) findViewById(R.id.tvBTCPrice);
        lvWallets = (ListView) findViewById(R.id.lvWallets);
        pieChart = (PieChart) findViewById(R.id.piechart);
        vWalletColor = (View) findViewById(R.id.walletColorCode);
        walletsList = new ArrayList<>();
        lastKnownBalance = new ArrayList<>();
        lastKnownConversion = new ArrayList<>();
        walletsAliasList = new ArrayList<>();
        //mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        walletsAdapter = new WalletsListAdapter(this, R.layout.adapter_lvwallets, walletsList);
        lvWallets.setAdapter(walletsAdapter);
        View footerView =  ((LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.lvwallets_footer, null, false);
        lvWallets.addFooterView(footerView, null, false);

        //Piechart Config
        pieChart.setUsePercentValues(false);
        pieChart.setNoDataText("Please, add a wallet");
        pieChart.getLegend().setEnabled(false);
        pieChart.setDescription(null);
        pieChart.setDrawSliceText(false);
        pieChart.setHoleRadius(92);
        pieChart.setCenterTextRadiusPercent(95);
        pieChart.animateXY(3000, 3000);







        queue = Volley.newRequestQueue(this);

        updateCoinTypes();


        /*mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                        updatelvWallets();
            }
        });*/





        lvWallets.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent walletEditorActivity = new Intent(MainActivity.this, WalletEditor.class);
                walletEditorActivity.putExtra("walletAlias", walletsAliasList.get(position).toString());
                walletEditorActivity.putExtra("convString", convString);
                if(!adsRemoved) {
                    if (mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                    } else {
                        Log.d("TAG", "The interstitial wasn't loaded yet.");
                    }
                }
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

    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                Log.d(TAG, "Failed to query inventory.");
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the ads removed?
            if(!adsRemoved){
                Purchase removeAdsPurchase = inventory.getPurchase(SKU_REMOVE_ADS);
                adsRemoved = (removeAdsPurchase != null && verifyDeveloperPayload(removeAdsPurchase));
                if(adsRemoved){
                    removeAds();
                }
            }


            Log.d(TAG, "User has " + (adsRemoved ? "Removed ads" : "Not removed ads"));


            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };


    @Override
    public void purchaseSomething(){
        purchaseRemoveAds();
    }

    public void purchaseRemoveAds() {
        purchaseActivity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                try {
                    mHelper.launchPurchaseFlow(purchaseActivity, SKU_REMOVE_ADS,
                            RC_REQUEST, mPurchaseFinishedListener, payload);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct.
         * It will be the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase
         * and verifying it here might seem like a good approach, but this will
         * fail in the case where the user purchases an item on one device and
         * then uses your app on a different device, because on the other device
         * you will not have access to the random string you originally
         * generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different
         * between them, so that one user's purchase can't be replayed to
         * another user.
         *
         * 2. The payload must be such that you can verify it even when the app
         * wasn't the one who initiated the purchase flow (so that items
         * purchased by the user on one device work on other devices owned by
         * the user).
         *
         * Using your own server to store and verify developer payloads across
         * app installations is recommended.
         */
        return true;
    }

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_REMOVE_ADS)) {
                Log.d(TAG, "Remove Ads Purchased!");
                removeAds();
            }
        }
    };

    private void removeAds() {
        SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("adsRemoved", true);
        editor.commit();
    }

    void complain(String message) {
        Log.e(TAG, "**** WalletTracker Error: " + message);
        alert("Error: " + message);
    }

    void alert(final String message) {

        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();

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
                updateCoinTypes();
                updateListView();
            }
        }
    }



    //Handlers for refresh
    final Handler refreshHandler = new Handler();
    Runnable refreshWallets = new Runnable() {
        @Override
        public void run() {
            updateWallets();
            refreshHandler.postDelayed(this, delayWallets * 1000);
        }
    };
    Runnable refreshListView = new Runnable() {
        @Override
        public void run() {
            updateListView();
            refreshHandler.postDelayed(this, delayListView * 1000);
        }
    };



    protected void onResume(){
        super.onResume();
        refreshHandler.removeCallbacksAndMessages(null);
        delayWallets = 10;
        delayListView = 5;
        refreshHandler.post(refreshWallets);
        refreshHandler.post(refreshListView);
        //lastKnownBalance.clear();
        //lastKnownConversion.clear();

    }

    protected void onPause(){
        super.onPause();
        refreshHandler.removeCallbacksAndMessages(null);
        delayWallets = 300;
        refreshHandler.post(refreshWallets);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHelper != null) try {
            mHelper.dispose();
        } catch (IabHelper.IabAsyncInProgressException e) {
            e.printStackTrace();
        }
        mHelper = null;
    }


    public void startAddWalletActivity(){
        Intent addWalletActivity = new Intent(MainActivity.this, AddWallet.class);
        startActivityForResult(addWalletActivity, addWalletResultCode);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == addWalletResultCode) {
            if (resultCode == RESULT_OK) {
                updateWallets();
                updateListView();
            }
        }
        if (requestCode == walletEditorResultCode) {
            if (resultCode == RESULT_OK) {
                updateWallets();
                updateListView();
            }
        }
        if (requestCode == 1001) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    Toast.makeText(MainActivity.this, "You have bought the " + sku + ". Please restart the app to complete the ads removal!", Toast.LENGTH_SHORT).show();
                }
                catch (JSONException e) {
                    Toast.makeText(MainActivity.this, "Purchase failed!", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }

    }


    public void updateCoinTypes(){
        if (updateCoinTypesTask == null) {
            updateCoinTypesTask = new UpdateCoinTypes();
            updateCoinTypesTask.execute();
        }
    }


    public void updateListView() {
        walletsList.clear();
        walletsAliasList.clear();
        totalConvBalance = 0.0;
        walletsAdapter.notifyDataSetChanged();
        pieChart.invalidate();
        String balance = "0.0";
        Double totalConvBalanceEUR = 0.0;
        Integer colorCount = 0;

        ArrayList<PieEntry> yvalues = new ArrayList<>();

        Cursor allWallets = mainDb.getAllDataWallets();
        if (allWallets.getCount() == 0) {
            //Toast.makeText(MainActivity.this, "No wallets available!", Toast.LENGTH_SHORT).show();
        } else {
            while (allWallets.moveToNext()) {
                String walletAlias = allWallets.getString(0);
                balance = allWallets.getString(3);
                String balanceEUR = allWallets.getString(5);
                String coinStr = "coin_" + allWallets.getString(7);

                SharedPreferences settings = getSharedPreferences("settings", Context.MODE_PRIVATE);
                //SharedPreferences.Editor editor = settings.edit();



                BigDecimal balanceD = new BigDecimal(Double.valueOf(balance));
                balanceD = balanceD.setScale(numberDecimals, RoundingMode.HALF_UP);
                String balanceSTR = formatter.format(balanceD);

                if (convString.equals("EUR")){
                    convBalance = Double.valueOf(balanceEUR);
                    yvalues.add(new PieEntry(convBalance.floatValue(), walletAlias));
                }
                else if(convString.equals("USD")){
                    convBalance = Double.valueOf(balanceEUR) * (double) settings.getFloat("USD", 0.0f);
                    yvalues.add(new PieEntry(convBalance.floatValue(), walletAlias));
                }
                else if(convString.equals("GBP")){
                    convBalance = Double.valueOf(balanceEUR) * (double) settings.getFloat("GBP", 0.0f);
                    yvalues.add(new PieEntry(convBalance.floatValue(), walletAlias));
                }
                else if(convString.equals("CNY")){
                    convBalance = Double.valueOf(balanceEUR) * (double) settings.getFloat("CNY", 0.0f);
                    yvalues.add(new PieEntry(convBalance.floatValue(), walletAlias));
                }
                else if(convString.equals("RUB")){
                    convBalance = Double.valueOf(balanceEUR) * (double) settings.getFloat("RUB", 0.0f);
                    yvalues.add(new PieEntry(convBalance.floatValue(), walletAlias));
                }
                else if(convString.equals("mBTC")){
                    convBalance = Double.valueOf(balanceEUR) * (double) settings.getFloat("BTC", 0.0f) * 1000;
                    yvalues.add(new PieEntry(convBalance.floatValue(), walletAlias));
                }



                BigDecimal convBalanceD = new BigDecimal(convBalance);
                convBalanceD = convBalanceD.setScale(2, RoundingMode.HALF_UP);
                String convBalanceSTR = formatter.format(convBalanceD);

                totalConvBalance = totalConvBalance + convBalance;
                totalConvBalanceEUR = totalConvBalanceEUR + Double.valueOf(balanceEUR);
                //totalConvBalanceUSD = totalConvBalanceUSD + Double.valueOf(balanceUSD);
                //totalConvBalanceBTC = totalConvBalanceBTC + (Double.valueOf(balanceBTC)*1000);

                //Wallet color
                int wColor = walletColors[colorCount];

                Wallet newWallet = new Wallet(wColor, coinStr, walletAlias, balanceSTR, convBalanceSTR + convTag);
                walletsAliasList.add(walletAlias);
                walletsList.add(newWallet);

                colorCount++;
                if (colorCount==7)
                        colorCount=0;
            }



            SharedPreferences settings = getSharedPreferences("settings", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();



            walletsAdapter.notifyDataSetChanged();

            if (convString.equals("EUR"))
                totalConvBalance = totalConvBalanceEUR;
            else if(convString.equals("USD"))
                totalConvBalance = totalConvBalanceEUR * (double) settings.getFloat("USD", 0.0f);
            else if(convString.equals("GBP"))
                totalConvBalance = totalConvBalanceEUR * (double) settings.getFloat("GBP", 0.0f);
            else if(convString.equals("CNY"))
                totalConvBalance = totalConvBalanceEUR * (double) settings.getFloat("CNY", 0.0f);
            else if(convString.equals("RUB"))
                totalConvBalance = totalConvBalanceEUR * (double) settings.getFloat("RUB", 0.0f);
            else if(convString.equals("mBTC"))
                totalConvBalance = totalConvBalanceEUR * (double) settings.getFloat("BTC", 0.0f) * 1000;



            BigDecimal totalBalanceD = new BigDecimal(totalConvBalance);
            totalBalanceD = totalBalanceD.setScale(2, RoundingMode.HALF_UP);
            String totalBalanceSTR = formatter.format(totalBalanceD);

            SpannableString styledBalance = new SpannableString(totalBalanceSTR + convTag);

            styledBalance.setSpan(new StyleSpan(Typeface.BOLD),0,styledBalance.length(),0);
            styledBalance.setSpan(new RelativeSizeSpan(1.4f),0,styledBalance.length(),0);




            //PieChart data
            PieDataSet dataSet = new PieDataSet(yvalues, "Wallet balances");
            PieData data = new PieData(dataSet);
            dataSet.setColors(walletColors, this);
            dataSet.setValueTextSize(8f);
            dataSet.setDrawValues(false);
            data.setValueTextColor(Color.WHITE);
            pieChart.setEntryLabelColor(Color.WHITE);
            pieChart.setEntryLabelTextSize(7f);
            pieChart.setData(data);
            pieChart.setCenterText(styledBalance);



            Double balance24Hour = 0.0;
            Double balance24Result = 0.0;
            Double percent24Result = 0.0;
            Double priceBTC = 0.0;
            Double priceETH = 0.0;

            today = System.currentTimeMillis();
            lastUpdate = settings.getLong("lastupdate24", System.currentTimeMillis());

            if (((today - lastUpdate) > (1000 * 60 * 60 * 24)) || today == lastUpdate || resetCounter) {
                editor.putFloat("balance24eur", totalConvBalanceEUR.floatValue());
                //editor.putFloat("balance24usd", totalConvBalanceUSD.floatValue());
                //editor.putFloat("balance24btc", totalConvBalanceBTC.floatValue());
                editor.putLong("lastupdate24", System.currentTimeMillis());
                editor.putString("bCurrency", convString);
                editor.commit();
                resetCounter = false;
            }

            if (convString.equals("EUR")){
                balance24Hour = (double) settings.getFloat("balance24eur", 0.0f);
                priceBTC = 1/((double) settings.getFloat("BTC", 1.0f));
                priceETH = 1/((double) settings.getFloat("ETH", 1.0f));}
            else if (convString.equals("USD")){
                balance24Hour = (double) settings.getFloat("balance24eur", 0.0f) * (double) settings.getFloat("USD", 0.0f);
                priceBTC =  ((double) settings.getFloat("USD", 0.0f)) * (1/((double) settings.getFloat("BTC", 1.0f)));
                priceETH = ((double) settings.getFloat("USD", 0.0f)) * (1/((double) settings.getFloat("ETH", 1.0f)));}
            else if (convString.equals("GBP")){
                balance24Hour = (double) settings.getFloat("balance24eur", 0.0f) * (double) settings.getFloat("GBP", 0.0f);
                priceBTC =  ((double) settings.getFloat("GBP", 0.0f)) * (1/((double) settings.getFloat("BTC", 1.0f)));
                priceETH = ((double) settings.getFloat("GBP", 0.0f)) * (1/((double) settings.getFloat("ETH", 1.0f)));}
            else if (convString.equals("CNY")){
                balance24Hour = (double) settings.getFloat("balance24eur", 0.0f) * (double) settings.getFloat("CNY", 0.0f);
                priceBTC =  ((double) settings.getFloat("CNY", 0.0f)) * (1/((double) settings.getFloat("BTC", 1.0f)));
                priceETH = ((double) settings.getFloat("CNY", 0.0f)) * (1/((double) settings.getFloat("ETH", 1.0f)));}
            else if (convString.equals("RUB")){
                balance24Hour = (double) settings.getFloat("balance24eur", 0.0f) * (double) settings.getFloat("RUB", 0.0f);
                priceBTC =  ((double) settings.getFloat("RUB", 0.0f)) * (1/((double) settings.getFloat("BTC", 1.0f)));
                priceETH = ((double) settings.getFloat("RUB", 0.0f)) * (1/((double) settings.getFloat("ETH", 1.0f)));}
            else if (convString.equals("mBTC")) {
                balance24Hour = ((double) settings.getFloat("balance24eur", 0.0f) * (double) settings.getFloat("BTC", 1.0f)) * 1000;
                priceBTC = 1000.0;
                priceETH = ((double) settings.getFloat("BTC", 0.0f)) * (1/((double) settings.getFloat("ETH", 1.0f))) * 1000;}
            balance24Result = totalConvBalance - balance24Hour;
            if(totalConvBalance != 0){
                percent24Result = (balance24Result/totalConvBalance)* 100;
            }
            else
                percent24Result=0.0;


            BigDecimal priceBTCD = new BigDecimal(priceBTC);
            BigDecimal priceETHD = new BigDecimal(priceETH);
            priceBTCD = priceBTCD.setScale(2,RoundingMode.HALF_UP);
            priceETHD = priceETHD.setScale(2,RoundingMode.HALF_UP);
            String priceBTCSTR = formatter.format(priceBTCD);
            String priceETHSTR = formatter.format(priceETHD);
            tvBTCPrice.setText(priceBTCSTR + convTag);
            tvETHPrice.setText(priceETHSTR + convTag);


            BigDecimal balance24ResultD = new BigDecimal(balance24Result);
            balance24ResultD = balance24ResultD.setScale(2, RoundingMode.HALF_UP);
            String balance24STR = formatter.format(balance24ResultD);

            BigDecimal percent24ResultD = new BigDecimal(percent24Result);
            percent24ResultD = percent24ResultD.setScale(2, RoundingMode.HALF_UP);
            String percent24STR = formatter.format(percent24ResultD);


            if (balance24Result > 0.00) {
                tv24Balance.setText("+" + balance24STR + convTag);
                percent24STR = "+" + percent24STR + "%";
                tv24Balance.setTextColor(Color.parseColor("#00800E"));
                tvTotalBalance.setTextColor(Color.parseColor("#00800E"));
            } else if (balance24Result < 0.00) {
                tv24Balance.setText(balance24STR + convTag);
                percent24STR = percent24STR + "%";
                tv24Balance.setTextColor(Color.parseColor("#B70000"));
                tvTotalBalance.setTextColor(Color.parseColor("#B70000"));
            } else
                tv24Balance.setTextColor(Color.parseColor("#000000"));
            tvTotalBalance.setText(percent24STR);
        }
    }

    public void updateWallets() {
        String apiURL;
        String address;
        String tokenString;
        String cryptoName;
        Double divider;
        String cryptoImage;
        String cryptoSymbol;

        Cursor allData = mainDb.getAllDataWallets();
        if (allData.getCount() == 0){
            //Toast.makeText(MainActivity.this, "No wallets available!", Toast.LENGTH_SHORT).show();
        }
        else {
            while (allData.moveToNext()) {
                String walletAlias = allData.getString(0);
                String walletTypeName = allData.getString(1);
                Cursor walletType = mainDb.getWalletType(walletTypeName);
                if (walletType.moveToFirst()) {
                    apiURL = walletType.getString(0);
                    //if (walletTypeName.equals("LBRY"))
                    //    apiURL = apiURL + "/balance";
                    address = allData.getString(2);
                    tokenString = walletType.getString(1);
                    cryptoName = walletType.getString(2);
                    divider = walletType.getDouble(3);
                    String strDivider = divider.toString();
                    cryptoImage = walletType.getString(5);
                    cryptoSymbol = walletType.getString(4);
                    apiRequest(walletAlias, address, apiURL, tokenString, strDivider, cryptoName, cryptoImage, cryptoSymbol);
                }

            }


        }
    }


    public void apiRequest(final String walletAlias , final String address, final String apiURL, final String tokenString, final String divider, final String cryptoName, final String cryptoImage, final String cryptoSymbol){
        String url = "https://smartdexsolutions.net/wallettracker/api/v3/";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Address:", address);
                        Log.d("ApiURL:", apiURL);
                        Log.d("tokenString:", tokenString);
                        Log.d("divider", divider);
                        Log.d("cryptoName", cryptoName);
                        Log.d("cryptoSymbol", cryptoSymbol);



                        Log.d("Response", response);


                        try {
                            JSONObject c = new JSONObject(response);
                            String balance = c.getString("balance");
                            String balanceEUR = c.getString("balanceEUR");
                            Double BTC = c.getDouble("BTC");
                            Double ETH = c.getDouble("ETH");
                            Double USD = c.getDouble("USD");
                            Double GBP = c.getDouble("GBP");
                            Double CNY = c.getDouble("CNY");
                            Double RUB = c.getDouble("RUB");

                            ContentValues contentValues = new ContentValues();
                            contentValues.put("Balance", Double.valueOf(balance));
                            contentValues.put("BalanceEUR", Double.valueOf(balanceEUR));
                            contentValues.put("CryptoImage", cryptoImage);
                            mainDb.updateWallets(walletAlias, contentValues);

                            //Update Bitcoin/Ethereum prices
                            SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putFloat("BTC", BTC.floatValue());
                            editor.putFloat("ETH", ETH.floatValue());
                            editor.putFloat("USD", USD.floatValue());
                            editor.putFloat("GBP", GBP.floatValue());
                            editor.putFloat("CNY", CNY.floatValue());
                            editor.putFloat("RUB", RUB.floatValue());
                            editor.commit();


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", "Request failed");
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("address", address);
                params.put("apiURL", apiURL);
                params.put("tokenString", tokenString);
                params.put("divider", divider);
                params.put("cryptoName", cryptoName);
                params.put("cryptoSymbol", cryptoSymbol);

                return params;
            }
        };
        queue.add(postRequest);
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
                        updateListView();
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

        if (pincodeEnabled){
            MenuItem item = menu.findItem(R.id.action_pinconfig);
            item.setTitle("Disable PIN");
        }
        else{
            MenuItem item = menu.findItem(R.id.action_pinconfig);
            item.setTitle("Enable PIN");
        }


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

        if(id == R.id.action_decimal2){
            SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("numberDecimals", 2);
            editor.commit();
            numberDecimals = 2;
            updateListView();
            return true;
        }

        if(id == R.id.action_decimal3){
            SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("numberDecimals", 3);
            editor.commit();
            numberDecimals = 3;
            updateListView();
            return true;
        }

        if(id == R.id.action_decimal4){
            SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("numberDecimals", 4);
            editor.commit();
            numberDecimals = 4;
            updateListView();
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
                editor.putString("convTag", convTag);
                editor.putString("convString", convString);
                editor.commit();

                updateListView();
        }

            return true;
        }

        if(id == R.id.action_currency_usd){
            if(convString != "USD") {
                convTag = "$";
                convString = "USD";
                SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("convTag", convTag);
                editor.putString("convString", convString);
                editor.commit();

                updateListView();
            }
            return true;
        }

        if(id == R.id.action_currency_gbp){
            if(convString != "GBP") {
                convTag = "£";
                convString = "GBP";
                SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("convTag", convTag);
                editor.putString("convString", convString);
                editor.commit();

                updateListView();
            }
            return true;
        }
        if(id == R.id.action_currency_cny){
            if(convString != "CNY") {
                convTag = "¥";
                convString = "CNY";
                SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("convTag", convTag);
                editor.putString("convString", convString);
                editor.commit();

                updateListView();
            }
            return true;
        }

        if(id == R.id.action_currency_rub){
            if(convString != "RUB") {
                convTag = "\u20BD";
                convString = "RUB";
                SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("convTag", convTag);
                editor.putString("convString", convString);
                editor.commit();

                updateListView();
            }
            return true;
        }

        if(id == R.id.action_currency_btc){
            if(convString != "mBTC"){
                convTag = "mB";
                convString = "mBTC";
                SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("convTag", convTag);
                editor.putString("convString", convString);
                editor.commit();

                updateListView();
            }

            return true;
        }


        if(id == R.id.action_setpin){

            Intent intent = new Intent(MainActivity.this, CustomPinActivity.class);
            intent.putExtra(AppLock.EXTRA_TYPE, AppLock.ENABLE_PINLOCK);
            startActivityForResult(intent, REQUEST_CODE_ENABLE);



            return true;
        }

        if(id == R.id.action_pinconfig){
            SharedPreferences settings = getSharedPreferences("settings",Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();

            boolean pincodeEnabled = settings.getBoolean("pincodeEnabled", false);

            if (pincodeEnabled){
                editor.putBoolean ("pincodeEnabled", false);
                LockManager lockManager = LockManager.getInstance();
                lockManager.disableAppLock();

                item.setTitle("Enable PIN");
                editor.commit();
            }
            else{
                editor.putBoolean ("pincodeEnabled", true);
                LockManager lockManager = LockManager.getInstance();
                lockManager.enableAppLock(this, CustomPinActivity.class);
                if (!lockManager.getAppLock().isPasscodeSet()) {
                    Intent intent = new Intent(this, CustomPinActivity.class);
                    intent.putExtra(AppLock.EXTRA_TYPE, AppLock.ENABLE_PINLOCK);
                    startActivityForResult(intent, REQUEST_FIRST_RUN_PIN);
                }
                item.setTitle("Disable PIN");
                editor.commit();
            }


            return true;
        }


        if(id == R.id.action_removeads){
            if(!adsRemoved){
                if(iabReady) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {

                            Log.d(TAG, "Launching purchase flow for Ads Removal.");
                            try {
                                mHelper.launchPurchaseFlow(MainActivity.this, SKU_REMOVE_ADS,
                                        RC_REQUEST, mPurchaseFinishedListener, payload);
                            } catch (IabHelper.IabAsyncInProgressException e) {
                                e.printStackTrace();
                            }
                        }
                    });}
                else{
                    Log.d(TAG, "IAB not ready, can't launch.");
                }
            }
            else{
                Toast.makeText(MainActivity.this, "Ads already removed!", Toast.LENGTH_SHORT).show();
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
                URL url = new URL("https://smartdexsolutions.net/wallettracker/coinbase/getdata.php");
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
                Toast.makeText(MainActivity.this, "WT Coinbase updated!", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(MainActivity.this, "Wallet Tracker couldn't connect to server.", Toast.LENGTH_SHORT).show();
            }
            updateCoinTypesTask = null;

        }


    }


}
