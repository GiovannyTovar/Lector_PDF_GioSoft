package com.giosoft.lectorpdf.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PdfHistoryManager {

    private static final String PREF_NAME = "pdf_history_prefs";
    private static final String KEY_HISTORY = "pdf_history";

    public static void savePdfItem(Context context, PdfItem item) {
        List<PdfItem> history = getPdfHistory(context);

        // Eliminar si ya existe (para evitar duplicados)
        for (PdfItem existingItem : history) {
            if (existingItem.getPath().equals(item.getPath())) {
                history.remove(existingItem);
                break;
            }
        }

        history.add(0, item); // Insertar al inicio (último abierto)

        // Guardar como JSON
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = new Gson().toJson(history);
        editor.putString(KEY_HISTORY, json);
        editor.apply();
    }

    public static List<PdfItem> getPdfHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) return new ArrayList<>();

        Type listType = new TypeToken<List<PdfItem>>() {}.getType();
        List<PdfItem> history = new Gson().fromJson(json, listType);
        return history != null ? history : Collections.emptyList();
    }


        public static void clearHistory(Context context) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(KEY_HISTORY);
            editor.apply();
        }


    public static List<PdfGroup> getPdfHistoryGrouped(Context context) {
        List<PdfItem> history = getPdfHistory(context);
        return groupByDate(history);
    }

    private static List<PdfGroup> groupByDate(List<PdfItem> items) {
        Collections.sort(items, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

        List<PdfGroup> groups = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());

        String currentGroup = "";
        List<PdfItem> currentItems = new ArrayList<>();

        for (PdfItem item : items) {
            calendar.setTimeInMillis(item.getTimestamp());
            String itemGroup = getGroupTitle(calendar);

            if (!itemGroup.equals(currentGroup)) {
                if (!currentItems.isEmpty()) {
                    groups.add(new PdfGroup(currentGroup, new ArrayList<>(currentItems)));
                    currentItems.clear();
                }
                currentGroup = itemGroup;
            }
            currentItems.add(item);
        }

        if (!currentItems.isEmpty()) {
            groups.add(new PdfGroup(currentGroup, currentItems));
        }

        return groups;
    }

    private static String getGroupTitle(Calendar calendar) {
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        if (isSameDay(calendar, today)) {
            return "Hoy";
        } else if (isSameDay(calendar, yesterday)) {
            return "Ayer";
        } else {
            SimpleDateFormat format = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());
            return format.format(calendar.getTime());
        }
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

}