package com.tb.tetrisbrick.game.ui.start;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.tb.tetrisbrick.game.databinding.ActivityStartBinding;
import com.tb.tetrisbrick.game.ui.main.MainActivity;
import com.tb.tetrisbrick.game.ui.score.ScoreActivity;
import com.tb.tetrisbrick.game.ui.settings.SettingsActivity;
import com.tb.tetrisbrick.game.utils.AnimationUtil;

public class StartActivity extends AppCompatActivity {

    private ActivityStartBinding binding;

    private AdView mAdView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitleAnimation();
        setButtonAnimation();

        binding.bStartGame.setOnClickListener(v -> startGame());
        binding.bOpenScores.setOnClickListener(v -> openScores());
        binding.bOpenSettings.setOnClickListener(v -> openSettings());

        //Initialize the banner ads
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        //Load the banner ads
        mAdView = binding.adView;
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitleAnimation();
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
