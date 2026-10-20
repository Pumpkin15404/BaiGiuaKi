package com.example.foodmanager240136;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class CategoryActivity extends AppCompatActivity {
    private DatabaseHelper helper;
    private LinearLayout items;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_categories);
        helper = new DatabaseHelper(this);
        items = findViewById(R.id.categoryItems);
        findViewById(R.id.btnAddCategory).setOnClickListener(v -> editCategory(-1, "", ""));
        findViewById(R.id.btnCategoryBack).setOnClickListener(v -> finish());
        refresh();
    }

    private void refresh() {
        items.removeAllViews();
        try (Cursor c = helper.getReadableDatabase().rawQuery(
                "SELECT c.BIT240136_Id,c.BIT240136_Name,c.BIT240136_Description,COUNT(d.BIT240136_Id) " +
                "FROM " + DatabaseHelper.TABLE_CATEGORY + " c LEFT JOIN " + DatabaseHelper.TABLE_DISH +
                " d ON c.BIT240136_Id=d.BIT240136_DishCategoryId GROUP BY c.BIT240136_Id ORDER BY c.BIT240136_Id", null)) {
            while (c.moveToNext()) {
                int id = c.getInt(0), count = c.getInt(3);
                String name = c.getString(1), desc = c.getString(2);
                View row = getLayoutInflater().inflate(R.layout.item_category, items, false);
                ((TextView) row.findViewById(R.id.txtCategoryItem)).setText(UiLanguage.category(this, name) + " (" + count + ")\n" + (desc == null ? "" : desc));
                row.findViewById(R.id.btnEditCategory).setOnClickListener(v -> editCategory(id, name, desc));
                row.findViewById(R.id.btnDeleteCategory).setOnClickListener(v -> deleteCategory(id));
                items.addView(row);
            }
        } catch (Exception e) { show(e.getMessage()); }
    }

    private void editCategory(int id, String oldName, String oldDescription) {
        View form = getLayoutInflater().inflate(R.layout.dialog_category, null);
        EditText name = form.findViewById(R.id.edtCategoryName);
        EditText description = form.findViewById(R.id.edtCategoryDescription);
        name.setText(oldName);
        description.setText(oldDescription == null ? "" : oldDescription);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(id < 0 ? R.string.add_category : R.string.edit_category)
                .setView(form).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            if (n.isEmpty()) { name.setError(getString(R.string.required_name)); return; }
            ContentValues data = new ContentValues();
            data.put("BIT240136_Name", n);
            data.put("BIT240136_Description", description.getText().toString().trim());
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                if (id < 0) db.insertOrThrow(DatabaseHelper.TABLE_CATEGORY, null, data);
                else {
                    db.update(DatabaseHelper.TABLE_CATEGORY, data, "BIT240136_Id=?", new String[]{String.valueOf(id)});
                    
                }
                SyncManager.schedule(this);
                dialog.dismiss(); refresh();
            } catch (Exception e) { show(e.getMessage()); }
        }));
        dialog.show();
    }

    private void deleteCategory(int id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_DISH +
                " WHERE BIT240136_DishCategoryId=?", new String[]{String.valueOf(id)})) {
            if (c.moveToFirst() && c.getInt(0) > 0) {
                show(getString(R.string.category_in_use)); return;
            }
        }
        new AlertDialog.Builder(this).setTitle(R.string.delete_category)
                .setMessage(R.string.confirm_category_delete)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    try {
                        helper.getWritableDatabase().delete(DatabaseHelper.TABLE_CATEGORY,
                                "BIT240136_Id=?", new String[]{String.valueOf(id)});
                        SyncManager.schedule(this);
                        refresh();
                    } catch (Exception e) { show(e.getMessage()); }
                }).show();
    }

    private void show(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
    @Override protected void onDestroy() { helper.close(); super.onDestroy(); }
}
