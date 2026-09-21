package com.tb.tetrisbrick.game.ui.settings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
import com.tb.tetrisbrick.game.data.SharedPreferencesManager;
import com.tb.tetrisbrick.game.databinding.ActivitySettingsBinding;
import com.tb.tetrisbrick.game.ui.score.ScoreActivity;
import com.tb.tetrisbrick.game.utils.Utils;
import com.shawnlin.numberpicker.NumberPicker;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.jetbrains.annotations.NotNull;

public class SettingsActivity extends AppCompatActivity implements SettingsView {

    private ActivitySettingsBinding binding;

    private List<TextView> speedItems;

    private NumberPicker squaresNumberPicker;

    private Switch enableHintsSwitch;

    private SettingsPresenter settingsPresenter;

    private AdView mAdView;

    private RewardedAd rewardedAd;
    private final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        speedItems = Arrays.asList(findViewById(R.id.tvVeryFast), findViewById(R.id.tvFast),
                findViewById(R.id.tvDefault), findViewById(R.id.tvSlow), findViewById(R.id.tvVerySlow));
        squaresNumberPicker = binding.squaresCountNumberPicker;
        enableHintsSwitch = findViewById(R.id.sEnableHints);
        settingsPresenter = new SettingsPresenter(this,
                new SharedPreferencesManager(getApplicationContext()));
        squaresNumberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> settingsPresenter.setSquareCountInRow(newVal));

        findViewById(R.id.flMoreApps).setOnClickListener(v -> showMoreApps());
        enableHintsSwitch.setOnClickListener(v -> enableHints());
        findViewById(R.id.flRate).setOnClickListener(v -> rateApp());
        findViewById(R.id.vLFigureColor).setOnClickListener(v -> chooseColorFirst());
        findViewById(R.id.vSquareFigureColor).setOnClickListener(v -> chooseColorSecond());
        findViewById(R.id.vLongFigureColor).setOnClickListener(v -> chooseColorThird());
        findViewById(R.id.vZFigureColor).setOnClickListener(v -> chooseColorFourth());
        findViewById(R.id.vTFigureColor).setOnClickListener(v -> chooseColorFifth());
        findViewById(R.id.vJFigureColor).setOnClickListener(v -> chooseColorSixth());
        findViewById(R.id.tvVerySlow).setOnClickListener(v -> chooseVerySlowSpeed());
        findViewById(R.id.tvSlow).setOnClickListener(v -> chooseSlowSpeed());
        findViewById(R.id.tvDefault).setOnClickListener(v -> chooseDefaultSpeed());
        findViewById(R.id.tvFast).setOnClickListener(v -> chooseFastSpeed());
        findViewById(R.id.tvVeryFast).setOnClickListener(v -> chooseVeryFastSpeed());

        //Initialize the rewarded ads
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        //Load the rewarded ads
        //Load the banner ads
        mAdView = binding.adView;
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
        if (settingsPresenter != null) settingsPresenter.setValues();
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        loadRewardedAd();
        if (rewardedAd != null) {
            Activity activityContext = SettingsActivity.this;
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

    @Override
    public void markChosenColor(int oldColor, int newItemId) {
        ImageView oldImageView = findViewById(Utils.getViewIdByColor(oldColor));
        if (oldImageView != null) {
            oldImageView.setImageDrawable(null);
        }
        ImageView newImageView = findViewById(newItemId);
        if (newImageView == null) {
            newImageView = findViewById(R.id.vZFigureColor);
        }
        newImageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_ok));
    }

    @Override
    public void setSpeedTitle(int newItemId) {
        for (TextView item : speedItems) {
            final Drawable wrappedDrawable = getDrawable(item, R.color.white);
            item.setBackground(wrappedDrawable);
            item.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
        TextView newItem = findViewById(newItemId);
        if (newItem != null) {
            final Drawable wrappedDrawable = getDrawable(newItem, R.color.colorPrimary);
            newItem.setBackground(wrappedDrawable);
            newItem.setTextColor(getResources().getColor(R.color.white));
        }
    }

    @NotNull
    private Drawable getDrawable(TextView item, int colorId) {
        final Drawable wrappedDrawable = DrawableCompat.wrap(item.getBackground());
        DrawableCompat.setTintList(wrappedDrawable, ColorStateList.valueOf(getResources().getColor(colorId)));
        return wrappedDrawable;
    }

    @Override
    public void setVerticalHintsChecked(boolean hintsEnabled) {
        enableHintsSwitch.setChecked(hintsEnabled);
    }

    @Override
    public void setSquaresCountInRow(int squaresCountInRow) {
        if (squaresNumberPicker != null) squaresNumberPicker.setValue(squaresCountInRow);
    }

    void showMoreApps() {
        try {
            startActivity(Utils.showMoreApps());
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getResources().getString(R.string.cannot_open_market_error_text), Toast.LENGTH_LONG).show();
        }
    }


    void enableHints() {
        settingsPresenter.getEvent(R.id.sEnableHints);
    }


    void rateApp() {
        try {
            startActivity(Utils.openMarket(this));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getResources().getString(R.string.cannot_open_market_error_text), Toast.LENGTH_LONG).show();
        }
    }

    void chooseColorFirst() {
        settingsPresenter.getEvent(R.id.vLFigureColor);
    }

    void chooseColorSecond() {
        settingsPresenter.getEvent(R.id.vSquareFigureColor);
    }

    void chooseColorThird() {
        settingsPresenter.getEvent(R.id.vLongFigureColor);
    }

    void chooseColorFourth() {
        settingsPresenter.getEvent(R.id.vZFigureColor);
    }

    void chooseColorFifth() {
        settingsPresenter.getEvent(R.id.vTFigureColor);
    }

    void chooseColorSixth() {
        settingsPresenter.getEvent(R.id.vJFigureColor);
    }

    void chooseVerySlowSpeed() {
        settingsPresenter.getEvent(R.id.tvVerySlow);
    }

    void chooseSlowSpeed() {
        settingsPresenter.getEvent(R.id.tvSlow);
    }

    void chooseDefaultSpeed() {
        settingsPresenter.getEvent(R.id.tvDefault);
    }

    void chooseFastSpeed() {
        settingsPresenter.getEvent(R.id.tvFast);
    }

    void chooseVeryFastSpeed() {
        settingsPresenter.getEvent(R.id.tvVeryFast);
    }

}
