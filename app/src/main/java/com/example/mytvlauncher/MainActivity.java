package com.example.mytvlauncher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private FrameLayout rootLayout;
    private ImageView backgroundView;
    private TextView widgetBola;
    private GridView appsGrid;
    private List<ResolveInfo> tvApps;
    
    // Konfigurasi Tema Kosmetik (Bisa dikembangkan lebih lanjut)
    private int highlightColor = 0xFF33B5E5; // Warna fokus remote (Default Biru)
    private static final int REQUEST_WALLPAPER = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Root Kontainer Utama
        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.BLACK);
        setContentView(rootLayout);

        // 2. Layer Background (Wallpaper)
        backgroundView = new ImageView(this);
        backgroundView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        rootLayout.addView(backgroundView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 3. Widget Jadwal Bola (Pojok Kiri Atas)
        LinearLayout widgetContainer = new LinearLayout(this);
        widgetContainer.setOrientation(LinearLayout.VERTICAL);
        widgetContainer.setPadding(40, 40, 40, 40);
        widgetContainer.setBackgroundColor(Color.parseColor("#80000000")); // Transparan hitam biar teks terbaca
        
        TextView widgetTitle = new TextView(this);
        widgetTitle.setText("⚽ JADWAL BOLA HARI INI (WIB)");
        widgetTitle.setTextColor(Color.YELLOW);
        widgetTitle.setTextSize(16);
        widgetContainer.addView(widgetTitle);

        widgetBola = new TextView(this);
        widgetBola.setText("Memuat jadwal...");
        widgetBola.setTextColor(Color.WHITE);
        widgetBola.setTextSize(14);
        widgetContainer.addView(widgetBola);

        FrameLayout.LayoutParams widgetParams = new FrameLayout.LayoutParams(450, ViewGroup.LayoutParams.WRAP_CONTENT);
        widgetParams.gravity = Gravity.TOP | Gravity.LEFT;
        widgetParams.setMargins(30, 30, 0, 0);
        rootLayout.addView(widgetContainer, widgetParams);

        // 4. Tombol Ganti Tema & Wallpaper (Pojok Kanan Atas)
        LinearLayout settingsContainer = new LinearLayout(this);
        settingsContainer.setPadding(20, 20, 20, 20);
        
        Button btnWallpaper = new Button(this);
        btnWallpaper.setText("Ganti Gambar");
        btnWallpaper.setOnClickListener(v -> openGallery());
        settingsContainer.addView(btnWallpaper);

        Button btnColor = new Button(this);
        btnColor.setText("Ganti Warna");
        btnColor.setOnClickListener(v -> {
            // Rotasi warna tema sederhana saat diklik
            highlightColor = (highlightColor == 0xFF33B5E5) ? 0xFFFFBB33 : 0xFF99CC00;
            appsGrid.invalidateViews();
        });
        settingsContainer.addView(btnColor);

        FrameLayout.LayoutParams settingsParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        settingsParams.gravity = Gravity.TOP | Gravity.RIGHT;
        rootLayout.addView(settingsContainer, settingsParams);

        // 5. Baris Aplikasi Di Bagian Bawah (Satu Baris Horisontal / Row)
        appsGrid = new GridView(this);
        appsGrid.setNumColumns(GridView.AUTO_FIT);
        appsGrid.setColumnWidth(150);
        appsGrid.setHorizontalSpacing(15);
        appsGrid.setPadding(30, 20, 30, 20);
        appsGrid.setBackgroundColor(Color.parseColor("#90111111")); // Background dock bawah transparan

        FrameLayout.LayoutParams gridParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 200); // Tinggi dock aplikasi diatur agar tipis di bawah
        gridParams.gravity = Gravity.BOTTOM;
        rootLayout.addView(appsGrid, gridParams);

        // Ambil Data Aplikasi & Jadwal Bola
        loadTvApps();
        new FetchJadwalBolaTask().execute();
    }

    private void loadTvApps() {
        Intent tvIntent = new Intent(Intent.ACTION_MAIN, null);
        tvIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        tvApps = getPackageManager().queryIntentActivities(tvIntent, 0);

        appsGrid.setAdapter(new ArrayAdapter<ResolveInfo>(this, 0, tvApps) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ResolveInfo info = tvApps.get(position);
                
                LinearLayout itemLayout = new LinearLayout(getContext());
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                itemLayout.setGravity(Gravity.CENTER);
                itemLayout.setPadding(10, 10, 10, 10);
                itemLayout.setFocusable(true); // Supaya merespon D-Pad remote

                ImageView icon = new ImageView(getContext());
                icon.setImageDrawable(info.loadIcon(getPackageManager()));
                itemLayout.addView(icon, new LinearLayout.LayoutParams(70, 70));

                TextView label = new TextView(getContext());
                label.setText(info.loadLabel(getPackageManager()));
                label.setTextColor(Color.WHITE);
                label.setTextSize(11);
                label.setGravity(Gravity.CENTER);
                label.setSingleLine(true);
                itemLayout.addView(label);

                // Feedback visual navigasi remote TV
                itemLayout.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        v.setBackgroundColor(highlightColor);
                        v.setScaleX(1.1f); v.setScaleY(1.1f); // Efek zoom sedikit
                    } else {
                        v.setBackgroundColor(Color.TRANSPARENT);
                        v.setScaleX(1.0f); v.setScaleY(1.0f);
                    }
                });

                itemLayout.setOnClickListener(v -> {
                    Intent launchIntent = getPackageManager().getLeanbackLaunchIntentForPackage(info.activityInfo.packageName);
                    if (launchIntent != null) startActivity(launchIntent);
                });

                return itemLayout;
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_WALLPAPER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WALLPAPER && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                InputStream imageStream = getContentResolver().openInputStream(selectedImage);
                Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                backgroundView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Metode Async untuk Mengambil Data Jadwal Bola dari API Publik Tanpa Token
    private class FetchJadwalBolaTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                // Menggunakan API publik skor/jadwal terbuka yang gratis
                URL url = new URL("https://scorebat.com");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
                reader.close();

                JSONObject response = new JSONObject(json.toString());
                JSONArray responseArray = response.getJSONArray("response");
                StringBuilder jadwalBuilder = new StringBuilder();

                // Ambil maksimal 5 pertandingan liga top yang sedang berjalan/akan datang hari ini
                int count = 0;
                for (int i = 0; i < responseArray.length() && count < 5; i++) {
                    JSONObject match = responseArray.getJSONObject(i);
                    String title = match.getString("title"); // Contoh: "Arsenal vs Chelsea"
                    String competition = match.getJSONObject("competition").getString("name");
                    
                    // Filter liga top secara sederhana
                    if (competition.contains("Premier League") || competition.contains("La Liga") || 
                        competition.contains("Serie A") || competition.contains("Champions League")) {
                        jadwalBuilder.append("• ").append(title).append("\n(").append(competition).append(")\n\n");
