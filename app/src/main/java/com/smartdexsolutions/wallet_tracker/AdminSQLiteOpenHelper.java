package com.smartdexsolutions.wallet_tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by alemo on 15/10/2017.
 */

public class AdminSQLiteOpenHelper extends SQLiteOpenHelper {

    public AdminSQLiteOpenHelper(Context context) {
        super(context, "mainDB.db", null, 5);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table Wallets(WalletAlias text primary key,WalletType text,Address text, Balance double default 0.0, BalanceUSD double default 0.0, BalanceEUR double default 0.0, BalanceBTC double default 0.0, CryptoImage text)");
        db.execSQL("create table WalletsType(WalletType text primary key,apiURL text,tokenString text,cryptoName text,divider double,cryptoSymbol text,cryptoImage text)");
        //insertWalletTypes(db);
    }
    private static final String DATABASE_UPDATE = "alter table Wallets rename to TempOldWallets";
    private static final String DATABASE_UPDATE2 = "create table Wallets (WalletAlias text primary key,WalletType text,Address text, Balance double default 0.0, BalanceUSD double default 0.0, BalanceEUR double default 0.0, BalanceBTC double default 0.0, CryptoImage text)";
    private static final String DATABASE_UPDATE3 = "insert into Wallets (WalletAlias, WalletType, Address) SELECT WalletAlias, WalletType, Address FROM TempOldWallets";
    private static final String DATABASE_UPDATE4 = "DROP TABLE TempOldWallets";

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5){
            db.execSQL(DATABASE_UPDATE);
            db.execSQL(DATABASE_UPDATE2);
            db.execSQL(DATABASE_UPDATE3);
            db.execSQL(DATABASE_UPDATE4);
        }

    }

    public void resetWalletTypes(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM WalletsType");
    }

    public void insertWalletTypes(ContentValues contentValues){
        SQLiteDatabase db = this.getWritableDatabase();
        db.insert("WalletsType",null,contentValues);

    }


    public boolean insertDataWallets(String walletAlias, String walletType, String address){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("WalletAlias", walletAlias);
        contentValues.put("WalletType", walletType);
        contentValues.put("Address", address);
        long result = db.insert("Wallets",null,contentValues);
        if(result==-1) return false;
        else return true;
    }

    public boolean updateWallets(String walletAlias, ContentValues contentUpdate){
        SQLiteDatabase db = this.getWritableDatabase();
        long result = db.update("Wallets", contentUpdate, "WalletAlias=?", new String[] { walletAlias });
        if(result==-1) return false;
        else return true;
    }

    public int deleteDataWallets(String walletAlias){
        SQLiteDatabase db = this.getWritableDatabase();
        int delete = db.delete("Wallets", "WalletAlias=?", new String[] { walletAlias });
        return delete;
    }

    public boolean checkAlias(String walletAlias){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from Wallets where WalletAlias=?",new String[] { walletAlias } );
        if(cursor.getCount() <= 0){
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
    }


    public Cursor getAllDataWallets(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.rawQuery("select * from Wallets", null);
        return data;
    }

    public Cursor getAllWalletTypes(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.rawQuery("select * from WalletsType", null);
        return data;
    }

    public Cursor getWalletsByAlias(String walletAlias){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.rawQuery("select WalletType,Address from Wallets where WalletAlias=?",new String[] { walletAlias } );
        return data;
    }

    public Cursor getWalletType(String walletType){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.rawQuery("select apiURL,tokenString,cryptoName,divider,cryptoSymbol,cryptoImage from WalletsType where WalletType=?", new String[] { walletType } );
        return data;
    }




}
