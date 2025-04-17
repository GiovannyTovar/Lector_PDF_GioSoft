package com.giosoft.lectorpdf;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.Manifest;

import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artifex.mupdf.viewer.BuildConfig;
import com.giosoft.lectorpdf.adapter.PdfAdapter;
import com.giosoft.lectorpdf.model.PdfHistoryManager;
import com.giosoft.lectorpdf.model.PdfItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OPEN_DOCUMENT = 1;

    private String lastOpenedPdfPath = null; // Ruta del último PDF abierto
    private PdfAdapter pdfAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configuración de Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView recyclerView = findViewById(R.id.pdfRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<PdfItem> history = PdfHistoryManager.getPdfHistory(this);
        pdfAdapter = new PdfAdapter(this, history);
        recyclerView.setAdapter(pdfAdapter);


        // Botón para abrir el PDF
        Button openPdfButton = findViewById(R.id.openPdfButton);
        openPdfButton.setOnClickListener(v -> openPdfFile());


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflar el menú de la Action Bar
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    private void openPdfFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();

            Log.d("PDF_DEBUG", "URI seleccionada: " + uri);

            if (uri != null) {
                getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                );

                String fileName = getFileNameFromUri(uri);
                String copiedPath = copyPdfToExternalStorage(uri, fileName);
                if (copiedPath != null) {
                    lastOpenedPdfPath = copiedPath;

                    String name = getFileNameFromUri(uri);
                    PdfHistoryManager.savePdfItem(this, new PdfItem(copiedPath, name, System.currentTimeMillis()));

                    pdfAdapter.updateData(PdfHistoryManager.getPdfHistory(this));
                    openPdfWithMuPDF(copiedPath);
                } else {
                    Toast.makeText(this, "Error al copiar el PDF", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    private String copyPdfToExternalStorage(Uri uri, String fileName) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);

            File externalDir = getExternalFilesDir(null);
            File outFile = new File(externalDir, fileName); // Usar nombre original

            OutputStream outputStream = new FileOutputStream(outFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return outFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("PDF_DEBUG", "Error al copiar PDF", e);
            return null;
        }
    }


    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }



    private void openPdfWithMuPDF(String filePath) {
        try {
            File file = new File(filePath);
            Uri uri = Uri.fromFile(file); // Esta URI es válida para MuPDF

            Log.d("PDF_DEBUG", "¿Existe el archivo? " + file.exists());
            Log.d("PDF_DEBUG", "Abriendo con MuPDF: " + file.getAbsolutePath());

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setClassName("com.giosoft.lectorpdf", "com.artifex.mupdf.viewer.DocumentActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            startActivity(intent);
        } catch (Exception e) {
            Log.e("PDF_DEBUG", "Error al abrir con MuPDF", e);
            Toast.makeText(this, "No se puede abrir el documento: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sharePdf(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(this, "Archivo no encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri contentUri = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".fileprovider", // Muy importante que coincida con el authorities del manifest
                file
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Compartir PDF usando"));
    }


}

