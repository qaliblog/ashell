package com.qali.ashell;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class StartupActivity extends Activity {

    private static final int PICK_ISO_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TerminalPreferences settings = new TerminalPreferences(this);
        boolean forceSettings = getIntent().getBooleanExtra("force_settings", false);
        boolean hasIso = settings.getCustomIsoUri(this) != null || new File(Config.getDataDirectory(this), Config.CDROM_IMAGE_NAME).exists();

        if (!forceSettings && (settings.isSetupDone() || hasIso)) {
            startActivity(new Intent(this, TerminalActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_startup);

        findViewById(R.id.download_default_iso).setOnClickListener(v -> {
            downloadDefaultIso();
        });

        findViewById(R.id.pick_iso_file).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_ISO_REQUEST_CODE);
        });

        findViewById(R.id.start_terminal).setOnClickListener(v -> {
            startActivity(new Intent(this, TerminalActivity.class));
            finish();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_ISO_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                copyIsoToInternalStorage(uri);
            }
        }
    }

    private void downloadDefaultIso() {
        String url = "https://dl-cdn.alpinelinux.org/alpine/v3.14/releases/x86_64/alpine-virt-3.14.2-x86_64.iso";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Downloading Alpine Linux");
        request.setDescription("Downloading default system image");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(this, null, "alpine.iso");

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = manager.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId == id) {
                    File downloadedFile = new File(getExternalFilesDir(null), "alpine.iso");
                    if (downloadedFile.exists()) {
                        TerminalPreferences prefs = new TerminalPreferences(StartupActivity.this);
                        prefs.setCustomIsoUri(StartupActivity.this, downloadedFile.getAbsolutePath());
                        Toast.makeText(StartupActivity.this, "Download Complete", Toast.LENGTH_SHORT).show();
                    }
                    unregisterReceiver(this);
                }
            }
        };
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        Toast.makeText(this, "Download Started", Toast.LENGTH_SHORT).show();
    }

    private void copyIsoToInternalStorage(Uri uri) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                File outputFile = new File(Config.getDataDirectory(this), "custom.iso");
                FileOutputStream outputStream = new FileOutputStream(outputFile);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();

                runOnUiThread(() -> {
                    TerminalPreferences prefs = new TerminalPreferences(this);
                    prefs.setCustomIsoUri(this, outputFile.getAbsolutePath());
                    Toast.makeText(this, "ISO Copied and Selected", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(Config.APP_LOG_TAG, "Failed to copy ISO", e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to copy ISO", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
