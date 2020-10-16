package com.example.dieuhrzeit;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Game extends AppCompatActivity {
    ImageView hrArrow, minArrow, exit, hint, sound, assess, star, congrats, hand;
    ProgressBar progressBar;
    TextView question, points;
    Animation zoomAnim, bounceAnim;
    TranslateAnimation handAnim;
    float refX, refY;               // coordinates of last touch event
    float minRotation = 0;          // what angle should the source image be rotated at
    float hrRotation = 90;          // what angle should the source image be rotated at
    float centerX, centerY;         // the actual center coordinates of the clock
    int taskNr, hrAsk, minAsk;      // current task to be answered
    int level;                      // current level
    int correctAnswers = 0;         // current level score
    int totalPoints = 0;            // total game score
    boolean usedHint = false;       // whether hints were used for current task
    ArrayList<List<Integer>> task;  // all tasks to be completed in this level
    int[] hrRange;                  // hour offsets for highest levels
    int[] minRange;                 // minute offsets for highest levels
    boolean blockTouch = true;      // whether to block current touch events
    MediaPlayer mediaPlayer;

    // needed for the hint animation process
    float hrCorrect = 0;
    float minCorrect = 0;
    float minRotHint = 0;
    float hrRotHint = 0;
    float hintRefX = 0;
    float hintRefY = 0;
    int direction = 0;
    int rotSteps = 1;

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
        setContentView(R.layout.activity_game);

        hrArrow = findViewById(R.id.blueArrow);
        minArrow = findViewById(R.id.redArrow);
        exit = findViewById(R.id.exit);
        hint = findViewById(R.id.hint);
        sound = findViewById(R.id.sound);
        assess = findViewById(R.id.assess);
        question = findViewById(R.id.task);
        points = findViewById(R.id.points);
        star = findViewById(R.id.star);
        congrats = findViewById(R.id.congrats);
        progressBar = findViewById(R.id.progressBar);
        hand = findViewById(R.id.hand);

        star.setColorFilter(Color.rgb(180, 180, 180));
        star.setVisibility(View.VISIBLE);

        zoomAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_animation);
        bounceAnim = AnimationUtils.loadAnimation(this, R.anim.bounce_animation);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // calculate the clock center coordinates
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        centerY = (float) (displayMetrics.heightPixels*0.5);
        centerX = (float) (displayMetrics.widthPixels*0.5);

        // initialize task
        level = 0;
        taskNr = 0;
        task = new ArrayList<>();
        hrRange = new int[12];
        minRange = new int[12];
        taskGenerator();
        nextTask();

        hrArrow.setOnTouchListener(new MyTouchListener());
        minArrow.setOnTouchListener(new MyTouchListener());
        exit.setOnTouchListener(new MyTouchListener());
        hint.setOnTouchListener(new MyTouchListener());
        sound.setOnTouchListener(new MyTouchListener());
        assess.setOnTouchListener(new MyTouchListener());
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        task = new ArrayList<>();
        hrRange = new int[12];
        minRange = new int[12];
        taskNr = 0;
        correctAnswers = 0;
        progressBar.setProgress(0);
        taskGenerator();
        nextTask();
    }

    void taskGenerator() {
        int min = 0;
        // shuffle a fixed set of hours to avoid duplicate tasks
        int[] hours = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        shuffleArray(hours);

        for (int i=0; i < 8; i++) {
            int hr = hours[i];

            if (level == 1) {
                min = 30;
            }
            if (level == 2 || level == 6) {
                min = 15 * new Random().nextInt(4);
            }
            if (level == 3) {
                min = (15 * new Random().nextInt(3)) + 15; // skip 0 here, because it makes no sense for the purpose of this level
            }
            if (level == 4 || level == 7 || level == 10) {
                min = 5 * new Random().nextInt(12);
            }
            if (level >= 5 && level <= 7 || level == 11) {
                hr = (hours[i] + 12) % 24;
            }
            if (level == 8 || level == 12) {
                if (i % 2 == 1) hr = (hours[i] + 12) % 24; // every other task is in with the 24h-clock
                min = 5 * new Random().nextInt(12);
            }
            if (level == 9 || level == 11) {
                hrRange[i] = new Random().nextInt(24) - 12;
                if (hrRange[i] == 0) hrRange[i] = 12;
            }
            if (level == 10 || level == 12) {
                minRange[i] = 5 * (new Random().nextInt(24) - 12);
                if (min == 0) minRange[i] = Math.abs(minRange[i]);
                if (minRange[i] == 0) minRange[i] = 30;
                if (min + minRange[i] < 0 || min + minRange[i] >= 60) minRange[i] = -minRange[i];
            }
            task.add(Arrays.asList(hr, min));
        }
    }

    void shuffleArray(int[] array) {
        int range = array.length;
        boolean[] isShuffled = new boolean[range];  // store which positions are shuffled

        while(!isArrayShuffled(isShuffled)) {
            int positionSrc = new Random().nextInt(12);
            int positionDst = new Random().nextInt(12);

            int temp = array[positionSrc];
            array[positionSrc] = array[positionDst];
            array[positionDst] = temp;
            isShuffled[positionSrc] = true;
            isShuffled[positionDst] = true;
        }
    }

    boolean isArrayShuffled(boolean[] isShuffled) {
        for (boolean b : isShuffled) if (!b) return false;

        return true;
    }

    void taskSound() {
        final Resources res = getResources();
        final int esIst = res.getIdentifier("es_ist", "raw", getPackageName());
        if (level == 3) {
            mediaPlayer = MediaPlayer.create(Game.this, esIst);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer player) {
                    player.start();
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer.release();

                    int stepSound = 0;
                    if (minAsk == 15 || minAsk == 45)
                        stepSound = res.getIdentifier("viertel", "raw", getPackageName());
                    else stepSound = res.getIdentifier("halb", "raw", getPackageName());

                    mediaPlayer = MediaPlayer.create(Game.this, stepSound);
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        public void onPrepared(MediaPlayer player) {
                            player.start();
                        }
                    });
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            mediaPlayer.release();

                            if (minAsk == 15) mediaPlayer = MediaPlayer.create(Game.this, R.raw.nach);
                            else if (minAsk == 45) mediaPlayer = MediaPlayer.create(Game.this, R.raw.vor);
                            else {
                                System.out.println(hrAsk);
                                int hrSound = res.getIdentifier("u"+hrAsk, "raw", getPackageName());
                                mediaPlayer = MediaPlayer.create(Game.this, hrSound);
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

                                    if (minAsk != 30) {
                                        int hrSound = res.getIdentifier("u"+hrAsk, "raw", getPackageName());
                                        mediaPlayer = MediaPlayer.create(Game.this, hrSound);
                                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                            public void onPrepared(MediaPlayer player) {
                                                player.start();
                                            }
                                        });
                                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                            @Override
                                            public void onCompletion(MediaPlayer mediaPlayer) {
                                                mediaPlayer.release();
                                                sound.clearColorFilter();
                                                // reactivate touch events
                                                blockTouch = false;
                                            }
                                        });
                                    } else {
                                        sound.clearColorFilter();
                                        // reactivate touch events
                                        blockTouch = false;
                                    }
                                }
                            });
                        }
                    });
                }
            });
            return;
        }
        final int hrSound = res.getIdentifier("u"+hrAsk, "raw", getPackageName());
        final boolean hrOnly = minAsk == 0;
        mediaPlayer = MediaPlayer.create(Game.this, esIst);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer player) {
                player.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();
                mediaPlayer = MediaPlayer.create(Game.this, hrSound);
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
                                if (hrRange[taskNr] > 0) mediaPlayer = MediaPlayer.create(Game.this, R.raw.wie_viel_vor);
                                else mediaPlayer = MediaPlayer.create(Game.this, R.raw.wie_viel_in);
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
                                        mediaPlayer = MediaPlayer.create(Game.this, hrRangeSound);
                                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                            public void onPrepared(MediaPlayer player) {
                                                player.start();
                                            }
                                        });
                                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                            @Override
                                            public void onCompletion(MediaPlayer mediaPlayer) {
                                                mediaPlayer.release();
                                                sound.clearColorFilter();

                                                // reactivate touch events
                                                blockTouch = false;
                                            }
                                        });
                                    }
                                });
                            } else {
                                sound.clearColorFilter();
                                // reactivate touch events
                                blockTouch = false;
                            }
                        } else {
                            final int andSound = res.getIdentifier("und", "raw", getPackageName());
                            final int minSound = res.getIdentifier("m"+minAsk, "raw", getPackageName());
                            mediaPlayer = MediaPlayer.create(Game.this, andSound);
                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                public void onPrepared(MediaPlayer player) {
                                    player.start();
                                }
                            });
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mediaPlayer) {
                                    mediaPlayer.release();
                                    mediaPlayer = MediaPlayer.create(Game.this, minSound);
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
                                                if (minRange[taskNr] > 0) mediaPlayer = MediaPlayer.create(Game.this, R.raw.wie_viel_vor);
                                                else mediaPlayer = MediaPlayer.create(Game.this, R.raw.wie_viel_in);
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
                                                        mediaPlayer = MediaPlayer.create(Game.this, minRangeSound);
                                                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                                            public void onPrepared(MediaPlayer player) {
                                                                player.start();
                                                            }
                                                        });
                                                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                                            @Override
                                                            public void onCompletion(MediaPlayer mediaPlayer) {
                                                                mediaPlayer.release();
                                                                sound.clearColorFilter();

                                                                // reactivate touch events
                                                                blockTouch = false;
                                                            }
                                                        });
                                                    }
                                                });
                                            } else {
                                                sound.clearColorFilter();
                                                // reactivate touch events
                                                blockTouch = false;
                                            }
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

    @SuppressLint("SetTextI18n")
    void nextTask() {
        int hourTo12 = task.get(taskNr).get(0);
        if (hourTo12 > 12) hourTo12 = task.get(taskNr).get(0) - 12;
        if ((hrRotation / 30) % 12 == hourTo12 % 12) {
            if ((minRotation / 6) % 60 == task.get(taskNr).get(1)) Collections.swap(task, taskNr, task.size()-1);
        }
        hrAsk = task.get(taskNr).get(0);
        if (level == 9 || level == 11) {
            hrAsk += hrRange[taskNr];
            if (hrAsk <= 0) hrAsk += 12;
        }
        if (level == 10 || level == 12) {
            minAsk = task.get(taskNr).get(1) + minRange[taskNr];
        } else {
            minAsk = task.get(taskNr).get(1);
        }

        if (level == 3 && minAsk > 15) hrAsk++;
        if (level > 8) setClock(hrAsk, minAsk);
        taskSound();
        question.setText(" "+task.get(taskNr).get(0)+" : "+task.get(taskNr).get(1));
    }

    void completeTask() {
        // move arrows back to their previous positions
        if (level < 9) TranslateView(hrArrow, hrRotation);
        if (level < 9) TranslateView(minArrow, minRotation);

        if (taskNr < 4) {
            taskNr++;
            usedHint = false;
            nextTask();
        } else {
            Intent intent = new Intent(Game.this,LevelComplete.class);
            intent.putExtra("levelCompleted", level);
            intent.putExtra("correct", correctAnswers);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            if (correctAnswers < (taskNr+1)*0.8f) {
                finish();
                return;
            }

            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (level < 12) {
                        LevelComplete.getInstance().finish();
                        if (level == 0) {
                            minRotation = 180;
                            TranslateView(minArrow, minRotation);
                        }
                        level++;
                        Intent intent = new Intent(Game.this,Instructions.class);
                        intent.putExtra("startLevel", level);
                        startActivityForResult(intent, 1);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    }
                }
            }, 7000);
        }
    }

    double ComputeAngle(float x, float y) {
        final double RADS_TO_DEGREES = 360 / (java.lang.Math.PI*2);
        double result = java.lang.Math.atan2(y,x) * RADS_TO_DEGREES;

        if (result < 0) {
            result = 360 + result;
        }

        return result;
    }

    void TranslateView(View view, float rotation) {
        final double DEGREES_TO_RADS = (java.lang.Math.PI*2) / 360;
        view.setTranslationX((float) (view.getWidth()*(java.lang.Math.sin(rotation * DEGREES_TO_RADS)/2.0f)));
        view.setTranslationY((float) (view.getHeight()*(0.5-java.lang.Math.cos(rotation * DEGREES_TO_RADS)/2.0f)));

        view.setRotation(rotation);
    }

    void sayNumber(int number, String arrow) {
        final Resources res = getResources();

        // say 12 o'clock instead of 0 o'çlock
        if (number == 0 && arrow == "u") number = 12;
        final int numSound = res.getIdentifier(arrow+number, "raw", getPackageName());
        mediaPlayer = MediaPlayer.create(Game.this, numSound);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer player) {
                player.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();

                // reactivate touch events
                blockTouch = false;
            }
        });
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
    }

    void Congratulate() {
        congrats.startAnimation(zoomAnim);
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                congrats.clearAnimation();
                congrats.setVisibility(View.INVISIBLE);
            }
        }, 1300);
    }

    void hint() {
        final double DEGREES_TO_RADS = (java.lang.Math.PI*2) / 360;

        int hourTo12 = task.get(taskNr).get(0);
        if (hourTo12 > 12) hourTo12 = task.get(taskNr).get(0) - 12;
        hrCorrect = (hourTo12 % 12) * 30;
        minCorrect = task.get(taskNr).get(1) * 6;

        float xDelta = 0;
        float yDelta = 0;

        if (hrRotation % 360 != hrCorrect) {
            // calculate movement of hrArrow
            rotSteps = (int) Math.abs(hrCorrect - (hrRotation % 360)) / 30;
            if (hrCorrect < hrRotation % 360) direction = -1;
            else if (hrCorrect > hrRotation % 360) direction = 1;

            // assure that always the shorter path is taken
            if (rotSteps > 6) {
                rotSteps = 12 - rotSteps;
                direction = -direction;
            }

            // find current position of hrArrow
            float factorX = (float) (Math.cos((hrRotation-90) * DEGREES_TO_RADS)-1);
            float factorY = (float) Math.sin((hrRotation-90) * DEGREES_TO_RADS);
            hintRefX = 100*factorX;
            hintRefY = 100*factorY;

            // calculate hand translation for hrArrow
            factorX = (float) (Math.cos((hrCorrect-90) * DEGREES_TO_RADS)-1);
            factorY = (float) Math.sin((hrCorrect-90) * DEGREES_TO_RADS);
            xDelta = 100*factorX;
            yDelta = 100*factorY;
        } else if (minRotation % 360 != minCorrect) {
            // calculate movement of minArrow
            rotSteps = (int) Math.abs(minCorrect - minRotation % 360) / 30;
            if (minCorrect < minRotation % 360) direction = -1;
            else if (minCorrect > minRotation % 360) direction = 1;

            // assure that always the shorter path is taken
            if (rotSteps > 6) {
                rotSteps = 12 - rotSteps;
                direction = -direction;
            }

            // find current position of minArrow
            float factorX = (float) Math.sin(minRotation * DEGREES_TO_RADS);
            float factorY = (float) (1 - Math.cos(minRotation * DEGREES_TO_RADS));
            hintRefX = 140*factorX;
            hintRefY = 140*factorY;

            // calculate hand translation for minArrow
            factorX = (float) Math.sin(minCorrect * DEGREES_TO_RADS);
            factorY = (float) (1 - Math.cos(minCorrect * DEGREES_TO_RADS));
            xDelta = 140*factorX;
            yDelta = 140*factorY;
        } else {
            // already correct, just press button!
            rotSteps = 1;
        }

        animateHand(xDelta, yDelta);

        hrRotHint = hrRotation;
        minRotHint = minRotation;
        (new Handler()).postDelayed(moveArrows, 1000 + (1000 / (rotSteps + 1)));
    }

    private Runnable moveArrows = new Runnable() {
        @Override
        public void run() {
            if (hrRotHint % 360 != hrCorrect) {
                hrRotHint += 30*direction;
                // normalize rotation if necessary
                if (hrRotHint < 0) hrRotHint +=360;

                TranslateView(hrArrow, hrRotHint);

                if (hrRotHint % 360 != hrCorrect) (new Handler()).postDelayed(this,1000 / (rotSteps + 1));
                else {
                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            hand.clearAnimation();
                            hand.setVisibility(View.INVISIBLE);
                            // move arrow back to its previous position
                            TranslateView(hrArrow, hrRotation);
                            blockTouch = false;
                        }
                    }, 400);
                }
            } else if (minRotHint % 360 != minCorrect) {
                minRotHint += 30*direction;
                // normalize rotation if necessary
                if (minRotHint < 0) minRotHint +=360;

                TranslateView(minArrow, minRotHint);

                if (minRotHint % 360 != minCorrect) (new Handler()).postDelayed(this,1000 / (rotSteps + 1));
                else {
                    (new Handler()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            hand.clearAnimation();
                            hand.setVisibility(View.INVISIBLE);
                            // move arrow back to its previous position
                            TranslateView(minArrow, minRotation);
                            blockTouch = false;
                        }
                    }, 400);
                }
            }
        }
    };

    void animateHand(final float xDelta, final float yDelta) {
        hand.setVisibility(View.VISIBLE);
        if (hrRotation % 360 != hrCorrect) {
            // hand appears on screen
            hand.setTranslationX(120);
            hand.setTranslationY(75);
            handAnim = new TranslateAnimation(hand.getHeight(), hintRefX, hand.getHeight(), hintRefY);
            handAnim.setDuration(800);
            hand.startAnimation(handAnim);

            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    // hand moves hrArrow
                    handAnim = new TranslateAnimation(hintRefX, xDelta, hintRefY, yDelta);
                    handAnim.setDuration(1000);
                    handAnim.setFillAfter(true);
                    hand.startAnimation(handAnim);
                }
            }, 800);
        } else if (minRotation % 360 != minCorrect) {
            // hand appears on screen
            hand.setTranslationX(0);
            hand.setTranslationY(-75);
            handAnim = new TranslateAnimation(hand.getHeight(), hintRefX, hand.getHeight(), hintRefY);
            handAnim.setDuration(800);
            hand.startAnimation(handAnim);

            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    // hand moves minArrow
                    handAnim = new TranslateAnimation(hintRefX, xDelta, hintRefY, yDelta);
                    handAnim.setDuration(1000);
                    handAnim.setFillAfter(true);
                    hand.startAnimation(handAnim);
                }
            }, 800);
        } else {
            // already correct, just press button!
            hand.setTranslationX(centerX - 70);
            hand.setTranslationY(75);
            handAnim = new TranslateAnimation(0, 0, hand.getHeight(), 0);
            handAnim.setDuration(1000);
            handAnim.setFillAfter(true);
            hand.startAnimation(handAnim);
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    hand.clearAnimation();
                    hand.setVisibility(View.INVISIBLE);
                    blockTouch = false;
                }
            }, 1600);
        }
    }

    private final class MyTouchListener implements View.OnTouchListener {
        public boolean onTouch(View view, MotionEvent motionEvent) {
            // block events during assessment
            if (blockTouch) return true;

            // in the first two levels one arrow is given and non-movable
            if (level <= 1 && view == minArrow) return true;

            int actionmasked = motionEvent.getActionMasked();

            if (actionmasked == MotionEvent.ACTION_DOWN) {
                if (view == exit) {
                    exit.setColorFilter(Color.rgb(200, 200, 200), android.graphics.PorterDuff.Mode.MULTIPLY);
                    LayoutInflater inflater = LayoutInflater.from(Game.this);
                    View view1 = inflater.inflate(R.layout.alert_dialog, null);

                    final AlertDialog builder = new AlertDialog.Builder(Game.this).setView(view1).create();
                    builder.setCanceledOnTouchOutside(false);

                    Button yes = view1.findViewById(R.id.yes);
                    Button no = view1.findViewById(R.id.no);

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

                    return true;
                } else if (view == hint)  {
                    usedHint = true;
                    blockTouch = true;
                    hand.setAlpha(0.6f);
                    hint();
                    return true;
                } else if (view == sound)  {
                    sound.setColorFilter(Color.rgb(200, 200, 200), android.graphics.PorterDuff.Mode.MULTIPLY);
                    blockTouch = true;
                    taskSound();
                    return true;
                } else if (view == assess)  {
                    blockTouch = true;
                    assess.setColorFilter(Color.rgb(200, 200, 200), android.graphics.PorterDuff.Mode.MULTIPLY);

                    int hourTo12 = task.get(taskNr).get(0);
                    if (hourTo12 > 12) hourTo12 = task.get(taskNr).get(0) - 12;

                    // shortly display correct answer with exact hrArrow position w.r.t. minutes
                    float hrCorrect = (hourTo12 * 30) + (task.get(taskNr).get(1) / 2);
                    float minCorrect = task.get(taskNr).get(1) * 6;
                    TranslateView(hrArrow, hrCorrect);
                    TranslateView(minArrow, minCorrect);

                    // assess each arrow and give feedback
                    if ((hrRotation / 30) % 12 == hourTo12 % 12) {
                        if ((minRotation / 6) % 60 == task.get(taskNr).get(1)) {
                            // correct answer
                            Resources res = getResources();
                            final int correct_tone = res.getIdentifier("correct_tone", "raw", getPackageName());
                            final int sound = res.getIdentifier("correct"+(taskNr % 6 + 1), "raw", getPackageName());
                            mediaPlayer = MediaPlayer.create(Game.this, correct_tone);
                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                public void onPrepared(MediaPlayer player) {
                                    player.start();
                                }
                            });

                            (new Handler()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Congratulate();
                                    star.startAnimation(bounceAnim);

                                    (new Handler()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (totalPoints == 0) star.clearColorFilter();
                                            points.setVisibility(View.INVISIBLE);
                                        }
                                    },200);

                                    (new Handler()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            totalPoints++;
                                            if (!usedHint) totalPoints += 2;
                                            correctAnswers++;

                                            if (totalPoints > 9) points.setTranslationX((float) Math.floor(Math.log10(totalPoints))*8);
                                            points.setText(""+totalPoints);
                                            points.setVisibility(View.VISIBLE);
                                            mediaPlayer.release();
                                            mediaPlayer = MediaPlayer.create(Game.this, sound);
                                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                                public void onPrepared(MediaPlayer player) {
                                                    player.start();
                                                }
                                            });
                                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                                @Override
                                                public void onCompletion(MediaPlayer mediaPlayer) {
                                                    mediaPlayer.release();
                                                    assess.clearColorFilter();
                                                    completeTask();
                                                }
                                            });
                                        }
                                    },1000);
                                }
                            }, 10);
                        } else {
                            // wrong answer
                            Resources res = getResources();
                            final int sound = res.getIdentifier("wrong"+(taskNr % 4 + 1), "raw", getPackageName());
                            mediaPlayer = MediaPlayer.create(Game.this, sound);
                            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                public void onPrepared(MediaPlayer player) {
                                    player.start();
                                }
                            });
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mediaPlayer) {
                                    mediaPlayer.release();
                                    assess.clearColorFilter();
                                    completeTask();
                                }
                            });
                        }
                    } else {
                        // wrong answer
                        Resources res = getResources();
                        final int sound = res.getIdentifier("wrong"+(taskNr % 4 + 1), "raw", getPackageName());
                        mediaPlayer = MediaPlayer.create(Game.this, sound);
                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            public void onPrepared(MediaPlayer player) {
                                player.start();
                            }
                        });
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                mediaPlayer.release();
                                assess.clearColorFilter();
                                completeTask();
                            }
                        });
                    }

                    // update progress bar
                    ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", (taskNr+1)*20);
                    animation.setDuration(800);
                    animation.setInterpolator(new AccelerateDecelerateInterpolator());
                    animation.start();
                } else {
                    refX = motionEvent.getX();
                    refY = motionEvent.getY();
                }
                return true;
            } else if (actionmasked == MotionEvent.ACTION_MOVE) {
                if (view != hrArrow && view != minArrow) return true;

                // normalize our touch event's X and Y coordinates to be relative to the center coordinate
                float x = motionEvent.getX() - centerX;
                float y =  centerY - motionEvent.getY();

                if ((x != 0) && (y != 0)) {
                    double angleB = ComputeAngle(x, y);

                    x = refX - centerX;
                    y = centerY - refY;
                    double angleA = ComputeAngle(x,y);
                    float rotation = (float)(angleA - angleB);

                    if (view == hrArrow) {
                        hrRotation += rotation;
                        TranslateView(view, hrRotation);
                    } else {
                        minRotation += rotation;
                        TranslateView(view, minRotation);
                    }
                }
                return true;
            } else if (actionmasked == MotionEvent.ACTION_UP) {
                if (view != hrArrow && view != minArrow) return true;

                // consider only the 12 main positions (hour marks) on a clock
                if (view == hrArrow) {
                    hrRotation = Math.round(hrRotation / 30) * 30;
                    TranslateView(view, hrRotation);
                } else {
                    minRotation = Math.round(minRotation / 30) * 30;
                    TranslateView(view, minRotation);
                }

                // normalize rotations if necessary
                while (hrRotation < 0) hrRotation +=360;
                while (minRotation < 0) minRotation +=360;

                blockTouch = true;
                int number = (view == hrArrow) ? (int)(hrRotation / 30) % 12 : (int)(minRotation / 6) % 60;
                sayNumber(number, (view == hrArrow) ? "u" : "m");
                return true;
            } else {
                return false;
            }
        }
    }
}
