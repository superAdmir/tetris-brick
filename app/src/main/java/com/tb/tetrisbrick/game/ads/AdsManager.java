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

import java.util.concurrent.atomic.AtomicBoolean;

public class AdsManager {

    private static final String TAG = "AdsManager";
    private static volatile boolean mobileAdsInitialized = false;

    private AdsManager() {
    }

    public static void requestConsentThenLoadBanner(@NonNull Activity activity, @NonNull AdView adView) {
        ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(activity);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();

        // Guards this single call against ever starting two loads for the same banner,
        // in case either UMP callback below were to fire more than once.
        AtomicBoolean bannerLoadStarted = new AtomicBoolean(false);

        consentInformation.requestConsentInfoUpdate(activity, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: " + formError.getMessage());
                    }
                    // canRequestAds() must be checked only after the form flow has fully
                    // resolved (shown-and-dismissed, or not required) - checking it any
                    // earlier can read a stale pre-form consent state.
                    boolean canRequestAds = consentInformation.canRequestAds();
                    Log.d(TAG, "Consent update succeeded, canRequestAds=" + canRequestAds);
                    if (canRequestAds) {
                        initializeAndLoad(activity, adView, bannerLoadStarted);
                    } else {
                        Log.d(TAG, "canRequestAds() is false - not requesting a banner");
                    }
                }),
                requestConsentError -> {
                    // Never block gameplay on a consent-flow failure, but still respect
                    // whatever consent state (if any) is already on record.
                    boolean canRequestAds = consentInformation.canRequestAds();
                    Log.w(TAG, "Consent info update failed: " + requestConsentError.getMessage()
                            + ", canRequestAds=" + canRequestAds);
                    if (canRequestAds) {
                        initializeAndLoad(activity, adView, bannerLoadStarted);
                    } else {
                        Log.d(TAG, "canRequestAds() is false - not requesting a banner");
                    }
                });
    }

    private static void initializeAndLoad(Activity activity, AdView adView, AtomicBoolean bannerLoadStarted) {
        // Diagnostics only - a failed/offline/no-fill load is never retried and never
        // blocks or otherwise affects gameplay; the AdView just stays empty.
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Banner loaded");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                Log.w(TAG, "Banner failed to load - code=" + adError.getCode()
                        + " message=" + adError.getMessage());
            }
        });

        if (mobileAdsInitialized) {
            loadBanner(adView, bannerLoadStarted);
        } else {
            // loadBanner() runs only inside this completion callback - never right after
            // initialize() is called - so the SDK is actually ready before the request.
            MobileAds.initialize(activity, status -> {
                mobileAdsInitialized = true;
                Log.d(TAG, "Mobile Ads initialization completed");
                loadBanner(adView, bannerLoadStarted);
            });
        }
    }

    private static void loadBanner(AdView adView, AtomicBoolean bannerLoadStarted) {
        if (!bannerLoadStarted.compareAndSet(false, true)) {
            return;
        }
        Log.d(TAG, "Banner load requested");
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
