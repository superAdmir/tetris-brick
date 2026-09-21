package com.tb.tetrisbrick.game.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tb.tetrisbrick.game.R;
import com.tb.tetrisbrick.game.ads.AdsManager;
import com.tb.tetrisbrick.game.data.SharedPreferencesManager;
import com.tb.tetrisbrick.game.databinding.ActivitySettingsBinding;
import com.tb.tetrisbrick.game.utils.Utils;
import com.tb.tetrisbrick.game.utils.EdgeToEdgeUtils;
import com.shawnlin.numberpicker.NumberPicker;

import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.jetbrains.annotations.NotNull;

public class SettingsActivity extends AppCompatActivity implements SettingsView {

    private ActivitySettingsBinding binding;

    private List<TextView> speedItems;

    private NumberPicker squaresNumberPicker;

    private androidx.appcompat.widget.SwitchCompat enableHintsSwitch;

    private SettingsPresenter settingsPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EdgeToEdgeUtils.applySystemBarInsets(binding.getRoot());
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
        findViewById(R.id.flPrivacyOptions).setOnClickListener(v -> AdsManager.openPrivacyOptions(this));
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

        AdsManager.requestConsentThenLoadBanner(this, binding.adView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.adView.resume();
        if (settingsPresenter != null) settingsPresenter.setValues();
        findViewById(R.id.flPrivacyOptions).setVisibility(
                AdsManager.isPrivacyOptionsRequired(this) ? View.VISIBLE : View.GONE);
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

    @Override
    public void markChosenColor(String oldColorKey, int newItemId) {
        ImageView oldImageView = findViewById(Utils.getViewIdByColorKey(oldColorKey));
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
            final Drawable wrappedDrawable = getDrawable(item, R.color.colorSurfaceAlt);
            item.setBackground(wrappedDrawable);
            item.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
        TextView newItem = findViewById(newItemId);
        if (newItem != null) {
            final Drawable wrappedDrawable = getDrawable(newItem, R.color.colorPrimary);
            newItem.setBackground(wrappedDrawable);
            newItem.setTextColor(getResources().getColor(R.color.colorOnAccent));
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
