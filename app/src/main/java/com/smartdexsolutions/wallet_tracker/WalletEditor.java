package com.smartdexsolutions.wallet_tracker;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.smartdexsolutions.wallet_tracker.qrreader.BarcodeCaptureActivity;

import java.util.ArrayList;
import java.util.List;

import static android.widget.Toast.makeText;

public class WalletEditor extends AppCompatActivity {

    AdminSQLiteOpenHelper mainDb;
    String walletAlias;
    EditText etWAlias;
    EditText etWAddress;
    Spinner spWType;
    SpinnerAddCoinsAdapter adapter;
    List<String> allCoins;
    String coinType;
    String convString;

    private static final int RC_BARCODE_CAPTURE = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_editor);

        etWAlias = (EditText) findViewById(R.id.etWAlias);
        etWAddress = (EditText) findViewById(R.id.etWAddress);
        spWType = (Spinner) findViewById(R.id.spWType);


        Bundle bundle = getIntent().getExtras();
        walletAlias = bundle.getString("walletAlias");
        convString = bundle.getString("convString");

        if (convString.equals("mBTC")){
            convString = "BTC";
        }

        allCoins = new ArrayList<>();

        mainDb = new AdminSQLiteOpenHelper(this);

        Cursor walletTypes = mainDb.getAllWalletTypes();
        ArrayList<CoinSpinner> options = new ArrayList<>();
        if (walletTypes.getCount() != 0){
            while (walletTypes.moveToNext()) {
                String coinStr = "coin_" + walletTypes.getString(6);
                options.add(new CoinSpinner(walletTypes.getString(0), coinStr));
                allCoins.add(walletTypes.getString(0));
            }
        }
        adapter = new SpinnerAddCoinsAdapter(this,R.layout.spinner_item, R.id.customSpinnerItemTextView, options);
        spWType.setAdapter(adapter);


        walletInfo();


        String dataChart = "<!DOCTYPE html>\n" +
                "\n" +
                "<html>\n" +
                "<head>\n" +
                "</head>\n" +
                "<body>\n" +
                "<script type=\"text/javascript\">\n" +
                "baseUrl = \"https://widgets.cryptocompare.com/\";\n" +
                "var scripts = document.getElementsByTagName(\"script\");\n" +
                "var embedder = scripts[ scripts.length - 1 ];\n" +
                "var cccTheme = {\"General\":{\"borderColor\":\"#545454\",\"borderRadius\":\"4px 4px 0 0\"},\"Header\":{\"background\":\"#000\",\"color\":\"#FFF\",\"displayFollowers\":false},\"Followers\":{\"background\":\"#000\",\"color\":\"#000\",\"borderColor\":\"#000\",\"counterBorderColor\":\"#000\",\"counterColor\":\"#000\"},\"Data\":{\"infoLabelColor\":\"#545454\",\"infoValueColor\":\"#545454\"},\"Chart\":{\"fillColor\":\"#DFDFDF\",\"borderColor\":\"#000\"},\"Conversion\":{\"background\":\"#000\",\"lineHeight\":\"15px\"}};\n" +
                "(function (){\n" +
                "var appName = encodeURIComponent(window.location.hostname);\n" +
                "if(appName==\"\"){appName=\"local\";}\n" +
                "var s = document.createElement(\"script\");\n" +
                "s.type = \"text/javascript\";\n" +
                "s.async = true;\n" +
                "var theUrl = baseUrl+'serve/v1/coin/chart?fsym=" + coinType + "&tsym="+ convString + "';\n" +
                "s.src = theUrl + ( theUrl.indexOf(\"?\") >= 0 ? \"&\" : \"?\") + \"app=\" + appName;\n" +
                "embedder.parentNode.appendChild(s);\n" +
                "})();\n" +
                "</script>\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "</body></html>\n";

        WebView webCharts = (WebView)findViewById(R.id.web_charts);
        WebSettings webSettings = webCharts.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webCharts.setWebChromeClient(new WebChromeClient());
        webCharts.loadData(dataChart,"text/html", null);
        //webCharts.loadUrl("file:///android_asset/exchangecharts.html");


    }


    public void walletInfo() {

        Cursor data = mainDb.getWalletsByAlias(walletAlias);

        if (data.moveToFirst()) {
            etWAlias.setText(walletAlias);
            etWAddress.setText(data.getString(1));

            for (int i = 0; i < allCoins.size(); i++) {
                if (data.getString(0).trim().equals(allCoins.get(i))) {
                    spWType.setSelection(i);
                    Cursor walletType = mainDb.getWalletType(data.getString(0));
                    walletType.moveToFirst();
                    coinType=walletType.getString(4);
                    break;
                }
            }
        }
        else
            makeText(this, "The wallet type doesn't exist.", Toast.LENGTH_SHORT).show();
    }


    public void readQRCode(View v){
        // launch barcode activity.
        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);

        startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Toast.makeText(this, R.string.barcode_success, Toast.LENGTH_SHORT).show();
                    etWAddress.setText(barcode.displayValue);
                    Log.d("Barcode Status", "Barcode read: " + barcode.displayValue);
                } else {
                    Toast.makeText(this, R.string.barcode_failure, Toast.LENGTH_SHORT).show();
                    Log.d("Barcode Status", "No barcode captured, intent data is null");
                }
            } else {
                Toast.makeText(this, R.string.barcode_error, Toast.LENGTH_SHORT).show();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        MenuInflater inflater = getMenuInflater();
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        inflater.inflate(R.menu.walleteditormenu, menu);
        /* Return true so that the menu is displayed in the Toolbar */
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_updatewallet){
            ContentValues contentUpdate = new ContentValues();
            contentUpdate.put("WalletAlias", etWAlias.getText().toString());
            contentUpdate.put("WalletType", ((TextView) findViewById(R.id.customSpinnerItemTextView)).getText().toString());
            contentUpdate.put("Address", etWAddress.getText().toString());


            if(TextUtils.isEmpty(etWAlias.getText().toString())) {
                etWAlias.setError("This field can't be left empty.");
                return false;
            }
            else if (TextUtils.isEmpty(etWAddress.getText().toString())){
                etWAddress.setError("This field can't be left empty.");
                return false;
            }
            else if (mainDb.checkAlias(etWAlias.getText().toString()) && !(walletAlias.equals(etWAlias.getText().toString()))){
                etWAlias.setError("This wallet alias is already in use!");
                return false;
            }
            else{
                if(mainDb.updateWallets(walletAlias, contentUpdate)){
                    Toast.makeText(this, "Wallet successfully updated!", Toast.LENGTH_SHORT).show();
                    mainDb.close();
                    Intent intent = new Intent();
                    intent.putExtra("walletUpdated", "true");
                    setResult(RESULT_OK, intent);
                    finish();
                    return true;
                }
                else{
                    Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }

        if (id == R.id.action_deletewallet) {
            if (mainDb.deleteDataWallets(walletAlias) == 1) {
                makeText(this, "Wallet successfully deleted!", Toast.LENGTH_SHORT).show();
            }
            else{
                makeText(this, "The wallet was already deleted!", Toast.LENGTH_SHORT).show();
            }
            Intent intent = new Intent();
            intent.putExtra("walletDeleted", "true");
            setResult(RESULT_OK, intent);
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }


}
