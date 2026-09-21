package com.tb.tetrisbrick.game.ui.score;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.tb.tetrisbrick.game.ads.AdsManager;
import com.tb.tetrisbrick.game.data.SharedPreferencesManager;
import com.tb.tetrisbrick.game.databinding.ActivityScoreBinding;
import com.tb.tetrisbrick.game.utils.AnimationUtil;

public class ScoreActivity extends AppCompatActivity {

    private ActivityScoreBinding binding;

    private SharedPreferencesManager sharedPreferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sharedPreferencesManager = new SharedPreferencesManager(getApplicationContext());
        binding.llScores.startAnimation(AnimationUtil.getZoomIn(this));
        binding.tvFirstScore.setText(sharedPreferencesManager.getFirstValue());
        binding.tvSecondScore.setText(sharedPreferencesManager.getSecondValue());
        binding.tvThirdScore.setText(sharedPreferencesManager.getThirdValue());

        AdsManager.requestConsentThenLoadBanner(this, binding.adView);
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
