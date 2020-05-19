package com.smartdexsolutions.wallet_tracker;

/**
 * Created by alemo on 15/10/2017.
 */

public class Wallet {
    private Integer walletColor;
    private String coinIcon;
    private String walletAlias;
    private String walletBalance;
    private String walletConversion;

    public Wallet(Integer walletColor, String coinIcon, String walletAlias, String walletBalance, String walletConversion) {
        this.walletColor = walletColor;
        this.coinIcon = coinIcon;
        this.walletAlias = walletAlias;
        this.walletBalance = walletBalance;
        this.walletConversion = walletConversion;
    }

    public Integer getWalletColor() {
        return walletColor;
    }

    public void setWalletColor(Integer walletColor) {
        this.walletColor = walletColor;
    }

    public String getCoinIcon() {
        return coinIcon;
    }

    public void setCoinIcon(String coinIcon) {
        this.coinIcon = coinIcon;
    }


    public String getWalletAlias() {
        return walletAlias;
    }

    public void setWalletAlias(String walletAlias) {
        this.walletAlias = walletAlias;
    }

    public String getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(String walletBalance) {
        this.walletBalance = walletBalance;
    }

    public String getWalletConversion() {
        return walletConversion;
    }

    public void setWalletConversion(String walletConversion) {
        this.walletConversion = walletConversion;
    }
}
