package com.tb.tetrisbrick.game.ui.main;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

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
import com.tb.tetrisbrick.game.Values;
import com.tb.tetrisbrick.game.ui.main.listeners.OnTimerStateChangedListener;
import com.tb.tetrisbrick.game.ui.main.views.PlayingAreaView;
import com.tb.tetrisbrick.game.ui.main.views.PreviewAreaView;
import com.tb.tetrisbrick.game.ui.main.views.ScoreView;
import com.tb.tetrisbrick.game.ui.settings.SettingsActivity;
import com.tb.tetrisbrick.game.utils.DebouncedOnClickListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements OnTimerStateChangedListener {

    @BindView(R.id.playingArea)
    PlayingAreaView playingAreaView;

    @BindView(R.id.tvScore)
    ScoreView scoreView;

    @BindView(R.id.tvNextFigure)
    PreviewAreaView previewAreaView;

    @BindView(R.id.ivPausePlay)
    ImageView playPauseImage;

    @BindView(R.id.ivRotate)
    ImageView rotateImage;

    @BindView(R.id.ivMoveDown)
    ImageView moveDownImage;

    private AdView mAdView;

    private RewardedAd rewardedAd;
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        playingAreaView.setDependencies(scoreView, previewAreaView, this);
        playingAreaView.cleanup();
        playingAreaView.createFigureWithDelay();
        ImageView rotate = findViewById(R.id.ivRotate);
        rotate.setOnClickListener(new DebouncedOnClickListener(Values.DEBOUNCE_DELAY_IN_MILLIS) {
            @Override
            public void onDebouncedClick(View v) {
                playingAreaView.rotate();
            }
        });

        //Initialize the rewarded ads
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        //Load the rewarded ads
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
    protected void onResume() {
        super.onResume();
        if (playingAreaView.isTimerRunning()) {
            playingAreaView.startTimer();
            setControlsEnabled(true);
        }
    }

    @Override
    protected void onStop() {
        playingAreaView.cancelTimer();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        playingAreaView.cleanup();
        super.onDestroy();
    }

    private void setControlsEnabled(boolean isRunning) {
        rotateImage.setEnabled(isRunning);
        moveDownImage.setEnabled(isRunning);
    }

    @OnClick(R.id.ivMoveDown)
    void moveDown() {
        playingAreaView.fastMoveDown();
    }

    @OnClick(R.id.ivPausePlay)
    void pausePlay() {
        playingAreaView.handleTimerState();
    }

    @Override
    public void isTimerRunning(boolean isRunning) {
        playPauseImage.setImageResource(isRunning ? R.drawable.ic_pause : R.drawable.ic_resume);
        setControlsEnabled(isRunning);
    }

    @Override
    public void disableAllControls() {
        playPauseImage.setEnabled(false);
        setControlsEnabled(false);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        loadRewardedAd();
        if (rewardedAd != null) {
            Activity activityContext = MainActivity.this;
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
