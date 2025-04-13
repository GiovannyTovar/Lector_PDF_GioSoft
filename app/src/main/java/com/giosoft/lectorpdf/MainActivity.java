package com.giosoft.lectorpdf;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_PDF = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button selectPdfButton = findViewById(R.id.selectPdfButton);

        selectPdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Abre el selector de archivos PDF
                openFilePicker();
            }
        });
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

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_PICK_PDF) {
            if (data != null) {
                Uri selectedPdfUri = data.getData();
                if (selectedPdfUri != null) {
                    // Aquí puedes pasar la URI al siguiente paso para abrir el PDF
                    openPdfViewer(selectedPdfUri);
                }
            }
        }
    }

    private void openPdfViewer(Uri pdfUri) {
        try {
            // Obtener el archivo desde la URI
            ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(pdfUri, "r");
            if (parcelFileDescriptor != null) {
                // Crear un PdfRenderer con el archivo
                PdfRenderer pdfRenderer = new PdfRenderer(parcelFileDescriptor);

                // Mostrar el primer página del PDF
                PdfRenderer.Page page = pdfRenderer.openPage(0);

                // Aquí puedes usar un ImageView o un Canvas para mostrar la página del PDF en la interfaz
                // Ejemplo de renderizar en una ImageView (suponiendo que tienes un ImageView en tu layout)
                ImageView pdfImageView = findViewById(R.id.pdfImageView);
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                pdfImageView.setImageBitmap(bitmap);

                // Cerrar la página
                page.close();
                pdfRenderer.close();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error al abrir el PDF", Toast.LENGTH_SHORT).show();
        }
    }
}
