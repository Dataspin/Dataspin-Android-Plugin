package io.dataspin.analyticsSDK;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.Log;

import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

import static java.lang.Integer.*;

/**
 * Created by rafal on 22.03.15.
 */
public class DataspinBacklog extends SQLiteOpenHelper {

    final class DataspinTaskContract implements BaseColumns {
        // To prevent someone from accidentally instantiating the contract class,
        // give it an empty constructor.
        public DataspinTaskContract() {}

        public static final String TABLE_NAME = "backlog_tasks";
        public static final String COLUMN_NAME_TASKID = "taskid";
        public static final String COLUMN_NAME_URL = "url";
        public static final String COLUMN_NAME_JSON = "json_data";
        public static final String COLUMN_NAME_DATASPIN_METHOD = "ds_method";
        public static final String COLUMN_NAME_HTTP_METHOD = "http_method";

        private static final String TEXT_TYPE = " TEXT";
        private static final String INTEGER_TYPE = " INTEGER";
        private static final String COMMA_SEP = ",";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + DataspinTaskContract.TABLE_NAME + " (" +
                        DataspinTaskContract.COLUMN_NAME_TASKID + " INTEGER PRIMARY KEY," +
                        DataspinTaskContract.COLUMN_NAME_URL + TEXT_TYPE + COMMA_SEP +
                        DataspinTaskContract.COLUMN_NAME_JSON + TEXT_TYPE + COMMA_SEP +
                        DataspinTaskContract.COLUMN_NAME_DATASPIN_METHOD + INTEGER_TYPE + COMMA_SEP +
                        DataspinTaskContract.COLUMN_NAME_HTTP_METHOD + INTEGER_TYPE +

                        " )";

        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + DataspinTaskContract.TABLE_NAME;
    }


    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "backlog.db";

    public DataspinBacklog(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DataspinTaskContract.SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DataspinTaskContract.TABLE_NAME);

        // Create tables again
        onCreate(db);
    }

    public void AddTask(DataspinConnection task) {
        Log.i("DataspinBacklog", "Adding new task on backlog: "+task.toString());
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            task.json.put("session_id", (DataspinManager.Instance().isSessionStarted == true) ?
                    DataspinManager.Instance().session_id :
                    DataspinManager.Instance().offline_session_id);

            ContentValues values = new ContentValues();
            values.put(DataspinTaskContract.COLUMN_NAME_URL, task.url);
            values.put(DataspinTaskContract.COLUMN_NAME_JSON, task.json.toString());
            values.put(DataspinTaskContract.COLUMN_NAME_DATASPIN_METHOD, task.dataspinMethod.methodCode);
            values.put(DataspinTaskContract.COLUMN_NAME_HTTP_METHOD, task.httpMethod.methodCode);

            // Inserting Row
            db.insert(DataspinTaskContract.TABLE_NAME, null, values);
            db.close(); // Closing database connection

            Log.i("DataspinBacklog", "Task added!");
        }
        catch(Exception e) {
            Log.w("DataspinBacklog", "Error while writing Offline request! Message: "+e.getMessage());
            e.printStackTrace();
        }
    }

    private void UpdateSessionTaskTime() {
        final Handler mHandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(10000);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
            }
        }).start();
    }

    public void DeleteTask(DataspinBacklogTask task) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DataspinTaskContract.TABLE_NAME, DataspinTaskContract.COLUMN_NAME_TASKID + " = ?",
                new String[] { String.valueOf(task.task_id) });
        db.close();
    }

    public void DeleteTask(int task_id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DataspinTaskContract.TABLE_NAME, DataspinTaskContract.COLUMN_NAME_TASKID + " = ?",
                new String[] { String.valueOf(task_id) });
        db.close();
    }

    public int UpdateTask(DataspinConnection task) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DataspinTaskContract.COLUMN_NAME_URL, task.url);
        values.put(DataspinTaskContract.COLUMN_NAME_JSON, task.json.toString());
        values.put(DataspinTaskContract.COLUMN_NAME_DATASPIN_METHOD, task.dataspinMethod.methodCode);
        values.put(DataspinTaskContract.COLUMN_NAME_HTTP_METHOD, task.httpMethod.methodCode);

        // updating row
        return db.update(DataspinTaskContract.TABLE_NAME, values, DataspinTaskContract.COLUMN_NAME_TASKID + " = ?",
                new String[] { String.valueOf(task.backlogTaskId) });
    }

    public void UpdateTasksWithSessionId(String session_id, int true_session_id) {
        LinkedList<DataspinConnection> tasks = GetAllTasks();
        try {
            for (DataspinConnection c : tasks) {
                if (c.json.getString("session_id") == session_id) {
                    c.json.put("session_id", String.valueOf(true_session_id));
                    UpdateTask(c);
                }
            }
        }
        catch (Exception e) {

        }
    }

    public LinkedList<DataspinConnection> GetAllTasks() {
        Log.i("DataspinBacklog","Getting tasks list...");
        LinkedList<DataspinConnection> taskList = new LinkedList<DataspinConnection>();

        String selectQuery = "SELECT  * FROM " + DataspinTaskContract.TABLE_NAME;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                try {
                    DataspinConnection dataspinConnection = new DataspinConnection(
                            DataspinMethod.values()[parseInt(cursor.getString(3))],
                            HttpMethod.values()[parseInt(cursor.getString(4))],
                            new JSONObject(cursor.getString(2)),
                            cursor.getString(1),
                            parseInt(cursor.getString(0))
                    );

                    taskList.add(dataspinConnection);
                }
                catch (Exception e) {
                    DataspinManager.Instance().AddError(new DataspinError(ErrorType.JSON_PROCESSING_ERROR, "Failed to parse task from backlog!", e));
                }

            } while (cursor.moveToNext());
        }
        Log.i("DataspinBacklog","Tasks retrieved!");
        return taskList;
    }
}
