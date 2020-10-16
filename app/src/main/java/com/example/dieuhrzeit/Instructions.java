package com.example.dieuhrzeit;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

// Imitates Game Activity, but only plays predefined animations instead of implementing onTouchListeners
public class Instructions extends AppCompatActivity {
    ImageView hrArrow, minArrow, exit, hint, assess, star, congrats, hand, dim;
    TextView x;
    ProgressBar progressBar;
    Animation zoomAnim;
    TranslateAnimation animation;
    float minRotation = 0;          // what angle should the source image be rotated at
    float hrRotation = 90;          // what angle should the source image be rotated at
    float centerX, centerY;         // the actual center coordinates of the clock
    int taskNr;                     // current task to be answered
    int level;                      // current level
    ArrayList<List<Integer>> task;  // all tasks to be completed in this level
    int[] hrRange;                  // hour offsets for highest levels
    int[] minRange;                 // minute offsets for highest levels
    MediaPlayer mediaPlayer;
    boolean finished = false;

    // needed for the main animation process
    float hrCorrect = 0;
    float minCorrect = 0;
    float hrRefX = 0;
    float hrRefY = 0;
    float minRefX = 0;
    float minRefY = 0;
    int hrDirection = 0;
    int minDirection = 0;
    int hrRotSteps = 1;
    int minRotSteps = 1;

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
        setContentView(R.layout.activity_game);

        hrArrow = findViewById(R.id.blueArrow);
        minArrow = findViewById(R.id.redArrow);
        exit = findViewById(R.id.exit);
        hint = findViewById(R.id.hint);
        assess = findViewById(R.id.assess);
        star = findViewById(R.id.star);
        congrats = findViewById(R.id.congrats);
        progressBar = findViewById(R.id.progressBar);
        hand = findViewById(R.id.hand);
        x = findViewById(R.id.x);

