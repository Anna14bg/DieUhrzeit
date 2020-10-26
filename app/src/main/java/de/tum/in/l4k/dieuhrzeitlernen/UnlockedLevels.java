package de.tum.in.l4k.dieuhrzeitlernen;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

public class UnlockedLevels extends AppCompatActivity {
    ImageView exit, card;

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
        setContentView(R.layout.activity_unlocked_levels);

        SharedPreferences sharedprefs = getSharedPreferences("DieUhrzeitData", Context.MODE_PRIVATE);
        int levelNr = sharedprefs.getInt("unlocked",0);
        final Resources res = getResources();

        for (int i=0; i <= levelNr; i++) {
            if (levelNr < 0 || levelNr > 11) continue; // should't happen
            final int cardId = res.getIdentifier("card"+i,
                    "id", getPackageName());
            card = findViewById(cardId);

            final int resourceId = res.getIdentifier("unlocked"+i,
                    "drawable", getPackageName());

            card.setImageResource(resourceId);

            final int choice = i;
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((ImageView) view).setColorFilter(Color.rgb(200, 200, 200), android.graphics.PorterDuff.Mode.MULTIPLY);
                    Intent intent = new Intent(UnlockedLevels.this,Instructions.class);
                    intent.putExtra("startLevel", choice);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }
            });
        }

        exit = findViewById(R.id.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit.setColorFilter(Color.rgb(200, 200, 200), android.graphics.PorterDuff.Mode.MULTIPLY);
                LayoutInflater inflater = LayoutInflater.from(UnlockedLevels.this);
                View alert = inflater.inflate(R.layout.alert_dialog, null);

                final AlertDialog builder = new AlertDialog.Builder(UnlockedLevels.this).setView(alert).create();
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
    }
}