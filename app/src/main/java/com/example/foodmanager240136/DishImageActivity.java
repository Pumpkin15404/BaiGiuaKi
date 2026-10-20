package com.example.foodmanager240136;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class DishImageActivity extends AppCompatActivity {
    private DatabaseHelper helper;
    private int dishId;
    private LinearLayout images;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        helper = new DatabaseHelper(this);
        dishId = getIntent().getIntExtra("dish_id", -1);
        if (dishId <= 0) { finish(); return; }
        setContentView(R.layout.activity_dish_images);
        images = findViewById(R.id.imageList);
        findViewById(R.id.addImage).setOnClickListener(v -> addImage());
        refresh();
    }
    private void addImage() {
        EditText input = new EditText(this);
        input.setHint("https://example.com/dish.jpg");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(this).setTitle(getString(R.string.image_url_title)).setView(input)
          .setNegativeButton(getString(R.string.cancel), null).setPositiveButton(getString(R.string.save), (d,w) -> {
            String url = input.getText().toString().trim();
            if (!url.startsWith("https://")) {
                Toast.makeText(this,getString(R.string.image_url_invalid),Toast.LENGTH_LONG).show();return;
            }
            SQLiteDatabase db = helper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("BIT240136_DishId",dishId);
            values.put("BIT240136_ImageUrl",url);
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_IMAGE + " WHERE BIT240136_DishId=?",new String[]{String.valueOf(dishId)})) {
                c.moveToFirst();values.put("BIT240136_IsThumbnail",c.getInt(0)==0?1:0);
            }
            db.insertOrThrow(DatabaseHelper.TABLE_IMAGE,null,values);
            SyncManager.schedule(this);
            refresh();
          }).show();
    }
    private void setThumbnail(int id) {
        SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
        try {
            db.execSQL("UPDATE " + DatabaseHelper.TABLE_IMAGE + " SET BIT240136_IsThumbnail=0 WHERE BIT240136_DishId=?",new Object[]{dishId});
            db.execSQL("UPDATE " + DatabaseHelper.TABLE_IMAGE + " SET BIT240136_IsThumbnail=1 WHERE BIT240136_Id=? AND BIT240136_DishId=?",new Object[]{id,dishId});
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
        SyncManager.schedule(this);
        refresh();
    }
    private void deleteImage(int id, boolean thumbnail) {
        SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
        try {
            db.delete(DatabaseHelper.TABLE_IMAGE,"BIT240136_Id=? AND BIT240136_DishId=?",new String[]{String.valueOf(id),String.valueOf(dishId)});
            if(thumbnail) db.execSQL("UPDATE " + DatabaseHelper.TABLE_IMAGE + " SET BIT240136_IsThumbnail=1 WHERE BIT240136_Id=(SELECT MIN(BIT240136_Id) FROM " + DatabaseHelper.TABLE_IMAGE + " WHERE BIT240136_DishId=?)",new Object[]{dishId});
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
        SyncManager.schedule(this);
        refresh();
    }
    private void refresh() {
        images.removeAllViews();
        SQLiteDatabase db=helper.getReadableDatabase();
        try(Cursor c=db.rawQuery("SELECT BIT240136_Id,BIT240136_ImageUrl,BIT240136_IsThumbnail FROM " + DatabaseHelper.TABLE_IMAGE + " WHERE BIT240136_DishId=? ORDER BY BIT240136_Id",new String[]{String.valueOf(dishId)})) {
            if(c.getCount()==0) { TextView empty=new TextView(this);empty.setText(getString(R.string.no_images));images.addView(empty); }
            while(c.moveToNext()) {
                int id=c.getInt(0);String url=c.getString(1);boolean thumb=c.getInt(2)==1;
                TextView title=new TextView(this);title.setText((thumb?"★ " + getString(R.string.thumbnail_name) + "  " :
                        getString(R.string.image_name) + "  ")+url);title.setPadding(0,20,0,8);images.addView(title);
                ImageView preview=new ImageView(this);preview.setLayoutParams(new LinearLayout.LayoutParams(-1,220));preview.setScaleType(ImageView.ScaleType.CENTER_CROP);images.addView(preview);
                new Thread(() -> { try {
                    java.net.HttpURLConnection conn=(java.net.HttpURLConnection)new java.net.URL(url).openConnection();conn.setConnectTimeout(5000);conn.setReadTimeout(5000);
                    android.graphics.Bitmap bitmap;try(java.io.InputStream stream=conn.getInputStream()){bitmap=android.graphics.BitmapFactory.decodeStream(stream);}finally{conn.disconnect();}
                    runOnUiThread(() -> {if(bitmap!=null)preview.setImageBitmap(bitmap);else preview.setImageResource(android.R.drawable.ic_menu_report_image);});
                } catch(Exception ex) {runOnUiThread(() -> preview.setImageResource(android.R.drawable.ic_menu_report_image));} }).start();
                Button choose=new Button(this);choose.setText(thumb?getString(R.string.current_thumbnail):getString(R.string.set_thumbnail));choose.setEnabled(!thumb);choose.setOnClickListener(v->setThumbnail(id));images.addView(choose);
                Button delete=new Button(this);delete.setText(getString(R.string.delete_image));delete.setOnClickListener(v->new AlertDialog.Builder(this).setMessage(getString(R.string.confirm_delete_image)).setNegativeButton(getString(R.string.cancel),null).setPositiveButton(getString(R.string.delete),(dialog,which)->deleteImage(id,thumb)).show());images.addView(delete);
            }
        }
    }
    @Override protected void onDestroy(){helper.close();super.onDestroy();}
}
