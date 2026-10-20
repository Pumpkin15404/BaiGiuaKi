
package com.example.foodmanager240136;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.util.Locale;

public class DishDetailActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private int dishId;

    private TextView txtName;
    private TextView txtPrice;
    private TextView txtCategory;
    private TextView txtTime;
    private TextView txtType;
    private TextView txtAvailable;
    private TextView txtDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dish_detail);

        dbHelper = new DatabaseHelper(this);

        dishId = getIntent().getIntExtra("dish_id", -1);

        txtName = findViewById(R.id.txtDetailName);
        txtPrice = findViewById(R.id.txtDetailPrice);
        txtCategory = findViewById(R.id.txtDetailCategory);
        txtTime = findViewById(R.id.txtDetailTime);
        txtType = findViewById(R.id.txtDetailType);
        txtAvailable = findViewById(R.id.txtDetailAvailable);
        txtDescription = findViewById(
                R.id.txtDetailDescription
        );

        Button btnEdit = findViewById(R.id.btnEditDish);
        Button btnDelete = findViewById(R.id.btnDeleteDish);
        Button btnBack = findViewById(R.id.btnBack);

        findViewById(R.id.btnDishImages).setOnClickListener(v -> {
            Intent intent = new Intent(this, DishImageActivity.class);
            intent.putExtra("dish_id", dishId);
            startActivity(intent);
        });

        // Mo man hinh chinh sua
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(
                    this, DishFormActivity.class
            );
            intent.putExtra("dish_id", dishId);
            startActivity(intent);
        });

        // Hien thi xac nhan xoa
        btnDelete.setOnClickListener(v -> confirmDelete());

        btnBack.setOnClickListener(v -> finish());
    }

    // Doc lai thong tin sau khi quay ve tu man hinh sua
    @Override
    protected void onResume() {
        super.onResume();
        loadDishDetail();
    }

    private void loadDishDetail() {

        if (dishId < 1) {
            Toast.makeText(
                    this, "Invalid dish ID",
                    Toast.LENGTH_SHORT
            ).show();
            finish();
            return;
        }

        String sql =
                "SELECT d.BIT240136_Name, " +
                        "d.BIT240136_Price, " +
                        "d.BIT240136_PreparationTime, " +
                        "d.BIT240136_IsAvailable, " +
                        "d.BIT240136_Description, " +
                        "c.BIT240136_Name " +
                        "FROM " + DatabaseHelper.TABLE_DISH + " d " +
                        "INNER JOIN " + DatabaseHelper.TABLE_CATEGORY +
                        " c ON d.BIT240136_DishCategoryId = " +
                        "c.BIT240136_Id " +
                        "WHERE d.BIT240136_Id = ?";

        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            try (Cursor cursor = db.rawQuery(
                    sql,
                    new String[]{String.valueOf(dishId)}
            )) {

                if (!cursor.moveToFirst()) {
                    Toast.makeText(
                            this, "Dish not found",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                    return;
                }

                String name = cursor.getString(0);
                double price = cursor.getDouble(1);
                int time = cursor.getInt(2);
                boolean available = cursor.getInt(3) == 1;
                String description = cursor.getString(4);
                String category = cursor.getString(5);

                NumberFormat formatter =
                        NumberFormat.getNumberInstance(
                                Locale.forLanguageTag("vi-VN")
                        );

                txtName.setText(name);
                loadThumbnail();
                txtPrice.setText(
                        formatter.format(price) + " VND"
                );
                txtCategory.setText(getString(R.string.category_pattern, UiLanguage.category(this, category)));
                txtTime.setText(
                        getString(R.string.prep_time_pattern, time)
                );
                txtType.setText(
                        getString(R.string.type_pattern, getString(time <= 15 ? R.string.quick : R.string.regular))
                );
                txtAvailable.setText(
                        getString(R.string.status_pattern, getString(available ? R.string.available : R.string.unavailable))
                );

                txtDescription.setText(
                        description == null ||
                                description.trim().isEmpty()
                                ? getString(R.string.no_description)
                                : description
                );
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Load error: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void loadThumbnail() {
        ImageView image = findViewById(R.id.imgDetailThumbnail);
        String imageUrl = null;
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT BIT240136_ImageUrl FROM " + DatabaseHelper.TABLE_IMAGE +
                " WHERE BIT240136_DishId=? AND BIT240136_IsThumbnail=1 LIMIT 1",
                new String[]{String.valueOf(dishId)})) {
            if (c.moveToFirst()) imageUrl = c.getString(0);
        }
        ImageLoader.load(image, imageUrl);
    }

    // Hop thoai xac nhan truoc khi xoa
    private void confirmDelete() {

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_dish)
                .setMessage(R.string.confirm_delete_dish_long)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(
                        R.string.delete,
                        (dialog, which) -> deleteDish()
                )
                .show();
    }

    // Xoa mon khoi SQLite
    private void deleteDish() {

        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            int deletedRows = db.delete(
                    DatabaseHelper.TABLE_DISH,
                    "BIT240136_Id = ?",
                    new String[]{String.valueOf(dishId)}
            );

            if (deletedRows > 0) {
                Toast.makeText(
                        this,
                        getString(R.string.dish_deleted),
                        Toast.LENGTH_SHORT
                ).show();

                SyncManager.schedule(this);
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(
                        this,
                        "Dish not found",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Delete error: " + e.getMessage(),
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
