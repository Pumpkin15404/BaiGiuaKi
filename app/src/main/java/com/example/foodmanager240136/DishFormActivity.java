
package com.example.foodmanager240136;
import android.widget.TextView;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class DishFormActivity extends AppCompatActivity {

    private EditText edtDishName;
    private EditText edtDishPrice;
    private EditText edtPreparationTime;
    private EditText edtDescription;
    private Spinner spinnerCategory;
    private Switch switchAvailable;

    private DatabaseHelper dbHelper;

    private int dishId = -1;

    private final ArrayList<Integer> categoryIds =
            new ArrayList<>();

    private final ArrayList<String> categoryNames =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dish_form);

        dbHelper = new DatabaseHelper(this);

        edtDishName = findViewById(R.id.edtDishName);
        edtDishPrice = findViewById(R.id.edtDishPrice);
        edtPreparationTime =
                findViewById(R.id.edtPreparationTime);
        edtDescription = findViewById(R.id.edtDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        switchAvailable = findViewById(R.id.switchAvailable);

        Button btnSave = findViewById(R.id.btnSaveDish);
        Button btnCancel = findViewById(R.id.btnCancel);

        dishId = getIntent().getIntExtra("dish_id", -1);

        loadCategories();

        // Neu co ID thi dang o che do chinh sua
        if (dishId > 0) {
            TextView txtTitle = findViewById(R.id.txtFormTitle);
            txtTitle.setText(R.string.edit_dish);

            btnSave.setText(R.string.update_dish);
            loadDishForEdit();
        }

        btnSave.setOnClickListener(v -> saveDish());
        btnCancel.setOnClickListener(v -> finish());
    }

    // Lay danh sach loai mon
    private void loadCategories() {

        categoryIds.clear();
        categoryNames.clear();

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            String sql =
                    "SELECT BIT240136_Id, BIT240136_Name " +
                            "FROM " + DatabaseHelper.TABLE_CATEGORY +
                            " ORDER BY BIT240136_Id";

            try (Cursor cursor = db.rawQuery(sql, null)) {

                while (cursor.moveToNext()) {
                    categoryIds.add(cursor.getInt(0));
                    categoryNames.add(UiLanguage.category(this, cursor.getString(1)));
                }
            }

            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            categoryNames
                    );

            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
            );

            spinnerCategory.setAdapter(adapter);

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Category error: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // Doc du lieu mon can sua
    private void loadDishForEdit() {

        String sql =
                "SELECT BIT240136_Name, " +
                        "BIT240136_Price, " +
                        "BIT240136_PreparationTime, " +
                        "BIT240136_IsAvailable, " +
                        "BIT240136_Description, " +
                        "BIT240136_DishCategoryId " +
                        "FROM " + DatabaseHelper.TABLE_DISH +
                        " WHERE BIT240136_Id = ?";

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            try (Cursor cursor = db.rawQuery(
                    sql,
                    new String[]{String.valueOf(dishId)}
            )) {

                if (!cursor.moveToFirst()) {
                    Toast.makeText(
                            this, getString(R.string.dish_not_found),
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                    return;
                }

                edtDishName.setText(cursor.getString(0));
                edtDishPrice.setText(
                        String.valueOf(cursor.getDouble(1))
                );
                edtPreparationTime.setText(
                        String.valueOf(cursor.getInt(2))
                );

                switchAvailable.setChecked(
                        cursor.getInt(3) == 1
                );

                String description = cursor.getString(4);
                edtDescription.setText(
                        description == null ? "" : description
                );

                int categoryId = cursor.getInt(5);
                int position = categoryIds.indexOf(categoryId);

                if (position >= 0) {
                    spinnerCategory.setSelection(position);
                } else {
                    Toast.makeText(
                            this, "Invalid category",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                }
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Load error: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // Kiem tra du lieu va luu
    private void saveDish() {

        String name = edtDishName.getText()
                .toString().trim();

        String priceText = edtDishPrice.getText()
                .toString().trim();

        String timeText = edtPreparationTime.getText()
                .toString().trim();

        String description = edtDescription.getText()
                .toString().trim();

        if (name.isEmpty()) {
            edtDishName.setError(getString(R.string.dish_name_required_error));
            edtDishName.requestFocus();
            return;
        }

        double price;

        try {
            price = Double.parseDouble(priceText);

            if (Double.isNaN(price)
                    || Double.isInfinite(price)
                    || price <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {
            edtDishPrice.setError(getString(R.string.price_positive));
            edtDishPrice.requestFocus();
            return;
        }

        int time;

        try {
            time = Integer.parseInt(timeText);

            if (time <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {
            edtPreparationTime.setError(
                    getString(R.string.time_positive)
            );
            edtPreparationTime.requestFocus();
            return;
        }

        int position = spinnerCategory.getSelectedItemPosition();

        if (position < 0 || position >= categoryIds.size()) {
            Toast.makeText(
                    this,
                    getString(R.string.please_category),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        int categoryId = categoryIds.get(position);

        ContentValues values = new ContentValues();

        values.put("BIT240136_Name", name);
        values.put("BIT240136_Price", price);
        values.put("BIT240136_PreparationTime", time);
        values.put(
                "BIT240136_IsAvailable",
                switchAvailable.isChecked() ? 1 : 0
        );
        values.put("BIT240136_Description", description);
        values.put("BIT240136_DishCategoryId", categoryId);

        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            if (dishId > 0) {

                // Cap nhat mon da ton tai
                int rows = db.update(
                        DatabaseHelper.TABLE_DISH,
                        values,
                        "BIT240136_Id = ?",
                        new String[]{String.valueOf(dishId)}
                );

                if (rows == 0) {
                    Toast.makeText(
                            this, getString(R.string.dish_not_found),
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                Toast.makeText(
                        this,
                        getString(R.string.dish_updated),
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                // Them mon an moi
                db.insertOrThrow(
                        DatabaseHelper.TABLE_DISH,
                        null,
                        values
                );

                Toast.makeText(
                        this,
                        getString(R.string.dish_added),
                        Toast.LENGTH_SHORT
                ).show();
            }

            SyncManager.schedule(this);
            setResult(RESULT_OK);
            finish();

        } catch (SQLiteConstraintException e) {

            edtDishName.setError(
                    getString(R.string.duplicate_dish)
            );
            edtDishName.requestFocus();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Save error: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}