        x.setVisibility(View.VISIBLE);
        x.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finished = true;
                Intent intent = new Intent(Instructions.this,Game.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                if (mediaPlayer != null) mediaPlayer.release();
                finish();
            }
        });

        final RelativeLayout rl = findViewById(R.id.rl);
        dim = new ImageView(this);
        dim.setImageResource(R.drawable.dim);
        LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        dim.setLayoutParams(lp);
        rl.addView(dim);

        zoomAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_animation);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // calculate the clock center coordinates
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        centerY = displayMetrics.heightPixels*0.5f;
        centerX = displayMetrics.widthPixels*0.5f;

        // initialize task
        level = getIntent().getIntExtra("startLevel", 0);
        if (level == 1) {
            minArrow.post(new Runnable() {
                @Override
                public void run() {
                    minRotation = 180;
                    TranslateView(minArrow, minRotation);
                }
            });
        }
        taskNr = 0;
        task = new ArrayList<>();
        taskGenerator();
        nextTask();
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

    private Runnable moveArrows = new Runnable() {
        @Override
        public void run() {
            hrRotation += 30*hrDirection;
            // normalize rotation if necessary
            if (hrRotation < 0) hrRotation +=360;
            if (hrRotation > 360) hrRotation -=360;

            TranslateView(hrArrow, hrRotation);

            if (hrRotation % 360 != hrCorrect) {
                (new Handler()).postDelayed(this,1000 / (hrRotSteps + 1));
            } else {
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (level < 2) {
                            (new Handler()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Congratulate();
                                }
                            }, 1000);
                            return;
                        }
                        hrDirection = 0;
                        minRotation += 30*minDirection;
                        // normalize rotation if necessary
                        if (minRotation < 0) minRotation +=360;
                        if (minRotation > 360) minRotation -=360;

                        TranslateView(minArrow, minRotation);
                        if (minRotation % 360 != minCorrect) {
                            (new Handler()).postDelayed(this,1000 / (minRotSteps + 1));
                        } else {
                            minDirection = 0;
                            (new Handler()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Congratulate();
                                }
                            }, 1000);
                        }
                    }
                },200);
            }
        }
    };

    void animateHand(final float hrXDelta, final float hrYDelta, final float minXDelta, final float minYDelta) {
        // skip hrArrow if it is already at the correct position
        final boolean skip = (hrRefX == hrXDelta && hrRefY == hrYDelta);

        // hand appears on screen
        if (skip) {
            hand.setTranslationX(0);
            hand.setTranslationY(-75);
            animation = new TranslateAnimation(hand.getHeight(), minRefX, hand.getHeight(), minRefY);
            animation.setDuration(1000);
        } else {
            hand.setTranslationX(120);
            hand.setTranslationY(75);
            animation = new TranslateAnimation(hand.getHeight(), hrRefX, hand.getHeight(), hrRefY);
            animation.setDuration(800);
        }
        hand.startAnimation(animation);
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (skip) {
                    // hand moves minArrow
                    animation = new TranslateAnimation(minRefX, minXDelta, minRefY, minYDelta);
                    animation.setDuration(1000);
                    hand.startAnimation(animation);
                    minRefX = minXDelta;
                    minRefY = minYDelta;
                } else {
                    // hand moves hrArrow
                    animation = new TranslateAnimation(hrRefX, hrXDelta, hrRefY, hrYDelta);
                    animation.setDuration(1000);
                    hand.startAnimation(animation);
                    hrRefX = hrXDelta;
                    hrRefY = hrYDelta;
                }


                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (level < 2 || minRotation == minCorrect) {
                            pressButton(hrRefX, hrRefY);
                            return;
                        }

                        if (skip) {
                            pressButton(minRefX, minRefY);
                            return;
                        }

                        // hand moves minArrow
                        hand.setTranslationX(0);
                        hand.setTranslationY(-75);
                        animation = new TranslateAnimation(minRefX, minXDelta, minRefY, minYDelta);
                        animation.setDuration(1000);
                        hand.startAnimation(animation);
                        minRefX = minXDelta;
                        minRefY = minYDelta;

                        (new Handler()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                pressButton(minRefX, minRefY);
                            }
                        }, 1000);
                    }
                }, 1000);
            }
        }, 800);
    }

    void pressButton(float fromX, float fromY) {
        hand.setTranslationX(centerX - 70);
        hand.setTranslationY(80);
        animation = new TranslateAnimation(fromX, 0, fromY, 0);
        animation.setDuration(1000);
        animation.setFillAfter(true);
        hand.startAnimation(animation);
        hand.setVisibility(View.VISIBLE);
    }

    void animate() {
        // calculate movement of arrows
        int hourTo12 = task.get(taskNr).get(0);
        if (hourTo12 > 12) hourTo12 = task.get(taskNr).get(0) - 12;
        hrCorrect = (hourTo12 % 12) * 30;
        hrRotSteps = (int) Math.abs(hrCorrect - (hrRotation % 360)) / 30;
        if (hrCorrect < hrRotation % 360) hrDirection = -1;
        else if (hrCorrect > hrRotation % 360) hrDirection = 1;

        // assure that always the shorter path is taken
        if (hrRotSteps > 6) {
            hrRotSteps = 12 - hrRotSteps;
            hrDirection = -hrDirection;
        }

        minCorrect = task.get(taskNr).get(1) * 6;
        minRotSteps = (int) Math.abs(minCorrect - (minRotation % 360)) / 30;
        if (minCorrect < minRotation % 360) minDirection = -1;
        else if (minCorrect > minRotation % 360) minDirection = 1;

        // assure that always the shorter path is taken
        if (minRotSteps > 6) {
            minRotSteps = 12 - minRotSteps;
            minDirection = -minDirection;
        }

        final double DEGREES_TO_RADS = (java.lang.Math.PI*2) / 360;

        // calculate hand translation for hrArrow
        float factorX = (float) (Math.cos((hrCorrect-90) * DEGREES_TO_RADS)-1);
        float factorY = (float) Math.sin((hrCorrect-90) * DEGREES_TO_RADS);
        final float hrXDelta = 100*factorX;
        final float hrYDelta = 100*factorY;

        // calculate hand translation for minArrow
        factorX = (float) Math.sin(minCorrect * DEGREES_TO_RADS);
        factorY = (float) (1 - Math.cos(minCorrect * DEGREES_TO_RADS));
        final float minXDelta = 140*factorX;
        final float minYDelta = 140*factorY;

       animateHand(hrXDelta, hrYDelta, minXDelta, minYDelta);

        int delay = 0;
        if (hrRotSteps > 0) delay = hrRotSteps + 1;
        else delay = minRotSteps + 1;

        (new Handler()).postDelayed(moveArrows, 1000 + (1000 / delay));
    }

    void taskGenerator() {
        if (level == 0 || level == 9) {
            task.add(Arrays.asList(2,0));
            task.add(Arrays.asList(12,0));
            task.add(Arrays.asList(7,0));
        }
        if (level == 1) {
            task.add(Arrays.asList(6,30));
            task.add(Arrays.asList(9,30));
            task.add(Arrays.asList(1,30));
        }
        if (level == 2 || level == 3) {
            task.add(Arrays.asList(5,15));
            task.add(Arrays.asList(10,30));
            task.add(Arrays.asList(3,45));
        }
        if (level == 4 || level == 10) {
            task.add(Arrays.asList(8,5));
            task.add(Arrays.asList(11,25));
            task.add(Arrays.asList(1,40));
        }
        if (level == 5 || level == 11) {
            task.add(Arrays.asList(13,0));
            task.add(Arrays.asList(21,0));
            task.add(Arrays.asList(0,0));
        }
        if (level == 6) {
            task.add(Arrays.asList(16,15));
            task.add(Arrays.asList(7,15));
            task.add(Arrays.asList(23,45));
        }
        if (level == 7) {
            task.add(Arrays.asList(18,5));
            task.add(Arrays.asList(17,55));
            task.add(Arrays.asList(20,10));
        }
        if (level == 8 || level == 12) {
            task.add(Arrays.asList(22,35));
            task.add(Arrays.asList(14,20));
            task.add(Arrays.asList(12,5));
        }
        if (level == 9) {
            hrRange = new int[]{4, -3, -5};
        }
        if (level == 10) {
            minRange = new int[]{30, 5, -20};
        }
        if (level == 11) {
            hrRange = new int[]{-1, 3, -5};
        }
        if (level == 12) {
            minRange = new int[]{15, -10, 50};
        }
    }

    @SuppressLint("SetTextI18n")
    void nextTask() {
        // Stop looping if user closed the Activity
        if (finished) return;

        int hourTo12 = task.get(taskNr).get(0);
        if (hourTo12 > 12) hourTo12 = task.get(taskNr).get(0) - 12;
        if ((hrRotation / 30) % 12 == hourTo12 % 12) {
            if ((minRotation / 6) % 60 == task.get(taskNr).get(1)) taskNr++;
        }

        final Resources res = getResources();
        final int esIst = res.getIdentifier("es_ist", "raw", getPackageName());

        if (level == 3) {
            final int hrNumber;
            final int minNumber = task.get(taskNr).get(1);
            if (minNumber == 15) hrNumber = task.get(taskNr).get(0);
            else hrNumber = task.get(taskNr).get(0) + 1;

            mediaPlayer = MediaPlayer.create(Instructions.this, esIst);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer player) {
                    player.start();
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer.release();

                    int stepSound;
                    if (minNumber == 15 || minNumber == 45)
                        stepSound = res.getIdentifier("viertel", "raw", getPackageName());
                    else stepSound = res.getIdentifier("halb", "raw", getPackageName());

                    mediaPlayer = MediaPlayer.create(Instructions.this, stepSound);
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        public void onPrepared(MediaPlayer player) {
                            player.start();
                        }
                    });
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            mediaPlayer.release();

                            if (minNumber == 15) mediaPlayer = MediaPlayer.create(Instructions.this, R.raw.nach);
                            else if (minNumber == 45) mediaPlayer = MediaPlayer.create(Instructions.this, R.raw.vor);
                            else {
                                System.out.println(hrNumber);
                                int hrSound = res.getIdentifier("u"+hrNumber, "raw", getPackageName());
                                mediaPlayer = MediaPlayer.create(Instructions.this, hrSound);
                            }
                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                public void onPrepared(MediaPlayer player) {
                                    player.start();
                                }
                            });
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mediaPlayer) {
                                    mediaPlayer.release();

                                    if (minNumber != 30) {
                                        int hrSound = res.getIdentifier("u"+hrNumber, "raw", getPackageName());
                                        mediaPlayer = MediaPlayer.create(Instructions.this, hrSound);
                                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                            public void onPrepared(MediaPlayer player) {
                                                player.start();
                                            }
                                        });
                                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                            @Override
                                            public void onCompletion(MediaPlayer mediaPlayer) {
                                                mediaPlayer.release();
                                                animate();
                                            }
                                        });
                                    } else animate();
                                }
                            });
                        }
                    });
                }
            });
            return;
        }

        int hrAsk = task.get(taskNr).get(0);
        if (level == 9 || level == 11) {
            hrAsk += hrRange[taskNr];
            if (hrAsk <= 0) hrAsk += 12;
        }
        final int minAsk;
        if (level == 10 || level == 12) {
            minAsk = task.get(taskNr).get(1) + minRange[taskNr];
        } else {
            minAsk = task.get(taskNr).get(1);
        }

        final boolean hrOnly = minAsk == 0;

        if (level > 8) setClock(hrAsk, minAsk);

        final int hrSound = res.getIdentifier("u"+hrAsk, "raw", getPackageName());
        mediaPlayer = MediaPlayer.create(Instructions.this, esIst);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer player) {
                player.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();
                mediaPlayer = MediaPlayer.create(Instructions.this, hrSound);
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer player) {
                        player.start();
                    }
                });
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        mediaPlayer.release();
                        if (hrOnly) {
                            if (level == 9 || level == 11) {
                                if (hrRange[taskNr] > 0) mediaPlayer = MediaPlayer.create(Instructions.this, R.raw.wie_viel_vor);
                                else mediaPlayer = MediaPlayer.create(Instructions.this, R.raw.wie_viel_in);
                                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                    public void onPrepared(MediaPlayer player) {
                                        player.start();
                                    }
                                });
                                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mediaPlayer) {
                                        mediaPlayer.release();
                                        final int hrRangeSound = res.getIdentifier("s"+Math.abs(hrRange[taskNr]), "raw", getPackageName());
                                        mediaPlayer = MediaPlayer.create(Instructions.this, hrRangeSound);
                                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                            public void onPrepared(MediaPlayer player) {
                                                player.start();
                                            }
                                        });
                                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                            @Override
                                            public void onCompletion(MediaPlayer mediaPlayer) {
                                                mediaPlayer.release();
                                                animate();
                                            }
                                        });
                                    }
                                });
                            } else animate();
                        } else {
                            final int andSound = res.getIdentifier("und", "raw", getPackageName());
                            final int minSound = res.getIdentifier("m"+minAsk, "raw", getPackageName());
                            mediaPlayer = MediaPlayer.create(Instructions.this, andSound);
                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                public void onPrepared(MediaPlayer player) {
                                    player.start();
                                }
                            });
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mediaPlayer) {
                                    mediaPlayer.release();
                                    mediaPlayer = MediaPlayer.create(Instructions.this, minSound);
                                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                        public void onPrepared(MediaPlayer player) {
                                            player.start();
                                        }
                                    });
                                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                        @Override
                                        public void onCompletion(MediaPlayer mediaPlayer) {
                                            mediaPlayer.release();
                                            if (level == 10 || level == 12) {
                                                if (minRange[taskNr] > 0) mediaPlayer = MediaPlayer.create(Instructions.this, R.raw.wie_viel_vor);
                                                else mediaPlayer = MediaPlayer.create(Instructions.this, R.raw.wie_viel_in);
                                                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                                    public void onPrepared(MediaPlayer player) {
                                                        player.start();
                                                    }
                                                });
                                                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                                    @Override
                                                    public void onCompletion(MediaPlayer mediaPlayer) {
                                                        mediaPlayer.release();
                                                        final int minRangeSound = res.getIdentifier("m"+Math.abs(minRange[taskNr]), "raw", getPackageName());
                                                        mediaPlayer = MediaPlayer.create(Instructions.this, minRangeSound);
                                                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                                            public void onPrepared(MediaPlayer player) {
                                                                player.start();
                                                            }
                                                        });
                                                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                                            @Override
                                                            public void onCompletion(MediaPlayer mediaPlayer) {
                                                                mediaPlayer.release();
                                                                animate();
                                                            }
                                                        });
                                                    }
                                                });
                                            } else animate();
                                        }
                                    });
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    void completeTask() {
        // move arrow back to its previous position
        if (level < 9) TranslateView(hrArrow, hrRotation);

        if (taskNr < 2) {
            taskNr++;
            nextTask();
        } else if (level == 0) {
            Intent intent = new Intent(Instructions.this,Game.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        } else {
            Intent intent = new Intent();
            setResult(1);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }

    void TranslateView(View view, float rotation) {
        final double DEGREES_TO_RADS = (java.lang.Math.PI*2) / 360;
        view.setTranslationX((float) (view.getWidth()*(java.lang.Math.sin(rotation * DEGREES_TO_RADS)/2.0f)));
        view.setTranslationY((float) (view.getHeight()*(0.5-java.lang.Math.cos(rotation * DEGREES_TO_RADS)/2.0f)));

        view.setRotation(rotation);
    }

    void setClock(float hr, float min) {
        if (hr > 12) hr -= 12;
        hrRotation = (hr % 12) * 30;
        minRotation = min * 6;

        hrArrow.post(new Runnable() {
            @Override
            public void run() {
                TranslateView(hrArrow, hrRotation);
            }
        });
        minArrow.post(new Runnable() {
            @Override
            public void run() {
                TranslateView(minArrow, minRotation);
            }
        });

        final double DEGREES_TO_RADS = (java.lang.Math.PI*2) / 360;

        // fit hrRefX and hrRefY to new hrArrow position
        float factorX = (float) (Math.cos((hrRotation-90) * DEGREES_TO_RADS)-1);
        float factorY = (float) Math.sin((hrRotation-90) * DEGREES_TO_RADS);
        hrRefX = 100*factorX;
        hrRefY = 100*factorY;

        // fit minRefX and minRefY to new minArrow position
        factorX = (float) Math.sin(minRotation * DEGREES_TO_RADS);
        factorY = (float) (1 - Math.cos(minRotation * DEGREES_TO_RADS));
        minRefX = 140*factorX;
        minRefY = 140*factorY;
    }

    void Congratulate() {
        assess.setColorFilter(Color.rgb(200, 200, 200), android.graphics.PorterDuff.Mode.MULTIPLY);

        // shortly display correct answer with exact hrArrow position w.r.t. minutes
        TranslateView(hrArrow, hrCorrect + (task.get(taskNr).get(1) / 2));

        // update progress bar
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", (taskNr+1)*100/3);
        animation.setDuration(800);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());
        animation.start();

        congrats.startAnimation(zoomAnim);
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                hand.clearAnimation();
                hand.setVisibility(View.INVISIBLE);
                assess.clearColorFilter();
                congrats.clearAnimation();
                congrats.setVisibility(View.INVISIBLE);
                completeTask();
            }
        }, 1300);
    }
}