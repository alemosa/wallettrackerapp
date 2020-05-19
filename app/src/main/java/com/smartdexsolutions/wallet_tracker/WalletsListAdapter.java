package com.smartdexsolutions.wallet_tracker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

/**
 * Created by alemo on 15/10/2017.
 */

public class WalletsListAdapter extends ArrayAdapter<Wallet> {

    private static final String TAG = "WalletsListAdapter";

    private Context mContext;
    int mResource;


    public WalletsListAdapter(Context context, int resource, ArrayList<Wallet> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;


    }

    @NonNull
    @Override
    public View getView(int position,View convertView,ViewGroup parent) {

        Integer walletColor = getItem(position).getWalletColor();
        String coinIcon = getItem(position).getCoinIcon();
        String walletAlias = getItem(position).getWalletAlias();
        String walletBalance = getItem(position).getWalletBalance();
        String walletConversion = getItem(position).getWalletConversion();

        Wallet wallet = new Wallet(walletColor,coinIcon,walletAlias, walletBalance, walletConversion);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);

        View vWalletColor = (View) convertView.findViewById(R.id.walletColorCode);
        ImageView ivCoinIcon = (ImageView) convertView.findViewById(R.id.ivCoinIcon);
        TextView tvWalletAlias = (TextView) convertView.findViewById(R.id.tvWalletAlias);
        TextView tvBalance = (TextView) convertView.findViewById(R.id.tvBalance);
        TextView tvConversion = (TextView) convertView.findViewById(R.id.tvConversion);



        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .resetViewBeforeLoading(false)  // default
                .delayBeforeLoading(0)
                .cacheInMemory(true) // default
                .cacheOnDisk(true) // default
		        .considerExifParams(false) // default
                .build();




        ImageLoader imageLoader = ImageLoader.getInstance();
        String imageUri = "https://smartdexsolutions.net/wallettracker/coinbase/images/coins/" + coinIcon + ".png";

        vWalletColor.setBackgroundColor(mContext.getResources().getColor(walletColor));
        imageLoader.displayImage(imageUri, ivCoinIcon, options);
        tvWalletAlias.setText(walletAlias);
        tvBalance.setText(walletBalance);
        tvConversion.setText(walletConversion);

        return convertView;
    }


}
