package com.giosoft.lectorpdf;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.Manifest;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_PDF = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private int currentPageIndex = 0;
    private ImageView pdfImageView;

    // Variables para zoom
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar los botones de navegación
        ImageButton prevPageButton = findViewById(R.id.prevPageButton);
        ImageButton nextPageButton = findViewById(R.id.nextPageButton);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pdfImageView = findViewById(R.id.pdfImageView);  // Asegúrate de inicializar tu ImageView

        Button selectPdfButton = findViewById(R.id.selectPdfButton);
        selectPdfButton.setOnClickListener(v -> openFilePicker());



        prevPageButton.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                currentPageIndex--;
                loadPage(currentPageIndex);
            }
        });

        nextPageButton.setOnClickListener(v -> {
            if (currentPageIndex < pdfRenderer.getPageCount() - 1) {
                currentPageIndex++;
                loadPage(currentPageIndex);
            }
        });

        // Inicializar el detector de gestos de escala
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                // Ajustamos el factor de escala basado en el gesto
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));

                // Renderizar la página con el nuevo factor de escala
                if (currentPage != null) {
                    renderPdf(scaleFactor);
                }
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;  // Comienza el gesto de escala
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                // No hacemos nada cuando termina el gesto de escala
            }
        });

        // Solicitar permiso para leer almacenamiento (Android < 13)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    // Método para abrir el selector de archivos PDF
    private void openFilePicker() {
        // Verificar permisos antes de continuar
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            // Crear un intent para abrir el selector de archivos PDF
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, REQUEST_CODE_PICK_PDF);
        } else {
            // Si no tienes el permiso, solicita el permiso
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    // Manejo de la respuesta del permiso
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Si el permiso es concedido, abre el selector de archivos
                openFilePicker();
            } else {
                Toast.makeText(this, "Permiso denegado para acceder al almacenamiento", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_PDF && resultCode == RESULT_OK && data != null) {
            Uri selectedPdfUri = data.getData();
            if (selectedPdfUri != null) {
                try {
                    // Abrir el PDF con un ParcelFileDescriptor
                    ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedPdfUri, "r");
                    if (parcelFileDescriptor != null) {
                        // Inicializar el PdfRenderer con el descriptor del archivo
                        pdfRenderer = new PdfRenderer(parcelFileDescriptor);

                        // Renderizar la primera página
                        currentPageIndex = 0;
                        renderPdf(scaleFactor); // ✅ Usa el factor actual Mostrar la primera página del PDF
                    }
                } catch (IOException e) {
                    Toast.makeText(this, "Error al abrir el PDF", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void renderPdf(float scaleFactor) {
        if (pdfRenderer != null && currentPageIndex >= 0 && currentPageIndex < pdfRenderer.getPageCount()) {
            // Cerrar la página anterior si está abierta
            if (currentPage != null) {
                currentPage.close();
            }

            // Abrir la nueva página
            currentPage = pdfRenderer.openPage(currentPageIndex);

            // Ajustar tamaño según el factor de escala, pero limitando a un tamaño razonable
            int baseWidth = currentPage.getWidth();
            int baseHeight = currentPage.getHeight();

            if (baseWidth <= 0 || baseHeight <= 0) {
                Toast.makeText(this, "No se pudo renderizar el PDF: tamaño inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            // Limitar el tamaño del bitmap para evitar errores de memoria
            int maxWidth = 2048;  // Máxima anchura del bitmap
            int maxHeight = 2048; // Máxima altura del bitmap

            int width = (int) (baseWidth * scaleFactor);
            int height = (int) (baseHeight * scaleFactor);

            // Limitar el tamaño del bitmap
            width = Math.min(width, maxWidth);
            height = Math.min(height, maxHeight);

            // Crear el bitmap con las nuevas dimensiones
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // Renderizar la página
            currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            // Mostrar el PDF en el ImageView
            pdfImageView.setImageBitmap(bitmap);
        }
    }


    private void loadPage(int pageIndex) {
        if (pdfRenderer != null && pageIndex >= 0 && pageIndex < pdfRenderer.getPageCount()) {
            if (currentPage != null) {
                currentPage.close();
            }

            currentPage = pdfRenderer.openPage(pageIndex);

            int baseWidth = currentPage.getWidth();
            int baseHeight = currentPage.getHeight();

            if (baseWidth <= 0 || baseHeight <= 0) {
                Toast.makeText(this, "No se pudo renderizar la página: tamaño inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            int width = (int) (baseWidth * scaleFactor);
            int height = (int) (baseHeight * scaleFactor);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            pdfImageView.setImageBitmap(bitmap);

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Asegúrate de que el evento toque se pase correctamente al detector de gestos
        if (scaleGestureDetector != null) {
            scaleGestureDetector.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfRenderer != null) {
            pdfRenderer.close();  // Cierra el PdfRenderer
        }
    }

}
