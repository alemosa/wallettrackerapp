package com.smartdexsolutions.wallet_tracker;

import com.github.omadahealth.lollipin.lib.managers.AppLockActivity;

/**
 * Created by alemo on 30/03/2018.
 */

public class CustomPinActivity extends AppLockActivity {
    @Override
    public void showForgotDialog() {
        //Launch your popup or anything you want here
    }

    @Override
    public void onPinFailure(int attempts) {

    }

    @Override
    public void onPinSuccess(int attempts) {

    }

    @Override
    public int getPinLength() {
        return super.getPinLength();//you can override this method to change the pin length from the default 4
    }


}
