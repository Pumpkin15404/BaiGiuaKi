package com.example.foodmanager240136;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Simple background thumbnail loader. Only HTTPS is loaded. */
public final class ImageLoader {
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String,Bitmap> CACHE = new LruCache<String,Bitmap>(8 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount() / 1024; }
    };
    private ImageLoader() { }

    public static void load(ImageView image, String address) {
        image.setImageResource(android.R.drawable.ic_menu_gallery);
        image.setTag(address);
        if (address == null || !address.startsWith("https://")) return;
        Bitmap hit = CACHE.get(address);
        if (hit != null) { image.setImageBitmap(hit); return; }
        POOL.execute(() -> {
            Bitmap bitmap = null;
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection)new URL(address).openConnection();
                conn.setConnectTimeout(6000); conn.setReadTimeout(6000);
                conn.setInstanceFollowRedirects(false);
                if (conn.getResponseCode() == 200 &&
                        conn.getContentLengthLong() <= 4L * 1024 * 1024) {
                    try (InputStream input = conn.getInputStream()) {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inSampleSize = 2;
                        bitmap = BitmapFactory.decodeStream(input, null, opts);
                    }
                }
            } catch (Exception ignored) { }
            finally { if (conn != null) conn.disconnect(); }
            final Bitmap result = bitmap;
            if (result != null) CACHE.put(address, result);
            MAIN.post(() -> {
                if (address.equals(image.getTag()) && result != null) {
                    image.setImageBitmap(result);
                }
            });
        });
    }
}
