package com.giosoft.lectorpdf;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.Manifest;

import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artifex.mupdf.viewer.BuildConfig;
import com.artifex.mupdf.viewer.DocumentActivity;
import com.giosoft.lectorpdf.adapter.PdfAdapter;
import com.giosoft.lectorpdf.model.PdfGroup;
import com.giosoft.lectorpdf.model.PdfHistoryManager;
import com.giosoft.lectorpdf.model.PdfItem;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OPEN_DOCUMENT = 1;

    private Uri uriPendiente = null;
    private String lastOpenedPdfPath = null; // Ruta del último PDF abierto
    private PdfAdapter pdfAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        getTheme().applyStyle(R.style.Theme_LectorPDFGioSoft, true);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Forzar el color blanco en el ícono de los tres puntos
        toolbar.getOverflowIcon().setTint(ContextCompat.getColor(this, android.R.color.white));


        RecyclerView recyclerView = findViewById(R.id.pdfRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<PdfGroup> groupedHistory = PdfHistoryManager.getPdfHistoryGrouped(this);
        pdfAdapter = new PdfAdapter(this, groupedHistory);
        recyclerView.setAdapter(pdfAdapter);


        // Configurar el gesto de deslizar para eliminar
        ItemTouchHelper.SimpleCallback swipeToDeleteCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // No permitir arrastrar y soltar
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                // Verificar que no sea un encabezado
                if (pdfAdapter.isHeaderPosition(position)) {
                    pdfAdapter.notifyItemChanged(position);
                    return;
                }

                // Obtener el item a eliminar
                PdfItem item = pdfAdapter.getItemForPosition(position);
                if (item != null) {
                    // Mostrar diálogo de confirmación
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Eliminar documento")
                            .setMessage("¿Estás seguro de que quieres eliminar este documento del historial?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                // Eliminar el archivo copiado
                                File file = new File(item.getPath());
                                if (file.exists()) {
                                    file.delete();
                                }

                                // Eliminar del historial
                                PdfHistoryManager.removePdfItem(MainActivity.this, item);

                                // Actualizar la lista
                                pdfAdapter.updateData(PdfHistoryManager.getPdfHistoryGrouped(MainActivity.this));

                                Toast.makeText(MainActivity.this, "Documento eliminado", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancelar", (dialog, which) -> {
                                // Restaurar el item si se cancela
                                pdfAdapter.notifyItemChanged(position);
                            })
                            .show();
                } else {
                    pdfAdapter.notifyItemChanged(position); // Restaurar el item si no se puede eliminar
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    Paint paint = new Paint();

                    // Configurar fondo rojo claro al deslizar
                    if (dX > 0) {
                        // Deslizando hacia la derecha
                        paint.setColor(Color.parseColor("#FFCDD2")); // Rojo claro
                        c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                                (float) itemView.getBottom(), paint);
                    } else {
                        // Deslizando hacia la izquierda
                        paint.setColor(Color.parseColor("#FFCDD2")); // Rojo claro
                        c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                                (float) itemView.getRight(), (float) itemView.getBottom(), paint);
                    }

                    // Dibujar icono de eliminar
                    Drawable deleteIcon = ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_delete);
                    if (deleteIcon != null) {
                        int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                        if (dX > 0) {
                            // Deslizando hacia la derecha
                            int iconLeft = itemView.getLeft() + iconMargin;
                            int iconRight = itemView.getLeft() + iconMargin + deleteIcon.getIntrinsicWidth();
                            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        } else {
                            // Deslizando hacia la izquierda
                            int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                            int iconRight = itemView.getRight() - iconMargin;
                            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        }

                        deleteIcon.draw(c);
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);




        // Boton Eliminar el historial completo
        ImageButton btnClearHistory = findViewById(R.id.btnClearHistory);
        btnClearHistory.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar historial")
                    .setMessage("¿Estás seguro de que quieres eliminar todo el historial?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        PdfHistoryManager.clearHistory(this);
                        pdfAdapter.updateData(PdfHistoryManager.getPdfHistoryGrouped(this));
                        Toast.makeText(this, "Historial eliminado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Botón para abrir el PDF
        Button openPdfButton = findViewById(R.id.openPdfButton);
        openPdfButton.setOnClickListener(v -> openPdfFile());


        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            uriPendiente = data;

            if ("content".equals(data.getScheme())) {
                try {
                    // Obtener solo READ o WRITE (ignorar PERSISTABLE)
                    int takeFlags = intent.getFlags() &
                            (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    if (takeFlags != 0) {
                        // Solo tomar permisos persistentes si hay READ o WRITE
                        getContentResolver().takePersistableUriPermission(data, takeFlags);
                    } else {
                        Log.w("PDF_DEBUG", "El Intent no otorgó READ/WRITE. Usando acceso temporal.");
                        // Opción 1: Abrir el PDF sin persistencia (temporal)
                        try (InputStream pdfStream = getContentResolver().openInputStream(data)) {
                            // Leer el PDF (permiso temporal)
                        } catch (IOException e) {
                            Log.e("PDF_DEBUG", "Error al leer PDF: " + e.getMessage());
                        }

                        // Opción 2: Pedir permisos persistentes manualmente
                        Intent openIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        openIntent.addCategory(Intent.CATEGORY_OPENABLE);
                        openIntent.setType("application/pdf");
                        startActivityForResult(openIntent, 101); // 101 = Código de solicitud
                    }
                } catch (SecurityException e) {
                    Log.e("PDF_DEBUG", "Error de permisos: " + e.getMessage());
                }
            }

            openPdfFromUri(data);
        }
    }

    private void openPdfFromUri(Uri uri) {
        // Verificar si la URI es un enlace web (comienza con http:// o https://)
        Log.d("PDF_DEBUG", "URI seleccionada: " + uri.toString());

        // Comprobar si la URI es un enlace web, usando los esquemas 'http' o 'https'
        String uriString = uri.toString().toLowerCase();

        if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            Log.d("PDF_DEBUG", "Es un enlace web, no un archivo PDF.");
            Toast.makeText(this, "Este es un enlace, no un archivo PDF", Toast.LENGTH_SHORT).show();
            return; // Detener la ejecución si es un enlace
        }

        // Si la URI no es un enlace
        try {
            String fileName = getFileNameFromUri(uri);
            String copiedPath = copyPdfToExternalStorage(uri, fileName);

            if (copiedPath != null) {
                // Guardar en el historial
                PdfHistoryManager.savePdfItem(this, new PdfItem(copiedPath, fileName, System.currentTimeMillis()));
                pdfAdapter.updateData(PdfHistoryManager.getPdfHistoryGrouped(this));

                // Intent para abrir con MuPDF
                openPdfWithMuPDF(copiedPath);
            } else {
                Toast.makeText(this, "Error al procesar el archivo", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("PDF_DEBUG", "Error al copiar PDF: " + e.getMessage(), e);  // Depuración
            Toast.makeText(this, "Error al copiar el PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflar el menú de la Action Bar
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about) {
            showDialogAbout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void showDialogAbout() {
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_about, null);

        // Crear el diálogo sin botones predeterminados
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .create();

        dialog.show();

        // Configurar el click listener del botón personalizado
        Button closeButton = dialog.findViewById(R.id.closeButton);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dialog.dismiss());
        }

        // Configurar la copia al portapapeles
        TextView donationAccount = dialog.findViewById(R.id.donationAccount);
        if (donationAccount != null) {
            donationAccount.setOnClickListener(v -> {
                // Copiar al portapapeles
                String accountNumber = donationAccount.getText().toString();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Número de cuenta", accountNumber);
                clipboard.setPrimaryClip(clip);

                // Mostrar Snackbar SOBRE el diálogo
                View dialogView = dialog.getWindow().getDecorView();
                Snackbar snackbar = Snackbar.make(dialogView, "Número Nequi copiado", Snackbar.LENGTH_LONG);

                // Personalización opcional del Snackbar
                View sbView = snackbar.getView();
                sbView.setBackgroundColor(ContextCompat.getColor(this, R.color.teal_700)); // Color del fondo
                TextView textView = sbView.findViewById(com.google.android.material.R.id.snackbar_text);
                textView.setTextColor(Color.WHITE); // Color del texto

                snackbar.setAction("OK", v1 -> snackbar.dismiss());
                snackbar.setActionTextColor(Color.WHITE);
                snackbar.show();
            });

            // Esto evita que el sistema lo interprete como número telefónico
            donationAccount.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            donationAccount.setAutoLinkMask(0);
            donationAccount.setLinksClickable(false);
        }
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

                    pdfAdapter.updateData(PdfHistoryManager.getPdfHistoryGrouped(this));
                    openPdfWithMuPDF(copiedPath);
                } else {
                    Toast.makeText(this, "Error al copiar el PDF", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String copyPdfToExternalStorage(Uri uri, String fileName) {
        if (uri == null || fileName == null) return null;
        File externalDir = getExternalFilesDir(null);
        File outFile = new File(externalDir, fileName);

        if (outFile.exists()) {
            Log.d("PDF_DEBUG", "Archivo ya existe: " + outFile.getAbsolutePath());
            return outFile.getAbsolutePath();
        }

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(outFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

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
            Uri uri = Uri.fromFile(file); // URI válida para MuPDF

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
                BuildConfig.APPLICATION_ID + ".fileprovider", // Importante que coincida con el authorities del manifest
                file
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Compartir PDF usando"));
    }


}

