package com.tb.tetrisbrick.game.ui.start;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.tb.tetrisbrick.game.R;
import com.tb.tetrisbrick.game.ads.AdsManager;
import com.tb.tetrisbrick.game.data.GameStateStore;
import com.tb.tetrisbrick.game.data.SharedPreferencesManager;
import com.tb.tetrisbrick.game.databinding.ActivityStartBinding;
import com.tb.tetrisbrick.game.ui.main.MainActivity;
import com.tb.tetrisbrick.game.ui.score.ScoreActivity;
import com.tb.tetrisbrick.game.ui.settings.SettingsActivity;
import com.tb.tetrisbrick.game.utils.AnimationUtil;
import com.tb.tetrisbrick.game.utils.EdgeToEdgeUtils;
import com.tb.tetrisbrick.game.utils.ScreenshotMode;

public class StartActivity extends AppCompatActivity {

    private ActivityStartBinding binding;
    private GameStateStore gameStateStore;
    private SharedPreferencesManager sharedPreferencesManager;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeUtils.applySystemBarInsets(binding.getRoot());
        gameStateStore = new GameStateStore(this);
        sharedPreferencesManager = new SharedPreferencesManager(this);
        setTitleAnimation();
        setButtonAnimation();

        binding.bStartGame.setOnClickListener(v -> startGame());
        binding.bContinueGame.setOnClickListener(v -> continueGame());
        binding.bOpenScores.setOnClickListener(v -> openScores());
        binding.bOpenSettings.setOnClickListener(v -> openSettings());

        if (ScreenshotMode.isActive(this)) {
            binding.adView.setVisibility(View.GONE);
        } else {
            AdsManager.requestConsentThenLoadBanner(this, binding.adView);
        }
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
        refreshGameState();
    }

    // Re-evaluated every time this screen becomes visible (not just onCreate()), since a
    // saved game or a new best score can appear while this Activity was backgrounded -
    // e.g. the user played a game, backed out to Home, and returned via the back stack.
    private void refreshGameState() {
        boolean hasSavedGame = gameStateStore.hasSavedGame();
        binding.bContinueGame.setVisibility(hasSavedGame ? View.VISIBLE : View.GONE);

        int best = sharedPreferencesManager.getBestScore();
        binding.tvBestScore.setText(best > 0
                ? String.valueOf(best)
                : getString(R.string.empty_score_placeholder));
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
        if (gameStateStore.hasSavedGame()) {
            new AlertDialog.Builder(this, R.style.AppAlertDialogTheme)
                    .setTitle(R.string.discard_saved_game_title)
                    .setMessage(R.string.discard_saved_game_message)
                    .setPositiveButton(R.string.discard_saved_game_confirm, (dialog, which) -> launchNewGame())
                    .setNegativeButton(R.string.cancel_text, null)
                    .show();
        } else {
            launchNewGame();
        }
    }

    private void launchNewGame() {
        this.startActivity(propagateScreenshotMode(new Intent(this, MainActivity.class)));
    }

    void continueGame() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_RESUME_GAME, true);
        this.startActivity(propagateScreenshotMode(intent));
    }

    void openScores() {
        this.startActivity(propagateScreenshotMode(new Intent(this, ScoreActivity.class)));
    }

    void openSettings() {
        this.startActivity(propagateScreenshotMode(new Intent(this, SettingsActivity.class)));
    }

    // These internal-only activities aren't exported (correctly, for a real launcher
    // Activity vs. its internal screens), so they can't be launched directly via
    // `adb shell am start`. Forwards ScreenshotMode.EXTRA when present so capturing
    // clean store screenshots can still reach every screen by navigating from here,
    // same as any real user would - not a new capability, just carrying through an
    // extra this Activity itself may have been launched with.
    private Intent propagateScreenshotMode(Intent intent) {
        if (getIntent().getBooleanExtra(ScreenshotMode.EXTRA, false)) {
            intent.putExtra(ScreenshotMode.EXTRA, true);
        }
        return intent;
    }
}
