package com.example.foodmanager240136;

import android.content.Context;

public final class UiLanguage {
    private UiLanguage() {}
    public static String category(Context context, String name) {
        if ("Main Dishes".equals(name)) return context.getString(R.string.main_dishes);
        if ("Drinks".equals(name)) return context.getString(R.string.drinks);
        if ("Desserts".equals(name)) return context.getString(R.string.desserts);
        return name;
    }
}
