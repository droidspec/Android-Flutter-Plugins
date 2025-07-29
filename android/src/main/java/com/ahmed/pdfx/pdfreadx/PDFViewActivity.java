package com.ahmed.pdfx.pdfreadx;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ahmed.pdfx.pdfreadx.views.ZoomableImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PDFViewActivity extends AppCompatActivity {
    private String fileName, filePath;
    private TextView zoomLevelText;
    private TextView pageInfo;
    private ImageButton btnPrevious, btnNext;
    private int currentPageIndex = 0;
    private ZoomableImageView imageView;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    public void setCurrentPageIndex(int currentPageIndex) {
        this.currentPageIndex = currentPageIndex;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfview);

        zoomLevelText = findViewById(R.id.zoomLevelText);
        imageView = findViewById(R.id.pdfPageImageView);
        pageInfo = findViewById(R.id.pageInfo);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);

        btnPrevious.setOnClickListener(v -> showPage(currentPageIndex - 1));
        btnNext.setOnClickListener(v -> showPage(currentPageIndex + 1));

        btnPrevious.setEnabled(false);
        btnNext.setEnabled(false);

        imageView.setZoomListener(scale -> {
            int percent = (int) (scale / imageView.getMinScale() * 100);
            zoomLevelText.setText(percent + "%");
            zoomLevelText.setVisibility(View.VISIBLE);
            zoomLevelText.postDelayed(() -> {
                zoomLevelText.setVisibility(View.GONE);
            }, 1500);
        });

        // Set up swipe listener
        imageView.setOnSwipeListener(new ZoomableImageView.OnSwipeListener() {
            @Override
            public void onSwipeLeft() {
                showPage(currentPageIndex + 1);
            }

            @Override
            public void onSwipeRight() {
                showPage(currentPageIndex - 1);
            }

            @Override
            public void onSwipeProgress(float offset, boolean isRightSwipe) {
                // Optional: Add visual feedback during swipe
                // For example, you could dim the current page slightly
            }
        });

        String messageFromFlutter = getIntent().getStringExtra("message_from_flutter");
        if (messageFromFlutter != null) {
            filePath = getIntent().getStringExtra("filePath");
            fileName = getIntent().getStringExtra("fileName");
            Toast.makeText(this, "Opening file " + fileName, Toast.LENGTH_SHORT).show();
            openPDF(filePath, fileName);
        } else {
            Toast.makeText(this, "No message received from Flutter", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPDF(String appFileDir, String fileName) {
        File file = new File(appFileDir, fileName);
        if (file.exists()) {
            openRenderer(file);
        } else {
            Toast.makeText(this, "PDF file not found", Toast.LENGTH_SHORT).show();
            Log.d("Failure", "PDF file not found");
        }
    }

    private void openRenderer(File file) {
        try {
            closeRenderer();

            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
            showPage(0);

            btnPrevious.setEnabled(false);
            btnNext.setEnabled(pdfRenderer.getPageCount() > 1);
        } catch (IOException e) {
            Toast.makeText(this, "Error opening PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showPage(int index) {
        if (pdfRenderer == null || pdfRenderer.getPageCount() <= index || index < 0) {
            return;
        }

        if (currentPage != null) {
            currentPage.close();
        }

        currentPageIndex = index;
        currentPage = pdfRenderer.openPage(index);

        int sampleSize = calculateSampleSize(currentPage.getWidth(), currentPage.getHeight());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;

        try {
            Bitmap bitmap = Bitmap.createBitmap(
                    currentPage.getWidth() / sampleSize,
                    currentPage.getHeight() / sampleSize,
                    Bitmap.Config.ARGB_8888);

            currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            imageView.setImageBitmap(bitmap);
            imageView.resetZoom();
        } catch (IllegalArgumentException e) {
            if (sampleSize == 1) {
                sampleSize = 2;
                Bitmap bitmap = Bitmap.createBitmap(
                        currentPage.getWidth() / sampleSize,
                        currentPage.getHeight() / sampleSize,
                        Bitmap.Config.ARGB_8888);
                currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                imageView.setImageBitmap(bitmap);
                imageView.resetZoom();
            }
        }

        updatePageInfo();
    }

    private int calculateSampleSize(int width, int height) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int reqWidth = displayMetrics.widthPixels * 2;
        int reqHeight = displayMetrics.heightPixels * 2;

        int sampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / sampleSize) >= reqHeight
                    && (halfWidth / sampleSize) >= reqWidth) {
                sampleSize *= 2;
            }
        }

        return sampleSize;
    }

    private void updatePageInfo() {
        pageInfo.setText(String.format("%d / %d", currentPageIndex + 1, pdfRenderer.getPageCount()));
        btnPrevious.setEnabled(currentPageIndex > 0);
        btnNext.setEnabled(currentPageIndex + 1 < pdfRenderer.getPageCount());
    }

    private void closeRenderer() {
        if (currentPage != null) {
            currentPage.close();
            currentPage = null;
        }
        if (pdfRenderer != null) {
            pdfRenderer.close();
            pdfRenderer = null;
        }
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (IOException e) {
                // ignored
            }
            parcelFileDescriptor = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeRenderer();
    }
}