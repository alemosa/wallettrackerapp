package com.smartdexsolutions.wallet_tracker;

/**
 * Created by alemo on 01/11/2017.
 */

public class CoinSpinner {
    private String coinIcon;
    private String coinType;


    public CoinSpinner(String coinType, String coinIcon) {
        this.coinIcon = coinIcon;
        this.coinType = coinType;

    }

    public String getCoinIcon() {
        return coinIcon;
    }

    public void setCoinIcon(String coinIcon) { this.coinIcon = coinIcon; }

    public String getCoinType() {
        return coinType;
    }

    public void setCoinType(String coinType) { this.coinType = coinType; }


}
