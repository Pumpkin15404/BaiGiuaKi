
package com.example.foodmanager240136;

import android.content.Intent;
import android.text.InputType;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;

    private LinearLayout layoutDishList;
    private LinearLayout layoutFilters;
    private TextView txtDishCount;

    private EditText edtSearch;
    private EditText edtMinPrice;
    private EditText edtMaxPrice;

    private Spinner spinnerCategory;
    private Spinner spinnerStatus;
    private Spinner spinnerSort;

    private Button btnToggleFilters;

    // Danh sach ID loai mon tu SQLite
    private final ArrayList<Integer> categoryIds =
            new ArrayList<>();

    private final ArrayList<String> categoryNames =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Giu padding va tranh thanh he thong che giao dien
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    int p = (int) (
                            12 * getResources()
                                    .getDisplayMetrics().density
                    );

                    v.setPadding(
                            p + bars.left,
                            p + bars.top,
                            p + bars.right,
                            p + bars.bottom
                    );

                    return insets;
                }
        );

        // Anh xa giao dien
        layoutDishList = findViewById(R.id.layoutDishList);
        layoutFilters = findViewById(R.id.layoutFilters);
        txtDishCount = findViewById(R.id.txtDishCount);

        edtSearch = findViewById(R.id.edtSearch);
        edtMinPrice = findViewById(R.id.edtMinPrice);
        edtMaxPrice = findViewById(R.id.edtMaxPrice);

        spinnerCategory =
                findViewById(R.id.spinnerFilterCategory);

        spinnerStatus =
                findViewById(R.id.spinnerFilterStatus);

        spinnerSort =
                findViewById(R.id.spinnerSort);

        btnToggleFilters =
                findViewById(R.id.btnToggleFilters);

        Button btnReset =
                findViewById(R.id.btnResetFilters);

        Button btnAddDish =
                findViewById(R.id.btnAddDish);

        findViewById(R.id.btnCategories).setOnClickListener(v ->
                startActivity(new Intent(this, CategoryActivity.class)));

        // Cau hinh dong bo MockAPI va chuyen ngon ngu
        findViewById(R.id.btnLanguage).setOnClickListener(v -> {
            String language = AppCompatDelegate.getApplicationLocales().toLanguageTags();
            AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(language.startsWith("vi") ? "en" : "vi")
            );
        });
        findViewById(R.id.btnSyncSettings).setOnClickListener(v -> showSyncDialog());

        // Khoi tao SQLite
        dbHelper = new DatabaseHelper(this);

        // Nap du lieu cho bo loc
        setupCategoryFilter();
        setupStatusFilter();
        setupSortFilter();

        // Lang nghe thay doi dieu kien loc
        setupFilterListeners();

        // Mo / dong bo loc
        btnToggleFilters.setOnClickListener(v -> {

            if (layoutFilters.getVisibility() == View.VISIBLE) {

                layoutFilters.setVisibility(View.GONE);
                btnToggleFilters.setText(R.string.show_filters);

            } else {

                layoutFilters.setVisibility(View.VISIBLE);
                btnToggleFilters.setText(R.string.hide_filters);
            }
        });

        // Dat lai tat ca dieu kien
        btnReset.setOnClickListener(v -> resetFilters());

        // Mo man hinh them mon
        btnAddDish.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    DishFormActivity.class
            );

            startActivity(intent);
        });
    }

    // Lay danh sach loai mon tu SQLite
    private void setupCategoryFilter() {

        categoryIds.clear();
        categoryNames.clear();

        categoryIds.add(-1);
        categoryNames.add(getString(R.string.all_categories));

        try {
            SQLiteDatabase db =
                    dbHelper.getReadableDatabase();

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

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Category error: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
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
    }

    // Bo loc trang thai
    private void setupStatusFilter() {

        String[] statuses = {
                getString(R.string.all_statuses),
                getString(R.string.available),
                getString(R.string.unavailable)
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        statuses
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerStatus.setAdapter(adapter);
    }

    // Bo sap xep
    private void setupSortFilter() {

        String[] sorts = {
                getString(R.string.sort_default),
                getString(R.string.price_low_high),
                getString(R.string.price_high_low),
                getString(R.string.time_short_long),
                getString(R.string.time_long_short)
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        sorts
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerSort.setAdapter(adapter);
    }

    // Gan su kien cho tat ca bo loc
    private void setupFilterListeners() {

        TextWatcher watcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {

                loadDishes();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        edtSearch.addTextChangedListener(watcher);
        edtMinPrice.addTextChangedListener(watcher);
        edtMaxPrice.addTextChangedListener(watcher);

        AdapterView.OnItemSelectedListener listener =
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        loadDishes();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                };

        spinnerCategory.setOnItemSelectedListener(listener);
        spinnerStatus.setOnItemSelectedListener(listener);
        spinnerSort.setOnItemSelectedListener(listener);
    }

    // Xoa bo loc va hien thi tat ca mon an
    private void resetFilters() {

        edtSearch.setText("");
        edtMinPrice.setText("");
        edtMaxPrice.setText("");

        spinnerCategory.setSelection(0);
        spinnerStatus.setSelection(0);
        spinnerSort.setSelection(0);

        loadDishes();
    }

    // Doc du lieu theo tat ca dieu kien dang chon
    private void loadDishes() {

        if (dbHelper == null || layoutDishList == null) {
            return;
        }

        layoutDishList.removeAllViews();

        String keyword =
                edtSearch.getText().toString().trim();

        String minText =
                edtMinPrice.getText().toString().trim();

        String maxText =
                edtMaxPrice.getText().toString().trim();

        Double minPrice = null;
        Double maxPrice = null;

        // Kiem tra khoang gia
        try {

            if (!minText.isEmpty()) {
                minPrice = Double.parseDouble(minText);
            }

            if (!maxText.isEmpty()) {
                maxPrice = Double.parseDouble(maxText);
            }

            if ((minPrice != null &&
                    (!Double.isFinite(minPrice) || minPrice < 0))
                    || (maxPrice != null &&
                    (!Double.isFinite(maxPrice) || maxPrice < 0))) {

                txtDishCount.setText(R.string.invalid_price_range);
                return;
            }

            if (minPrice != null && maxPrice != null
                    && minPrice > maxPrice) {

                txtDishCount.setText(R.string.min_exceeds_max);
                return;
            }

        } catch (NumberFormatException e) {

            txtDishCount.setText(R.string.invalid_price_range);
            return;
        }

        ArrayList<String> args = new ArrayList<>();

        StringBuilder sql = new StringBuilder();

        sql.append(
                "SELECT d.BIT240136_Id, " +
                        "d.BIT240136_Name, " +
                        "d.BIT240136_Price, " +
                        "d.BIT240136_PreparationTime, " +
                        "d.BIT240136_IsAvailable, " +
                        "c.BIT240136_Name " +
                        "FROM " + DatabaseHelper.TABLE_DISH + " d " +
                        "INNER JOIN " + DatabaseHelper.TABLE_CATEGORY +
                        " c ON d.BIT240136_DishCategoryId = " +
                        "c.BIT240136_Id WHERE 1=1 "
        );

        // 1. Tim theo ten mon
        if (!keyword.isEmpty()) {

            sql.append("AND d.BIT240136_Name LIKE ? ");
            args.add("%" + keyword + "%");
        }

        // 2. Loc theo loai mon
        int categoryPosition =
                spinnerCategory.getSelectedItemPosition();

        if (categoryPosition > 0 &&
                categoryPosition < categoryIds.size()) {

            int categoryId =
                    categoryIds.get(categoryPosition);

            sql.append(
                    "AND d.BIT240136_DishCategoryId = ? "
            );

            args.add(String.valueOf(categoryId));
        }

        // 3. Loc theo trang thai
        int statusPosition =
                spinnerStatus.getSelectedItemPosition();

        if (statusPosition == 1) {

            sql.append(
                    "AND d.BIT240136_IsAvailable = 1 "
            );

        } else if (statusPosition == 2) {

            sql.append(
                    "AND d.BIT240136_IsAvailable = 0 "
            );
        }

        // 4. Loc theo gia toi thieu
        if (minPrice != null) {

            sql.append(
                    "AND d.BIT240136_Price >= ? "
            );

            args.add(String.valueOf(minPrice));
        }

        // 5. Loc theo gia toi da
        if (maxPrice != null) {

            sql.append(
                    "AND d.BIT240136_Price <= ? "
            );

            args.add(String.valueOf(maxPrice));
        }

        // 6. Sap xep theo lua chon
        int sortPosition =
                spinnerSort.getSelectedItemPosition();

        switch (sortPosition) {

            case 1:
                sql.append(
                        "ORDER BY d.BIT240136_Price ASC, " +
                                "d.BIT240136_Id ASC"
                );
                break;

            case 2:
                sql.append(
                        "ORDER BY d.BIT240136_Price DESC, " +
                                "d.BIT240136_Id ASC"
                );
                break;

            case 3:
                sql.append(
                        "ORDER BY d.BIT240136_PreparationTime ASC, " +
                                "d.BIT240136_Id ASC"
                );
                break;

            case 4:
                sql.append(
                        "ORDER BY d.BIT240136_PreparationTime DESC, " +
                                "d.BIT240136_Id ASC"
                );
                break;

            default:
                sql.append(
                        "ORDER BY d.BIT240136_Id ASC"
                );
                break;
        }

        // Thuc hien truy van SQLite
        int count = 0;

        try {

            SQLiteDatabase db =
                    dbHelper.getReadableDatabase();

            String[] selectionArgs =
                    args.toArray(new String[0]);

            try (Cursor cursor = db.rawQuery(
                    sql.toString(),
                    selectionArgs
            )) {

                while (cursor.moveToNext()) {

                    int id = cursor.getInt(0);
                    String name = cursor.getString(1);
                    double price = cursor.getDouble(2);
                    int time = cursor.getInt(3);
                    boolean available = cursor.getInt(4) == 1;
                    String category = cursor.getString(5);

                    addDishView(
                            id,
                            name,
                            price,
                            time,
                            available,
                            category
                    );

                    count++;
                }
            }

            txtDishCount.setText(
                    getString(R.string.all_dishes_count, count)
            );

            if (count == 0) {

                TextView emptyView = new TextView(this);
                emptyView.setText(
                        "No dishes match your filters."
                );
                emptyView.setTextSize(15);
                emptyView.setPadding(12, 24, 12, 24);

                layoutDishList.addView(emptyView);
            }

        } catch (Exception e) {

            txtDishCount.setText(R.string.cannot_load_dishes);

            Toast.makeText(
                    this,
                    "Database error: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // Hien thi tung mon an
    private void addDishView(
            int id,
            String name,
            double price,
            int time,
            boolean available,
            String category) {

        View item = LayoutInflater.from(this).inflate(
                R.layout.item_dish,
                layoutDishList,
                false
        );

        TextView txtName =
                item.findViewById(R.id.txtDishName);

        TextView txtInfo =
                item.findViewById(R.id.txtDishInfo);

        TextView txtPrice =
                item.findViewById(R.id.txtDishPrice);

        TextView txtIcon =
                item.findViewById(R.id.txtDishIcon);
        ImageView thumbnail = item.findViewById(R.id.imgDishThumbnail);

        txtName.setText(name);

        // Phan loai thoi gian che bien
        String type = getString(time <= 15 ? R.string.quick : R.string.regular);

        txtInfo.setText(getString(R.string.dish_info_pattern,
                UiLanguage.category(this, category), time, type,
                available ? "" : getString(R.string.unavailable_suffix)));

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        Locale.forLanguageTag("vi-VN")
                );

        txtPrice.setText(
                formatter.format(price) + " VND"
        );

        // Hien thi bieu tuong tam thoi
        if (category.equals("Drinks")) {

            txtIcon.setText("🥤");

        } else if (category.equals("Desserts")) {

            txtIcon.setText("🍰");

        } else {

            txtIcon.setText("🍽");
        }

        String imageUrl = thumbnailUrl(id);
        if (imageUrl != null) {
            thumbnail.setVisibility(View.VISIBLE);
            ImageLoader.load(thumbnail, imageUrl);
        }

        // Mon ngung ban van hien trong danh sach quan ly
        if (!available) {

            txtName.setTextColor(Color.GRAY);
            item.setAlpha(0.65f);
        }

        // Mo man hinh chi tiet mon
        item.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    DishDetailActivity.class
            );

            intent.putExtra("dish_id", id);
            startActivity(intent);
        });

        layoutDishList.addView(item);
    }

    private String thumbnailUrl(int id) {
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT BIT240136_ImageUrl FROM " + DatabaseHelper.TABLE_IMAGE +
                " WHERE BIT240136_DishId=? AND BIT240136_IsThumbnail=1 LIMIT 1",
                new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? c.getString(0) : null;
        } catch (Exception ignored) { return null; }
    }

    // Giu nguyen bo loc khi quay lai tu man hinh khac
    @Override
    protected void onResume() {
        super.onResume();

        if (dbHelper != null && edtSearch != null) {
            int oldPosition = spinnerCategory.getSelectedItemPosition();
            int oldCategory = oldPosition >= 0 && oldPosition < categoryIds.size()
                    ? categoryIds.get(oldPosition) : -1;
            // Refresh categories after editing them, preserve selected category.
            setupCategoryFilter();
            int newPosition = categoryIds.indexOf(oldCategory);
            spinnerCategory.setSelection(Math.max(newPosition, 0));
            loadDishes();
            refreshSyncStatus();
            SyncManager.schedule(this);
        }
    }

    private void refreshSyncStatus() {
        TextView status = findViewById(R.id.txtSyncStatus);
        if (!SyncManager.validUrl(SyncManager.getBaseUrl(this))) {
            status.setText(R.string.not_configured);
        } else {
            status.setText(getString(R.string.sync_pending,
                    SyncManager.pendingCount(this)));
        }
    }

    private void showSyncDialog() {
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        edit.setHint("https://YOUR-PROJECT.mockapi.io/api/v1");
        edit.setText(SyncManager.getBaseUrl(this));
        new AlertDialog.Builder(this)
                .setTitle(R.string.mockapi_url_title)
                .setView(edit)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.sync_now, (dialog, which) -> {
                    if (!SyncManager.validUrl(SyncManager.getBaseUrl(this))) {
                        Toast.makeText(this, R.string.not_configured, Toast.LENGTH_LONG).show();
                        return;
                    }
                    SyncManager.schedule(this);
                    refreshSyncStatus();
                    Toast.makeText(this, getString(R.string.sync_queued,
                            SyncManager.pendingCount(this)), Toast.LENGTH_LONG).show();
                })
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String url = edit.getText().toString().trim();
                    if (!SyncManager.validUrl(url)) {
                        Toast.makeText(this,R.string.url_https,Toast.LENGTH_LONG).show();
                        return;
                    }
                    SyncManager.setBaseUrl(this, url);
                    refreshSyncStatus();
                    Toast.makeText(this,R.string.url_saved,Toast.LENGTH_SHORT).show();
                }).show();
    }

    @Override
    protected void onDestroy() {

        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }
}
