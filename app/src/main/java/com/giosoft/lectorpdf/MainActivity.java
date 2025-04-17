package com.giosoft.lectorpdf;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.Manifest;

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

import com.artifex.mupdf.viewer.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OPEN_DOCUMENT = 1;

    private String lastOpenedPdfPath = null; // Ruta del último PDF abierto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configuración de Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

    // Acción al seleccionar "Compartir"
    //@Override
    /*public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            if (lastOpenedPdfPath != null) {
                sharePdf(lastOpenedPdfPath);
            } else {
                Toast.makeText(this, "Primero abre un PDF para compartirlo", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

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

                String path = copyPdfToExternalStorage(uri);
                if (path != null) {
                    lastOpenedPdfPath = path; // Guardamos para compartir después
                    openPdfWithMuPDF(path);
                } else {
                    Toast.makeText(this, "Error al copiar el PDF", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }



    private String copyPdfToExternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);

            File externalDir = getExternalFilesDir(null); // Ruta válida externa
            File outFile = new File(externalDir, "documento.pdf");

            OutputStream outputStream = new FileOutputStream(outFile);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            Log.d("PDF_DEBUG", "Archivo copiado a: " + outFile.getAbsolutePath());
            Log.d("PDF_DEBUG", "Existe: " + outFile.exists() + " | Tamaño: " + outFile.length());

            return outFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("PDF_DEBUG", "Error al copiar PDF", e);
            return null;
        }
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

