package de.tum.in.l4k.dieuhrzeitlernen;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class Welcome extends AppCompatActivity {
    Animation shakeAnim;
    ImageView bird, exit, start, impressum;

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
        setContentView(R.layout.activity_welcome);

        bird = findViewById(R.id.bird);
        exit = findViewById(R.id.exit);
        start = findViewById(R.id.start);
        impressum = findViewById(R.id.impressum);
        shakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake_animation);

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit.setColorFilter(Color.rgb(200, 200, 200), android.graphics.PorterDuff.Mode.MULTIPLY);
                LayoutInflater inflater = LayoutInflater.from(Welcome.this);
                View alert = inflater.inflate(R.layout.alert_dialog, null);

                final AlertDialog builder = new AlertDialog.Builder(Welcome.this).setView(alert).create();
                builder.setCanceledOnTouchOutside(false);

                Button yes = alert.findViewById(R.id.yes);
                Button no = alert.findViewById(R.id.no);

                yes.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        builder.dismiss();
                        finish();
                    }
                });

                no.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        builder.cancel();
                        exit.clearColorFilter();
                    }
                });

                builder.show();
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                bird.startAnimation(shakeAnim);
            }
        },800);
        final MediaPlayer mediaPlayer = MediaPlayer.create(Welcome.this, R.raw.hallo);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer player) {
                player.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();
            }
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showMenu();
            }
        },3800);

        // make the bird move on click just for fun
        bird.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bird.startAnimation(shakeAnim);
            }
        });
    }

    void showMenu() {
        start.setVisibility(View.VISIBLE);
        impressum.setVisibility(View.VISIBLE);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start.setColorFilter(Color.rgb(200, 200, 200), android.graphics.PorterDuff.Mode.MULTIPLY);
                Intent intent = new Intent(Welcome.this,UnlockedLevels.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });

        impressum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                impressum.setColorFilter(Color.rgb(200, 200, 200), android.graphics.PorterDuff.Mode.MULTIPLY);
                LayoutInflater inflater = LayoutInflater.from(Welcome.this);
                View view1 = inflater.inflate(R.layout.impressum_dialog, null);

                final AlertDialog builder = new AlertDialog.Builder(Welcome.this).setView(view1).create();
                builder.setCanceledOnTouchOutside(false);

                TextView txt = view1.findViewById(R.id.txt);
                TextView close = view1.findViewById(R.id.x);
                String htmlAsString = getString(R.string.html);
                // set the html content on the TextView
                txt.setText(Html.fromHtml(htmlAsString));
                txt.setMovementMethod(new ScrollingMovementMethod());

                close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        builder.cancel();
                        impressum.clearColorFilter();
                    }
                });

                builder.show();
            }
        });
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