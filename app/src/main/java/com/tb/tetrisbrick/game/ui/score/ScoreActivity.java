package com.tb.tetrisbrick.game.ui.score;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.tb.tetrisbrick.game.R;
import com.tb.tetrisbrick.game.data.SharedPreferencesManager;
import com.tb.tetrisbrick.game.ui.settings.SettingsActivity;
import com.tb.tetrisbrick.game.utils.AnimationUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ScoreActivity<privateButton> extends AppCompatActivity {

    @BindView(R.id.tvFirstScore)
    TextView firstScore;

    @BindView(R.id.tvSecondScore)
    TextView secondScore;

    @BindView(R.id.tvThirdScore)
    TextView thirdScore;

    @BindView(R.id.llScores)
    LinearLayout scoresLayout;

    private AdView mAdView;

    private SharedPreferencesManager sharedPreferencesManager;

    private RewardedAd rewardedAd;
    private final String TAG = "ScoreActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);
        sharedPreferencesManager = new SharedPreferencesManager(getApplicationContext());
        ButterKnife.bind(this);
        scoresLayout.startAnimation(AnimationUtil.getZoomIn(this));
        firstScore.setText(sharedPreferencesManager.getFirstValue());
        secondScore.setText(sharedPreferencesManager.getSecondValue());
        thirdScore.setText(sharedPreferencesManager.getThirdValue());

        //Initialize the banner ads
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        //Load the banner ads
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        loadRewardedAd();
    }

    //Method is used to load the rewarded ads
    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Handle the error.
                Log.d(TAG, loadAdError.toString());
                rewardedAd = null;
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        // Called when a click is recorded for an ad.
                        Log.d(TAG, "Ad was clicked.");
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        Log.d(TAG, "Ad dismissed fullscreen content.");
                        rewardedAd = null;
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        super.onAdFailedToShowFullScreenContent(adError);
                        Log.e(TAG, "Ad failed to show fullscreen content.");
                        rewardedAd = null;
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        // Called when an impression is recorded for an ad.
                        Log.d(TAG, "Ad recorded an impression.");
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent();
                        // Called when ad is shown.
                        Log.d(TAG, "Ad showed fullscreen content.");
                    }
                });
                Log.d(TAG, "Ad was loaded.");
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        loadRewardedAd();
        if (rewardedAd != null) {
            Activity activityContext = ScoreActivity.this;
            rewardedAd.show(activityContext, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // Handle the reward.
                    Log.d(TAG, "The user earned the reward.");
                    int rewardAmount = rewardItem.getAmount();
                    String rewardType = rewardItem.getType();
                }
            });
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.");
        }
    }
}
