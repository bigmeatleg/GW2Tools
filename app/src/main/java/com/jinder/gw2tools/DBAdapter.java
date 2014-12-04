package com.jinder.gw2tools;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Jinder on 14/12/2.
 */
public class DBAdapter {
    SQLiteDatabase      database;
    DBHelper            OpenHelper;
    int                 DBStatus;

    public DBAdapter(Context ctx, String db_name, int db_version) {
        OpenHelper = new DBHelper(ctx, db_name, null, db_version);
    }

    public class DBHelper extends SQLiteOpenHelper{
        public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    public DBAdapter OpenDB(){
        database = OpenHelper.getWritableDatabase();
        return this;
    }

    public void CloseDB(){
        OpenHelper.close();
    }

    public Cursor getRecordBySQL(String sql, String[] selectargs){
        return database.rawQuery(sql, selectargs);
    }

    public void execSQL(String sql){
        database.execSQL(sql);
    }
}
