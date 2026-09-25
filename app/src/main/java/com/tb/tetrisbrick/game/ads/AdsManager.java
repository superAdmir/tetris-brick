package com.tb.tetrisbrick.game.ads;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

public class AdsManager {

    private static final String TAG = "AdsManager";
    private static volatile boolean mobileAdsInitialized = false;

    private AdsManager() {
    }

    public static void requestConsentThenLoadBanner(@NonNull Activity activity, @NonNull AdView adView) {
        ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(activity);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();

        consentInformation.requestConsentInfoUpdate(activity, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: " + formError.getMessage());
                    }
                    initializeAndLoad(activity, adView);
                }),
                requestConsentError -> {
                    // Never block gameplay on a consent-flow failure - proceed with ads
                    // (subject to whatever consent state, if any, is already on record).
                    Log.w(TAG, "Consent info update failed: " + requestConsentError.getMessage());
                    initializeAndLoad(activity, adView);
                });
    }

    private static void initializeAndLoad(Activity activity, AdView adView) {
        if (!mobileAdsInitialized) {
            MobileAds.initialize(activity, status -> mobileAdsInitialized = true);
        }
        // Diagnostics only - a failed/offline/no-fill load is never retried and never
        // blocks or otherwise affects gameplay; the AdView just stays empty.
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                Log.w(TAG, "Banner ad failed to load: " + adError.getMessage());
            }
        });
        adView.loadAd(new AdRequest.Builder().build());
    }

    public static boolean isPrivacyOptionsRequired(@NonNull Activity activity) {
        return UserMessagingPlatform.getConsentInformation(activity).getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public static void openPrivacyOptions(@NonNull Activity activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
            if (formError != null) Log.w(TAG, "Privacy options form error: " + formError.getMessage());
        });
    }
}
