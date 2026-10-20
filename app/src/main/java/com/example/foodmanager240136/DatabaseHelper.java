
package com.example.foodmanager240136;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Thong tin database
    private static final String DATABASE_NAME =
            "BIT240136_FoodManager.db";
    private static final int DATABASE_VERSION = 2;

    // Ten cac bang co tien to MSSV
    public static final String TABLE_CATEGORY =
            "BIT240136_DishCategory";
    public static final String TABLE_DISH =
            "BIT240136_Dish";
    public static final String TABLE_IMAGE =
            "BIT240136_DishImage";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // 1. Tao bang loai mon an
        String createCategory =
                "CREATE TABLE " + TABLE_CATEGORY + " (" +
                        "BIT240136_Id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "BIT240136_Name TEXT NOT NULL " +
                        "CHECK(length(trim(BIT240136_Name)) > 0), " +
                        "BIT240136_Description TEXT" +
                        ")";

        db.execSQL(createCategory);

        // 2. Tao bang mon an
        String createDish =
                "CREATE TABLE " + TABLE_DISH + " (" +
                        "BIT240136_Id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "BIT240136_Name TEXT NOT NULL COLLATE NOCASE " +
                        "CHECK(length(trim(BIT240136_Name)) > 0), " +
                        "BIT240136_Price REAL NOT NULL " +
                        "CHECK(BIT240136_Price > 0), " +
                        "BIT240136_PreparationTime INTEGER NOT NULL " +
                        "CHECK(BIT240136_PreparationTime > 0), " +
                        "BIT240136_IsAvailable INTEGER NOT NULL DEFAULT 1 " +
                        "CHECK(BIT240136_IsAvailable IN (0,1)), " +
                        "BIT240136_Description TEXT, " +
                        "BIT240136_DishCategoryId INTEGER NOT NULL, " +
                        "FOREIGN KEY(BIT240136_DishCategoryId) " +
                        "REFERENCES " + TABLE_CATEGORY +
                        "(BIT240136_Id), " +
                        "UNIQUE(BIT240136_DishCategoryId, BIT240136_Name)" +
                        ")";

        db.execSQL(createDish);

        // 3. Tao bang hinh anh mon an
        String createImage =
                "CREATE TABLE " + TABLE_IMAGE + " (" +
                        "BIT240136_Id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "BIT240136_DishId INTEGER NOT NULL, " +
                        "BIT240136_ImageUrl TEXT NOT NULL " +
                        "CHECK(length(trim(BIT240136_ImageUrl)) > 0), " +
                        "BIT240136_IsThumbnail INTEGER NOT NULL DEFAULT 0 " +
                        "CHECK(BIT240136_IsThumbnail IN (0,1)), " +
                        "FOREIGN KEY(BIT240136_DishId) " +
                        "REFERENCES " + TABLE_DISH +
                        "(BIT240136_Id) ON DELETE CASCADE" +
                        ")";

        db.execSQL(createImage);

        // Moi mon co toi da 1 anh dai dien
        db.execSQL(
                "CREATE UNIQUE INDEX BIT240136_OneThumbnail " +
                        "ON " + TABLE_IMAGE + "(BIT240136_DishId) " +
                        "WHERE BIT240136_IsThumbnail = 1"
        );

        // Theo doi moi thay doi SQLite de dong bo khi co mang
        setupSyncTables(db);

        // Them du lieu mau
        insertSampleData(db);
    }

    // Them du lieu 3 loai mon an
    private void insertSampleData(SQLiteDatabase db) {

        db.execSQL("INSERT INTO " + TABLE_CATEGORY +
                " VALUES (1, 'Main Dishes', 'Mon chinh')");

        db.execSQL("INSERT INTO " + TABLE_CATEGORY +
                " VALUES (2, 'Drinks', 'Do uong')");

        db.execSQL("INSERT INTO " + TABLE_CATEGORY +
                " VALUES (3, 'Desserts', 'Trang mieng')");

        // Them 5 mon an mau
        insertDish(db, "Fried Rice", 45000, 15, 1);
        insertDish(db, "Beef Noodles", 55000, 20, 1);
        insertDish(db, "Milk Tea", 30000, 10, 2);
        insertDish(db, "Orange Juice", 25000, 5, 2);
        insertDish(db, "Flan Cake", 20000, 10, 3);
    }

    // Ham them mon an
    private void insertDish(SQLiteDatabase db,
                            String name,
                            double price,
                            int preparationTime,
                            int categoryId) {

        ContentValues values = new ContentValues();

        values.put("BIT240136_Name", name);
        values.put("BIT240136_Price", price);
        values.put("BIT240136_PreparationTime", preparationTime);
        values.put("BIT240136_IsAvailable", 1);
        values.put("BIT240136_Description", "");
        values.put("BIT240136_DishCategoryId", categoryId);

        db.insertOrThrow(TABLE_DISH, null, values);
    }

    // Queue + mapping duoc tao ma KHONG xoa du lieu cu.
    private void setupSyncTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS BIT240136_SyncQueue (" +
                "BIT240136_Id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "BIT240136_Entity TEXT NOT NULL, " +
                "BIT240136_LocalId INTEGER NOT NULL, " +
                "BIT240136_Action TEXT NOT NULL, " +
                "UNIQUE(BIT240136_Entity,BIT240136_LocalId))");
        db.execSQL("CREATE TABLE IF NOT EXISTS BIT240136_RemoteMap (" +
                "BIT240136_Entity TEXT NOT NULL, " +
                "BIT240136_LocalId INTEGER NOT NULL, " +
                "BIT240136_RemoteId TEXT NOT NULL, " +
                "PRIMARY KEY(BIT240136_Entity,BIT240136_LocalId))");

        createSyncTriggers(db, TABLE_CATEGORY, "categories");
        createSyncTriggers(db, TABLE_DISH, "dishes");
        createSyncTriggers(db, TABLE_IMAGE, "images");
    }

    private void createSyncTriggers(SQLiteDatabase db, String table, String entity) {
        for (String action : new String[]{"INSERT", "UPDATE", "DELETE"}) {
            String shortName = action.equals("INSERT") ? "ins" :
                    (action.equals("UPDATE") ? "upd" : "del");
            String row = action.equals("DELETE") ? "OLD" : "NEW";
            String operation = action.equals("DELETE") ? "delete" : "upsert";
            db.execSQL("CREATE TRIGGER IF NOT EXISTS BIT240136_sync_" + entity + "_" + shortName +
                    " AFTER " + action + " ON " + table + " BEGIN " +
                    "INSERT OR REPLACE INTO BIT240136_SyncQueue(" +
                    "BIT240136_Entity,BIT240136_LocalId,BIT240136_Action) VALUES ('" +
                    entity + "'," + row + ".BIT240136_Id,'" + operation + "'); END");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            setupSyncTables(db);
            // Dua du lieu da ton tai vao hang doi, khong xoa 6-7 mon cu.
            for (String[] pair : new String[][]{
                    {TABLE_CATEGORY, "categories"},
                    {TABLE_DISH, "dishes"},
                    {TABLE_IMAGE, "images"}}) {
                db.execSQL("INSERT OR REPLACE INTO BIT240136_SyncQueue(" +
                        "BIT240136_Entity,BIT240136_LocalId,BIT240136_Action) " +
                        "SELECT '" + pair[1] + "',BIT240136_Id,'upsert' FROM " + pair[0]);
            }
        }
    }
}
