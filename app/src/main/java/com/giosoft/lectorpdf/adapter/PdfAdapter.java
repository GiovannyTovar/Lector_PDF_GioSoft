package com.giosoft.lectorpdf.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        holder.pdfIcon.setImageResource(R.drawable.ic_pdf); // icono PDF vector

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
