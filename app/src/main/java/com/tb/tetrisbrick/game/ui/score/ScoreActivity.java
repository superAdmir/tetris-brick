package com.tb.tetrisbrick.game.ui.score;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.tb.tetrisbrick.game.R;
import com.tb.tetrisbrick.game.ads.AdsManager;
import com.tb.tetrisbrick.game.data.SharedPreferencesManager;
import com.tb.tetrisbrick.game.databinding.ActivityScoreBinding;
import com.tb.tetrisbrick.game.utils.AnimationUtil;
import com.tb.tetrisbrick.game.utils.EdgeToEdgeUtils;
import com.tb.tetrisbrick.game.utils.ScreenshotMode;

public class ScoreActivity extends AppCompatActivity {

    private ActivityScoreBinding binding;

    private SharedPreferencesManager sharedPreferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeUtils.applySystemBarInsets(binding.getRoot());
        sharedPreferencesManager = new SharedPreferencesManager(getApplicationContext());
        binding.llScores.startAnimation(AnimationUtil.getZoomIn(this));
        binding.tvFirstScore.setText(displayValue(sharedPreferencesManager.getFirstValue()));
        binding.tvSecondScore.setText(displayValue(sharedPreferencesManager.getSecondValue()));
        binding.tvThirdScore.setText(displayValue(sharedPreferencesManager.getThirdValue()));

        if (ScreenshotMode.isActive(this)) {
            binding.adView.setVisibility(View.GONE);
        } else {
            AdsManager.requestConsentThenLoadBanner(this, binding.adView);
        }
    }

    private String displayValue(String value) {
        return "0".equals(value) ? getString(R.string.empty_score_placeholder) : value;
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.adView.resume();
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
}
