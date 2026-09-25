package com.tb.tetrisbrick.game.ui.main;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.tb.tetrisbrick.game.R;
import com.tb.tetrisbrick.game.Values;
import com.tb.tetrisbrick.game.ads.AdsManager;
import com.tb.tetrisbrick.game.databinding.ActivityMainBinding;
import com.tb.tetrisbrick.game.ui.main.listeners.OnTimerStateChangedListener;
import com.tb.tetrisbrick.game.utils.DebouncedOnClickListener;
import com.tb.tetrisbrick.game.utils.EdgeToEdgeUtils;
import com.tb.tetrisbrick.game.utils.ScreenshotMode;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements OnTimerStateChangedListener {

    // Set by StartActivity's "Continue" action to request a restore on a fresh Intent
    // launch - restoreGameIfAvailable() otherwise only runs when Android itself is
    // recreating this screen (non-null savedInstanceState), never on a deliberate
    // from-Home launch.
    public static final String EXTRA_RESUME_GAME = "com.tb.tetrisbrick.game.EXTRA_RESUME_GAME";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeUtils.applySystemBarInsets(binding.getRoot());

        binding.playingArea.setDependencies(binding.tvScore, binding.tvNextFigure, this);
        boolean shouldResume = savedInstanceState != null
                || getIntent().getBooleanExtra(EXTRA_RESUME_GAME, false);
        if (shouldResume) {
            binding.playingArea.restoreGameIfAvailable();
        } else {
            binding.playingArea.startFreshGame();
        }
        binding.ivRotate.setOnClickListener(new DebouncedOnClickListener(Values.DEBOUNCE_DELAY_IN_MILLIS) {
            @Override
            public void onDebouncedClick(View v) {
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                binding.playingArea.rotate();
            }
        });
        binding.ivMoveDown.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            moveDown();
        });
        binding.ivPausePlay.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            pausePlay();
        });

        if (ScreenshotMode.isActive(this)) {
            binding.adView.setVisibility(View.GONE);
        } else {
            AdsManager.requestConsentThenLoadBanner(this, binding.adView);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.adView.resume();
        if (binding.playingArea.isTimerRunning()) {
            binding.playingArea.startTimer();
            setControlsEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        // Must happen here, not in onStop(): Android runs the incoming activity's
        // onResume() (e.g. StartActivity refreshing its "Continue" button) BEFORE this
        // activity's onStop() - saving in onStop() would race that check and lose.
        // onPause() is guaranteed to finish first.
        binding.playingArea.saveGameState();
        binding.adView.pause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        binding.playingArea.cancelTimer();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        binding.playingArea.cleanup();
        binding.adView.destroy();
        super.onDestroy();
    }

    private void setControlsEnabled(boolean isRunning) {
        binding.ivRotate.setEnabled(isRunning);
        binding.ivMoveDown.setEnabled(isRunning);
    }

    void moveDown() {
        binding.playingArea.fastMoveDown();
    }

    void pausePlay() {
        binding.playingArea.handleTimerState();
    }

    @Override
    public void isTimerRunning(boolean isRunning) {
        binding.ivPausePlay.setImageResource(isRunning ? R.drawable.ic_pause : R.drawable.ic_resume);
        setControlsEnabled(isRunning);
    }

    @Override
    public void disableAllControls() {
        binding.ivPausePlay.setEnabled(false);
        setControlsEnabled(false);
    }

    @Override
    public void onGameOver(int finalScore, boolean isNewRecord) {
        if (isFinishing() || isDestroyed()) return;
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_game_over, null);
        ((TextView) dialogView.findViewById(R.id.tvGameOverScore)).setText(String.valueOf(finalScore));
        dialogView.findViewById(R.id.tvNewRecord).setVisibility(isNewRecord ? View.VISIBLE : View.GONE);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        ((Button) dialogView.findViewById(R.id.bReplay)).setOnClickListener(v -> {
            dialog.dismiss();
            binding.playingArea.startFreshGame();
            setControlsEnabled(true);
            binding.ivPausePlay.setEnabled(true);
            binding.ivPausePlay.setImageResource(R.drawable.ic_pause);
        });
        ((Button) dialogView.findViewById(R.id.bHome)).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        dialog.show();
    }
}
