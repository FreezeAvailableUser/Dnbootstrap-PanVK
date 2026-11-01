package git.artdeell.dnbootstrap;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.io.FileUtils;

import java.io.IOException;

import git.artdeell.dnbootstrap.assets.AppDirs;
import git.artdeell.dnbootstrap.utils.Utils;

public class ManageDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_data);
        findViewById(R.id.manage_space_uninstall_game).setOnClickListener(v->performAction(this::uninstallGame));
        findViewById(R.id.manage_space_uninstall_components).setOnClickListener(v->performAction(this::uninstallComponents));
        findViewById(R.id.manage_space_uninstall_everything).setOnClickListener(v->eraseAppData());
        findViewById(R.id.manage_space_open_data_directory).setOnClickListener(v->{
            Utils.openPath(this, getFilesDir(), false);
        });
    }



    private void performAction(UninstallAction action) {
        new Thread(()->{
            try {
                action.performAction();
                showDone();
            }catch (IOException e) {
                Utils.showErrorDialog(this, e, false);
            }
        }).start();
    }

    private void uninstallGame() throws IOException {
        FileUtils.deleteDirectory(new AppDirs(getFilesDir()).vs);
    }

    private void uninstallComponents() throws IOException {
        AppDirs appDirs = new AppDirs(getFilesDir());
        FileUtils.deleteDirectory(appDirs.vs);
        FileUtils.deleteDirectory(appDirs.runtime);
        FileUtils.deleteDirectory(appDirs.fontconfig);
    }

    private void eraseAppData() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.clearApplicationUserData();
    }

    private void showDone() {
        runOnUiThread(()-> Toast.makeText(this, R.string.manage_data_done, Toast.LENGTH_SHORT).show());
    }

    private interface UninstallAction {
        void performAction() throws IOException;
    }
}
