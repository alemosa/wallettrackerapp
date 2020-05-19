package com.smartdexsolutions.wallet_tracker;

import android.app.Activity;
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
 * Created by alemo on 01/11/2017.
 */

public class SpinnerAddCoinsAdapter extends ArrayAdapter<CoinSpinner> {

    int groupid;
    Activity context;
    ArrayList<CoinSpinner> coinList;
    LayoutInflater inflater;

    public SpinnerAddCoinsAdapter(Activity context, int groupid, int id, ArrayList<CoinSpinner> coinList) {
        super(context, id, coinList);
        this.coinList=coinList;
        inflater=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.groupid=groupid;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View itemView=inflater.inflate(groupid,parent,false);

        ImageView ivCoinIcon = (ImageView) itemView.findViewById(R.id.customSpinnerItemImageView);
        TextView tvCoinType = (TextView) itemView.findViewById(R.id.customSpinnerItemTextView);

        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .resetViewBeforeLoading(false)  // default
                .delayBeforeLoading(0)
                .cacheInMemory(true) // default
                .cacheOnDisk(true) // default
                .considerExifParams(false) // default
                .build();




        ImageLoader imageLoader = ImageLoader.getInstance();
        String imageUri = "https://smartdexsolutions.net/wallettracker/coinbase/images/coins/" + coinList.get(position).getCoinIcon() + ".png";


        imageLoader.displayImage(imageUri, ivCoinIcon, options);
        tvCoinType.setText(coinList.get(position).getCoinType());

        return itemView;
    }

    public View getDropDownView(int position, View convertView, ViewGroup parent){
        return getView(position,convertView,parent);
    }

}
