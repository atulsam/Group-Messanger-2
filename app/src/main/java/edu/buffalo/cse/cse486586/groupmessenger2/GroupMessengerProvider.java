package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    FeedReaderDbHelper dbHelper;
    String TABLE_NAME = "pa2b";
    String COLUMN_NAME_TITLE = "key";
    String COLUMN_NAME_SUBTITLE = "value";

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        /*https://developer.android.com/training/data-storage/sqlit*/
        String str = "key = '"+selection+"'";
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletedRows = db.delete(TABLE_NAME, str, selectionArgs);
        Log.v("Deleted Rows: ",deletedRows+"");
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        try{
            String key = (String) values.get(COLUMN_NAME_TITLE);
            Cursor cur = query(uri,null,key,null,null);
            int numRows =0;
            if(cur !=null)
                numRows = cur.getCount();
            //System.out.println("****"+numRows+"***"+cur+"***"+key+"****"+uri);
            if(numRows > 0){
                update(uri,values,key,null);
            }else{
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                long newRowId = db.replaceOrThrow(TABLE_NAME, null, values);
                //Log.v("Inserted Row Id: ", newRowId+"");
            }
        }catch(Exception e){
            e.printStackTrace();
            //Log.v("Exception",""+e);
        }


        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        dbHelper = new FeedReaderDbHelper(getContext());
        //SQLiteDatabase db = dbHelper.getWritableDatabase();
        //int deletedRows = db.delete(TABLE_NAME, null, null);
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        String str = "key = '"+selection+"'";
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.update(TABLE_NAME, values, str, selectionArgs);
        //Log.v("update Row: ", count+"");
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */

        /*https://developer.android.com/training/data-storage/sqlit*/
        Cursor cursor = null;
        try{
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //Log.v("Query", selection+"~"+projection);
            String str = "key = '"+selection+"'";
            //System.out.println("********"+selection+"****"+uri+"******"+str);
            cursor = db.query(
                    TABLE_NAME,         // The table to query
                    projection,        // The array of columns to return (pass null to get all)
                    str,              // The columns for the WHERE clause
                    selectionArgs,   // The values for the WHERE clause
                    null,
                    null,
                    sortOrder      // The sort order
            );
            //Log.v("Query", selection+"~"+cursor.getCount());
        }catch(Exception e){
            e.printStackTrace();
            //Log.v("Exception", e+"");
        }

        return cursor;
    }




    /*https://developer.android.com/training/data-storage/sqlit*/

    public class FeedReaderDbHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;
        public static final String DATABASE_NAME = "FeedReader";
        private final String SQL_CREATE_ENTRIES ="CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ( " +
                COLUMN_NAME_TITLE+" TEXT," +
                COLUMN_NAME_SUBTITLE + " TEXT)";
        //private final String SQL_DELETE_ENTRIES ="DROP TABLE IF EXISTS " + TABLE_NAME;

        public FeedReaderDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);

        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}
