package com.tb.tetrisbrick.game.ui.main;

import android.os.Bundle;
import android.view.View;

import com.tb.tetrisbrick.game.R;
import com.tb.tetrisbrick.game.Values;
import com.tb.tetrisbrick.game.ads.AdsManager;
import com.tb.tetrisbrick.game.databinding.ActivityMainBinding;
import com.tb.tetrisbrick.game.ui.main.listeners.OnTimerStateChangedListener;
import com.tb.tetrisbrick.game.utils.DebouncedOnClickListener;
import com.tb.tetrisbrick.game.utils.EdgeToEdgeUtils;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements OnTimerStateChangedListener {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeUtils.applySystemBarInsets(binding.getRoot());

        binding.playingArea.setDependencies(binding.tvScore, binding.tvNextFigure, this);
        if (savedInstanceState == null) {
            binding.playingArea.startFreshGame();
        } else {
            binding.playingArea.restoreGameIfAvailable();
        }
        binding.ivRotate.setOnClickListener(new DebouncedOnClickListener(Values.DEBOUNCE_DELAY_IN_MILLIS) {
            @Override
            public void onDebouncedClick(View v) {
                binding.playingArea.rotate();
            }
        });
        binding.ivMoveDown.setOnClickListener(v -> moveDown());
        binding.ivPausePlay.setOnClickListener(v -> pausePlay());

        AdsManager.requestConsentThenLoadBanner(this, binding.adView);
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
        binding.adView.pause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        binding.playingArea.saveGameState();
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
}
