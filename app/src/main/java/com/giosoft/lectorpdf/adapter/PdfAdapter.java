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
import com.giosoft.lectorpdf.model.PdfGroup;
import com.giosoft.lectorpdf.model.PdfHistoryManager;
import com.giosoft.lectorpdf.model.PdfItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
public class PdfAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private Context context;
    private List<PdfGroup> pdfGroups;

    public PdfAdapter(Context context, List<PdfGroup> pdfGroups) {
        this.context = context;
        this.pdfGroups = pdfGroups;
    }


    @Override
    public int getItemViewType(int position) {
        int accumulatedPosition = 0;
        for (PdfGroup group : pdfGroups) {
            if (position == accumulatedPosition) {
                return TYPE_HEADER;
            }
            accumulatedPosition += 1; // header

            if (position < accumulatedPosition + group.getItems().size()) {
                return TYPE_ITEM;
            }
            accumulatedPosition += group.getItems().size();
        }
        throw new IllegalArgumentException("Invalid position");
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_group_header, parent, false);
            return new GroupHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_pdf, parent, false);
            return new PdfViewHolder(view);
        }
    }



    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_HEADER) {
            GroupHeaderViewHolder headerHolder = (GroupHeaderViewHolder) holder;
            PdfGroup group = getGroupForPosition(position);
            headerHolder.groupTitle.setText(group.getTitle());
        } else {
            PdfViewHolder pdfHolder = (PdfViewHolder) holder;
            PdfItem item = getItemForPosition(position);

            if (item != null) {
                // Configurar el ViewHolder
                pdfHolder.pdfNameTextView.setText(item.getName());
                pdfHolder.pdfPathTextView.setText(item.getPath());
                pdfHolder.pdfIcon.setImageResource(R.drawable.ic_pdf);

                // Configurar clic en toda la tarjeta
                pdfHolder.itemView.setOnClickListener(v -> {
                    if (new File(item.getPath()).exists()) {
                        openPdfWithMuPDF(context, item.getPath());
                    } else {
                        Toast.makeText(context, "Archivo no encontrado", Toast.LENGTH_SHORT).show();
                    }
                });

                pdfHolder.shareButton.setOnClickListener(v -> {
                    File file = new File(item.getPath());
                    if (!file.exists()) {
                        Toast.makeText(context, "Archivo no encontrado", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Uri contentUri = FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".fileprovider",
                            file
                    );

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/pdf");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF usando"));
                });
            } else {
                // Manejar caso cuando item es null
                pdfHolder.pdfNameTextView.setText("Archivo no disponible");
                pdfHolder.pdfPathTextView.setText("");
                pdfHolder.itemView.setOnClickListener(v ->
                        Toast.makeText(context, "Este archivo no está disponible", Toast.LENGTH_SHORT).show());
            }
        }
    }


    private PdfGroup getGroupForPosition(int position) {
        int accumulatedPosition = 0;
        for (PdfGroup group : pdfGroups) {
            int groupSize = group.getItems().size() + 1; // +1 for header
            if (position < accumulatedPosition + groupSize) {
                return group;
            }
            accumulatedPosition += groupSize;
        }
        return null;
    }


    private PdfItem getItemForPosition(int position) {
        int accumulatedPosition = 0;
        for (PdfGroup group : pdfGroups) {
            int groupSize = group.getItems().size() + 1; // +1 for header
            if (position < accumulatedPosition + groupSize) {
                if (position == accumulatedPosition) {
                    return null; // It's a header
                }
                return group.getItems().get(position - accumulatedPosition - 1);
            }
            accumulatedPosition += groupSize;
        }
        return null;
    }


    @Override
    public int getItemCount() {
        int count = 0;
        for (PdfGroup group : pdfGroups) {
            count += group.getItems().size() + 1; // +1 for header
        }
        return count;
    }


    public void updateData(List<PdfGroup> newGroups) {
        this.pdfGroups = new ArrayList<>(newGroups); // Crear nueva instancia para evitar mutabilidad
        notifyDataSetChanged();
    }


    static class GroupHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView groupTitle;

        public GroupHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            groupTitle = itemView.findViewById(R.id.groupTitle);
        }
    }



    private void openPdfWithMuPDF(Context context, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(context, "El archivo no existe", Toast.LENGTH_SHORT).show();
                return;
            }

            // Actualizar el historial ANTES de abrir el PDF
            for (PdfGroup group : pdfGroups) {
                for (PdfItem item : group.getItems()) {
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
            }

            // Abrir el PDF
            Uri uri = Uri.fromFile(file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setClassName("com.giosoft.lectorpdf", "com.artifex.mupdf.viewer.DocumentActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            context.startActivity(intent);

            // Actualizar la lista después de abrir
            List<PdfGroup> updatedGroups = PdfHistoryManager.getPdfHistoryGrouped(context);
            updateData(updatedGroups);

        } catch (Exception e) {
            Toast.makeText(context, "Error al abrir el PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("PDF_ADAPTER", "Error al abrir PDF", e);
        }
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
