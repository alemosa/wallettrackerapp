package com.smartdexsolutions.wallet_tracker;

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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.smartdexsolutions.wallet_tracker.qrreader.BarcodeCaptureActivity;

import java.util.ArrayList;

public class AddWallet extends AppCompatActivity {

    private EditText etWalletAlias, etAddress;
    private Spinner spWalletType;
    private ImageView ivQRreader;
    AdminSQLiteOpenHelper mainDb;

    private static final int RC_BARCODE_CAPTURE = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_wallet);

        mainDb = new AdminSQLiteOpenHelper(this);

        etWalletAlias = (EditText)findViewById(R.id.etWAlias);
        etAddress = (EditText)findViewById(R.id.etAddress);
        spWalletType = (Spinner)findViewById(R.id.spWalletType);
        ivQRreader = (ImageView)findViewById(R.id.ivQRreader);


        ImageButton lvAddWalletBtn = (ImageButton) findViewById(R.id.btn_add_wallet);
        lvAddWalletBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveWallet();
            }
        });



        Cursor walletTypes = mainDb.getAllWalletTypes();
        ArrayList<CoinSpinner> options = new ArrayList<>();
        if (walletTypes.getCount() != 0){
            while (walletTypes.moveToNext()) {
                String coinStr = "coin_" + walletTypes.getString(6);
                options.add(new CoinSpinner(walletTypes.getString(0), coinStr));
            }
        }
        SpinnerAddCoinsAdapter adapter = new SpinnerAddCoinsAdapter(this,R.layout.spinner_item, R.id.customSpinnerItemTextView, options);
        spWalletType.setAdapter(adapter);
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
                    etAddress.setText(barcode.displayValue);
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



    public boolean saveWallet(){
        String walletAlias = etWalletAlias.getText().toString();
        String walletType = ((TextView) findViewById(R.id.customSpinnerItemTextView)).getText().toString();
        Log.i("Wallet Type", walletType);
        String address = etAddress.getText().toString();

        if(TextUtils.isEmpty(walletAlias)) {
            etWalletAlias.setError("This field can't be left empty.");
            return false;
        }
        else if (TextUtils.isEmpty(address)){
            etAddress.setError("This field can't be left empty.");
            return false;
        }
        else {
            boolean isInserted = mainDb.insertDataWallets(walletAlias, walletType, address);
            if (isInserted) {
                Toast.makeText(this, "Wallet saved successfully!", Toast.LENGTH_SHORT).show();
                mainDb.close();
                Intent intent = new Intent();
                intent.putExtra("walletAdded", "true");
                setResult(RESULT_OK, intent);
                finish();
            } else
                etWalletAlias.setError("This wallet alias is already in use!");
            return true;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        MenuInflater inflater = getMenuInflater();
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        inflater.inflate(R.menu.addwalletmenu, menu);
        /* Return true so that the menu is displayed in the Toolbar */
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_savewallet){
            return saveWallet();
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }



}
