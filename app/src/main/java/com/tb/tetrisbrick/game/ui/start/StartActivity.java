package com.tb.tetrisbrick.game.ui.start;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.tb.tetrisbrick.game.ads.AdsManager;
import com.tb.tetrisbrick.game.databinding.ActivityStartBinding;
import com.tb.tetrisbrick.game.ui.main.MainActivity;
import com.tb.tetrisbrick.game.ui.score.ScoreActivity;
import com.tb.tetrisbrick.game.ui.settings.SettingsActivity;
import com.tb.tetrisbrick.game.utils.AnimationUtil;
import com.tb.tetrisbrick.game.utils.EdgeToEdgeUtils;

public class StartActivity extends AppCompatActivity {

    private ActivityStartBinding binding;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeUtils.applySystemBarInsets(binding.getRoot());
        setTitleAnimation();
        setButtonAnimation();

        binding.bStartGame.setOnClickListener(v -> startGame());
        binding.bOpenScores.setOnClickListener(v -> openScores());
        binding.bOpenSettings.setOnClickListener(v -> openSettings());

        AdsManager.requestConsentThenLoadBanner(this, binding.adView);
        requestNotificationPermissionIfNeeded();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.adView.resume();
        setTitleAnimation();
    }

    @Override
    protected void onPause() {
        binding.adView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        binding.adView.destroy();
        super.onDestroy();
    }

    private void setTitleAnimation() {
        binding.tvGameTitle.startAnimation(AnimationUtil.getZoomIn(this));
    }

    private void setButtonAnimation() {
        binding.bStartGame.startAnimation(AnimationUtil.getSlideInLeft(this));
        binding.bOpenScores.startAnimation(AnimationUtil.getSlideInRight(this));
    }

    void startGame() {
        this.startActivity(new Intent(this, MainActivity.class));
    }

    void openScores() {
        this.startActivity(new Intent(this, ScoreActivity.class));
    }

    void openSettings() {
        this.startActivity(new Intent(this, SettingsActivity.class));
    }
}
