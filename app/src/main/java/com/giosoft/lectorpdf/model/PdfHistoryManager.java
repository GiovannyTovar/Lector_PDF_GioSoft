package com.giosoft.lectorpdf.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

}