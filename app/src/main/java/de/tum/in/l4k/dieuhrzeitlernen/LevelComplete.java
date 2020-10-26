package de.tum.in.l4k.dieuhrzeitlernen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class LevelComplete extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    static LevelComplete activity;
    Animation moveupAnim, fadeinAnim;
    ImageView level, ribbon;
    TextView result;
    static final int TOTAL_TASKS = 10;
    MediaPlayer mediaPlayer;
    SharedPreferences sharedprefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        setContentView(R.layout.activity_level_complete);
        activity = this;

        result = findViewById(R.id.result);

        final int correctAnswers = getIntent().getIntExtra("correct", 0);
        result.setText(correctAnswers + " / " + TOTAL_TASKS);

        final Resources res = getResources();

        if (correctAnswers >= TOTAL_TASKS*0.8f) {
            final int completed_tone = res.getIdentifier("completed_tone", "raw", getPackageName());
            mediaPlayer = MediaPlayer.create(LevelComplete.this, completed_tone);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer player) {
                    player.start();
                }
            });
        }

        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (correctAnswers < TOTAL_TASKS*0.8f) {
                    MediaPlayer mediaPlayer = MediaPlayer.create(LevelComplete.this, R.raw.gameover);
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        public void onPrepared(MediaPlayer player) {
                            player.start();
                        }
                    });

                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(LevelComplete.this, UnlockedLevels.class);
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            finish();
                        }
                    }, 3000);
                } else {
                    int levelNr = getIntent().getIntExtra("levelCompleted", 0) + 1;
                    level = findViewById(R.id.level);
                    final int resourceId = res.getIdentifier("level"+levelNr,
                            "drawable", getPackageName());

                    level.setImageResource(resourceId);
                    ribbon = findViewById(R.id.ribbon);
                    fadeinAnim = AnimationUtils.loadAnimation(LevelComplete.this, R.anim.fadein_animation);
                    moveupAnim = AnimationUtils.loadAnimation(LevelComplete.this, R.anim.moveup_animation);
                    level.startAnimation(fadeinAnim);
                    ribbon.startAnimation(moveupAnim);
                    final int sound = res.getIdentifier((levelNr == 12 ? "winner" : "level"), "raw", getPackageName());
                    mediaPlayer = MediaPlayer.create(LevelComplete.this, sound);
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        public void onPrepared(MediaPlayer player) {
                            player.start();
                        }
                    });

                    if (levelNr < 12) {
                        sharedprefs = getSharedPreferences("DieUhrzeitData", Context.MODE_PRIVATE);
                        int lastData = sharedprefs.getInt("unlocked",0);
                        if (levelNr > lastData) {
                            SharedPreferences.Editor editor = sharedprefs.edit();
                            editor.putInt("unlocked", levelNr);
                            editor.commit();
                        }
                    }

                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(LevelComplete.this, UnlockedLevels.class);
                            startActivity(intent);
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                            finish();
                        }
                    }, 5000);
                }
            }
        },2000);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }
}