package com.giosoft.lectorpdf;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.Manifest;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_PDF = 1;

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

        pdfImageView = findViewById(R.id.pdfImageView);  // Asegúrate de inicializar tu ImageView

        Button selectPdfButton = findViewById(R.id.selectPdfButton);
        selectPdfButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 1001);
        });

        // Inicializar los botones de navegación
        Button prevPageButton = findViewById(R.id.prevPageButton);
        Button nextPageButton = findViewById(R.id.nextPageButton);

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
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }

        // Aquí puedes continuar con tu lógica para abrir PDFs desde intent
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_VIEW.equals(action) && type != null && type.equals("application/pdf")) {
            Uri pdfUri = intent.getData();
            if (pdfUri != null) {
                Log.d("PDF", "PDF recibido desde otra app: " + pdfUri.toString());
                // Aquí puedes visualizar el PDF
            }
        }
    }

    private void openFilePicker() {
        // Crear un intent para abrir el selector de archivos PDF
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_PICK_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Uri selectedPdfUri = data.getData();
            if (selectedPdfUri != null) {
                try {
                    // Abrir el PDF con un ParcelFileDescriptor
                    ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedPdfUri, "r");
                    if (parcelFileDescriptor != null) {
                        // Inicializar el PdfRenderer con el descriptor del archivo
                        pdfRenderer = new PdfRenderer(parcelFileDescriptor);

                        // Renderizar la primera página
                        renderPdf(0);  // Mostrar la primera página del PDF
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

            // Ajustar tamaño según el factor de escala
            int width = (int) (currentPage.getWidth() * scaleFactor);
            int height = (int) (currentPage.getHeight() * scaleFactor);

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

            // Ajustar tamaño según el factor de escala
            int width = (int) (currentPage.getWidth() * scaleFactor);
            int height = (int) (currentPage.getHeight() * scaleFactor);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            pdfImageView.setImageBitmap(bitmap);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (scaleGestureDetector != null) {
            scaleGestureDetector.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }


}
