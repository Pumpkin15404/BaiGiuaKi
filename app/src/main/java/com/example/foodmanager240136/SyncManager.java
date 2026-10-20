package com.example.foodmanager240136;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Local-first: queue SQLite is always the source of truth. */
public final class SyncManager {
    private static final String PREFS = "BIT240136_sync_settings";
    private static final String KEY_URL = "mockapi_base_url";
    private static final int JOB_ID = 240136;
    private SyncManager() { }

    public static String getBaseUrl(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_URL, "");
    }

    public static boolean validUrl(String text) {
        if (text == null || !text.startsWith("https://")) return false;
        try {
            URL url = new URL(text);
            return url.getHost() != null && !url.getHost().isEmpty() &&
                    url.getUserInfo() == null;
        } catch (Exception ignored) { return false; }
    }

    public static void setBaseUrl(Context context, String url) {
        if (!validUrl(url)) throw new IllegalArgumentException("HTTPS MockAPI URL required");
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_URL, url.replaceAll("/+$", "")).apply();
        schedule(context);
    }

    public static int pendingCount(Context context) {
        try (DatabaseHelper helper = new DatabaseHelper(context)) {
            try (Cursor c = helper.getReadableDatabase().rawQuery(
                    "SELECT COUNT(*) FROM BIT240136_SyncQueue", null)) {
                return c.moveToFirst() ? c.getInt(0) : 0;
            }
        } catch (Exception ignored) { return -1; }
    }

    // JobScheduler waits for network; job persists over process death and device reboot.
    public static void schedule(Context context) {
        if (!validUrl(getBaseUrl(context))) return;
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return;
        JobInfo job = new JobInfo.Builder(JOB_ID,
                new ComponentName(context, SyncJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setBackoffCriteria(30000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .build();
        scheduler.schedule(job);
    }

    // Called from background worker only; never on main thread.
    public static boolean sync(Context context) {
        String base = getBaseUrl(context);
        if (!validUrl(base)) return false;
        try (DatabaseHelper helper = new DatabaseHelper(context)) {
            SQLiteDatabase db = helper.getWritableDatabase();
            // Order: categories before dishes, then images for FK-friendly remote APIs.
            for (String entity : new String[]{"categories", "dishes", "images"}) {
                for (Pending p : pending(db, entity, "upsert")) {
                    JSONObject payload = snapshot(db, p.entity, p.localId);
                    if (payload == null) {
                        // Deleted since reading queue: process as deletion instead.
                        deleteRemote(db, base, p.entity, p.localId);
                    } else {
                        String remoteId = remoteId(db, base, p.entity, p.localId);
                        JSONObject output = request(remoteId == null ? "POST" : "PUT",
                                base + "/" + p.entity + (remoteId == null ? "" : "/" + remoteId),
                                payload);
                        String savedId = output.optString("id", remoteId);
                        if (savedId == null || savedId.isEmpty())
                            throw new IllegalStateException("MockAPI response missing id");
                        ContentValues mapping = new ContentValues();
                        mapping.put("BIT240136_Entity", p.entity);
                        mapping.put("BIT240136_LocalId", p.localId);
                        mapping.put("BIT240136_RemoteId", savedId);
                        db.insertWithOnConflict("BIT240136_RemoteMap", null, mapping,
                                SQLiteDatabase.CONFLICT_REPLACE);
                    }
                    clearIfUnchanged(db, p);
                }
            }
            // Reverse order for deletions, so dependent objects go first.
            for (String entity : new String[]{"images", "dishes", "categories"}) {
                for (Pending p : pending(db, entity, "delete")) {
                    deleteRemote(db, base, p.entity, p.localId);
                    clearIfUnchanged(db, p);
                }
            }
            return true;
        } catch (Exception ignored) {
            // Queue remains durable; JobScheduler retries after network/API recovers.
            return false;
        }
    }

    private static final class Pending {
        final long queueId;
        final String entity;
        final int localId;
        final String action;
        Pending(long queueId, String entity, int localId, String action) {
            this.queueId=queueId; this.entity=entity; this.localId=localId; this.action=action;
        }
    }

    private static List<Pending> pending(SQLiteDatabase db, String entity, String action) {
        List<Pending> results = new ArrayList<>();
        try (Cursor c = db.rawQuery(
                "SELECT BIT240136_Id,BIT240136_Entity,BIT240136_LocalId,BIT240136_Action " +
                "FROM BIT240136_SyncQueue WHERE BIT240136_Entity=? AND BIT240136_Action=? " +
                "ORDER BY BIT240136_Id", new String[]{entity, action})) {
            while (c.moveToNext()) results.add(new Pending(c.getLong(0),c.getString(1),c.getInt(2),c.getString(3)));
        }
        return results;
    }

    private static void clearIfUnchanged(SQLiteDatabase db, Pending p) {
        // A new edit during a slow network request creates a new queue ID.
        db.delete("BIT240136_SyncQueue", "BIT240136_Id=?",
                new String[]{String.valueOf(p.queueId)});
    }

    private static String remoteId(SQLiteDatabase db, String base, String entity, int localId) throws Exception {
        try (Cursor c = db.rawQuery("SELECT BIT240136_RemoteId FROM BIT240136_RemoteMap " +
                "WHERE BIT240136_Entity=? AND BIT240136_LocalId=?",
                new String[]{entity,String.valueOf(localId)})) {
            if (c.moveToFirst()) return c.getString(0);
        }
        // Recover mapping from previous server upload after app reinstall/map loss.
        JSONArray remote = requestList(base + "/" + entity);
        for (int i=0;i<remote.length();i++) {
            JSONObject item = remote.optJSONObject(i);
            if (item != null && item.optInt("BIT240136_LocalId", -1) == localId) {
                String id = item.optString("id", "");
                if (!id.isEmpty()) return id;
            }
        }
        return null;
    }

    private static void deleteRemote(SQLiteDatabase db, String base, String entity, int localId) throws Exception {
        String id = remoteId(db, base, entity, localId);
        if (id != null) {
            try { request("DELETE", base + "/" + entity + "/" + id, null); }
            catch (ApiException e) { if (e.status != 404) throw e; }
        }
        db.delete("BIT240136_RemoteMap", "BIT240136_Entity=? AND BIT240136_LocalId=?",
                new String[]{entity, String.valueOf(localId)});
    }

    private static JSONObject snapshot(SQLiteDatabase db, String entity, int localId) throws Exception {
        String table;
        if (entity.equals("categories")) table=DatabaseHelper.TABLE_CATEGORY;
        else if (entity.equals("dishes")) table=DatabaseHelper.TABLE_DISH;
        else table=DatabaseHelper.TABLE_IMAGE;
        try (Cursor c = db.query(table, null, "BIT240136_Id=?",
                new String[]{String.valueOf(localId)}, null, null, null)) {
            if (!c.moveToFirst()) return null;
            JSONObject json = new JSONObject();
            json.put("BIT240136_LocalId", localId);
            for (int i=0;i<c.getColumnCount();i++) {
                String key=c.getColumnName(i);
                if (key.equals("BIT240136_Id")) continue;
                if (c.isNull(i)) json.put(key, JSONObject.NULL);
                else if (c.getType(i)==Cursor.FIELD_TYPE_INTEGER) json.put(key,c.getLong(i));
                else if (c.getType(i)==Cursor.FIELD_TYPE_FLOAT) json.put(key,c.getDouble(i));
                else json.put(key,c.getString(i));
            }
            return json;
        }
    }

    private static final class ApiException extends Exception {
        final int status;
        ApiException(int status) { super("HTTP " + status); this.status=status; }
    }

    private static JSONArray requestList(String url) throws Exception {
        Object value = http("GET",url,null);
        if (!(value instanceof JSONArray)) throw new Exception("Expected JSON array from GET " + url);
        return (JSONArray)value;
    }

    private static JSONObject request(String method, String url, JSONObject payload) throws Exception {
        Object value = http(method,url,payload);
        return value instanceof JSONObject ? (JSONObject)value : new JSONObject();
    }

    private static Object http(String method, String address, JSONObject payload) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)new URL(address).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", "application/json");
        try {
            if (payload != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream stream=connection.getOutputStream()) {
                    stream.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int status=connection.getResponseCode();
            if (status < 200 || status >= 300) throw new ApiException(status);
            try (InputStream stream=connection.getInputStream()) {
                ByteArrayOutputStream buffer=new ByteArrayOutputStream();
                byte[] chunk=new byte[4096]; int count;
                while ((count=stream.read(chunk))!=-1) buffer.write(chunk,0,count);
                String body=buffer.toString("UTF-8").trim();
                if (body.isEmpty()) return new JSONObject();
                if (body.charAt(0)=='[') return new JSONArray(body);
                return new JSONObject(body);
            }
        } finally { connection.disconnect(); }
    }
}
