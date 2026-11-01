package git.artdeell.dnbootstrap;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import git.artdeell.dnbootstrap.assets.AppDirs;
import git.artdeell.dnbootstrap.assets.AssetsExtractor;
import git.artdeell.dnbootstrap.io.IOUtil;

public class AssetsCheckActivity extends AppCompatActivity implements AssetsExtractor.ProgressCallback {
    private static AssetsExtractor extractorTask;
    private ProgressBar extractionProgress;
    private Button selectButton;
    private TextView assetsMessage;
    private ActivityResultLauncher<String> selectGameLauncher;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppDirs appDirs = new AppDirs(getFilesDir());
        if(appDirs.isFullyInstalled()) {
            exit();
            return;
        }
        setContentView(R.layout.activity_assets_check);
        extractionProgress = findViewById(R.id.extraction_progress);
        selectButton = findViewById(R.id.select_game_data_btn);
        assetsMessage = findViewById(R.id.assets_message);
        if(extractorTask != null) {
            connectExtractionTask();
        }else if(appDirs.isGameInstalled()) {
            componentsOnlyExtract();
        }else {
            selectGameLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), this::fullExtract);
            selectButton.setOnClickListener((v)-> selectGameLauncher.launch("application/gzip"));
        }
    }

    private void fullExtract(Uri gameUri) {
        long gameSize = IOUtil.getFileSize(getContentResolver(), gameUri);
        extractorTask = new AssetsExtractor(getApplicationContext(), gameUri, gameSize);
        connectExtractionTask();
        new Thread(extractorTask).start();
    }

    private void componentsOnlyExtract() {
        extractorTask = new AssetsExtractor(getApplicationContext(), null, 0);
        connectExtractionTask();
        new Thread(extractorTask).start();
    }

    private void connectExtractionTask() {
        assetsMessage.setText(R.string.extract_runtime);
        selectButton.setVisibility(View.GONE);
        extractionProgress.setIndeterminate(!extractorTask.progressAvailable());
        onProgressChanged();
        extractorTask.setCallback(this);
    }

    private void exit() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        if(extractorTask != null) {
            extractorTask.setCallback(null);
            extractorTask = null;
        }
    }

    @Override
    public void onProgressChanged() {
        runOnUiThread(()->{
            extractionProgress.setProgress((int) (extractorTask.getProgress() * extractionProgress.getMax()));
            if(extractorTask.progressComplete()) exit();
        });
    }
}
