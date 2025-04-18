package com.giosoft.lectorpdf.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.artifex.mupdf.viewer.BuildConfig;
import com.giosoft.lectorpdf.R;
import com.giosoft.lectorpdf.model.PdfHistoryManager;
import com.giosoft.lectorpdf.model.PdfItem;

import java.io.File;
import java.util.List;
public class PdfAdapter extends RecyclerView.Adapter<PdfAdapter.PdfViewHolder> {

    private Context context;
    private List<PdfItem> pdfList;

    public PdfAdapter(Context context, List<PdfItem> pdfList) {
        this.context = context;
        this.pdfList = pdfList;
    }

    @NonNull
    @Override
    public PdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pdf, parent, false);
        return new PdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfViewHolder holder, int position) {
        PdfItem item = pdfList.get(position);
        holder.pdfNameTextView.setText(item.getName());
        holder.pdfPathTextView.setText(item.getPath());
        holder.pdfIcon.setImageResource(R.drawable.ic_pdf);

        // Configurar clic en toda la tarjeta
        holder.itemView.setOnClickListener(v -> {
            File file = new File(item.getPath());
            if (file.exists()) {
                openPdfWithMuPDF(context, item.getPath());
            } else {
                Toast.makeText(context, "Archivo no encontrado", Toast.LENGTH_SHORT).show();
            }
        });

        holder.shareButton.setOnClickListener(v -> {
            File file = new File(item.getPath());
            if (!file.exists()) {
                Toast.makeText(context, "Archivo no encontrado", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri contentUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider", // Usa tu nombre de paquete
                    file
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF usando"));
        });
    }

    // Método para abrir PDF (similar al de MainActivity)
    private void openPdfWithMuPDF(Context context, String filePath) {
        try {
            File file = new File(filePath);
            Uri uri = Uri.fromFile(file);

            // Actualizar el historial ANTES de abrir el PDF
            for (PdfItem item : pdfList) {
                if (item.getPath().equals(filePath)) {
                    // Guardar como recientemente abierto
                    PdfHistoryManager.savePdfItem(context, new PdfItem(
                            item.getPath(),
                            item.getName(),
                            System.currentTimeMillis() // Actualiza timestamp
                    ));
                    break;
                }
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setClassName("com.giosoft.lectorpdf", "com.artifex.mupdf.viewer.DocumentActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            context.startActivity(intent);

            // Actualizar la lista después de abrir
            List<PdfItem> updatedHistory = PdfHistoryManager.getPdfHistory(context);
            updateData(updatedHistory);

        } catch (Exception e) {
            Toast.makeText(context, "Error al abrir el PDF", Toast.LENGTH_SHORT).show();
            Log.e("PDF_ADAPTER", "Error al abrir PDF", e);
        }
    }

    @Override
    public int getItemCount() {
        return pdfList.size();
    }

    public void updateData(List<PdfItem> newList) {
        this.pdfList = newList;
        notifyDataSetChanged();
    }

    static class PdfViewHolder extends RecyclerView.ViewHolder {
        TextView pdfNameTextView, pdfPathTextView;
        ImageView pdfIcon;
        ImageButton shareButton;

        public PdfViewHolder(@NonNull View itemView) {
            super(itemView);
            pdfNameTextView = itemView.findViewById(R.id.pdfName);
            pdfIcon = itemView.findViewById(R.id.pdfIcon);
            pdfPathTextView = itemView.findViewById(R.id.pdfPath);
            shareButton = itemView.findViewById(R.id.shareButton);
        }
    }
}
