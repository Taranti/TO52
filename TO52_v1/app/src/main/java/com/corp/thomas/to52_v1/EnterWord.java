package com.corp.thomas.to52_v1;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;


public class EnterWord extends AppCompatActivity {
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private final int CHECK_CODE = 0x1;
    private final int SHORT_DURATION = 1000;

    private Button launchPrompt, readText, loadInPhone, printLocal;
    private MediaPlayer player;
    private Speaker speaker;
    private TextView tv;

    private Calendar calendar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_word);

        launchPrompt = (Button) findViewById(R.id.launchPrompt);
        launchPrompt.setOnClickListener(promptListener);

        readText = (Button) findViewById(R.id.readText);
        readText.setOnClickListener(readListener);

        loadInPhone = (Button) findViewById(R.id.loadInPhone);
        loadInPhone.setOnClickListener(LoadInPhonListener);

        printLocal = (Button) findViewById(R.id.printOffline);
        printLocal.setOnClickListener(printLocalListener);

        tv = (TextView) findViewById(R.id.tv);


        checkTTS();
    }

    private void checkTTS(){
        Intent check = new Intent();
        check.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(check, CHECK_CODE);
    }

    View.OnClickListener promptListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            promptSpeechInput();
        }
    };

    View.OnClickListener readListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            doGetRequest();
        }
    };

    View.OnClickListener LoadInPhonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            doGetRequestForOfflineMode();
        }
    };

    View.OnClickListener printLocalListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            printOfflineMode();
        }
    };

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().getDisplayLanguage());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Vous pouvez parler ...");

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), "Désolé, votre appareil ne supporte pas d'entrée vocale...", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> buffer = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String result = buffer.get(0);
                    tv.setText("");
                    tv.append(result);
                    try {
                        doPostRequest(result);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Pattern ff = Pattern.compile("Final|fantasy");
                    if (ff.matcher(result).find()) {
                        try {
                            preparePlayer("ffx-victory.mp3");
                            player.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            }
            case CHECK_CODE: {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    speaker = new Speaker(this);
                } else {
                    Intent install = new Intent();
                    install.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(install);
                }
            }
            default:
                break;
        }
    }

    public void preparePlayer(String path) throws IOException {
        AssetFileDescriptor afd = getAssets().openFd(path);
        player = new MediaPlayer();
        player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        player.prepare();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        checkTTS();
    }

    @Override
    protected void onStop() {
        if(player != null) {
            if(player.isPlaying()) {
                player.stop();
            }
        }
        speaker.destroy();
        super.onStop();
    }

    private void doGetRequest() {

        Random r = new Random();
        int i1 = r.nextInt(7 - 1) + 1;
        String url ="https://calm-stream-70416.herokuapp.com/api/questions/"+i1;
        RequestQueue ExampleRequestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest ExampleRequest = new JsonObjectRequest(Request.Method.GET, url,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {

                    String question=response.getString("text");
                    tv.setText("");
                    if(!speaker.isSpeaking()) {
                        speaker.speak(question);
                        tv.append(question);
                        speaker.pause(SHORT_DURATION);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                //This code is executed if there is an error.
            }
        });
        ExampleRequestQueue.add(ExampleRequest);

    }

    private void doGetRequestForOfflineMode(){


        final File f = new File(this.getFilesDir(),"offlineText");
        Random r = new Random();
        int i1 = r.nextInt(7 - 1) + 1;
        String url ="https://calm-stream-70416.herokuapp.com/api/questions/"+i1;
        RequestQueue ExampleRequestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest ExampleRequest = new JsonObjectRequest(Request.Method.GET, url,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {

                    String question=response.getString("text");
                    tv.setText("");
                    try {
                        FileWriter fw = new FileWriter(f.getAbsoluteFile());
                        BufferedWriter bw = new BufferedWriter(fw);
                        bw.write("Question: ");
                        bw.write(question);
                        bw.close();
                        tv.append(question);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                //This code is executed if there is an error.
            }
        });
        ExampleRequestQueue.add(ExampleRequest);

    }

    private void printOfflineMode(){

        final File f = new File(this.getFilesDir(),"offlineText");
        StringBuffer output = new StringBuffer();
        FileReader fr = null;
        tv.setText("");
        try {
            fr = new FileReader(f.getAbsolutePath());
            BufferedReader br = new BufferedReader(fr);
            String line = "";
            while ((line = br.readLine()) != null){
                output.append(line+"\n");
            }
            String response = output.toString();
            br.close();
            tv.append(response);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doPostRequest(final String answer) throws JSONException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        calendar = Calendar.getInstance();
        final String date = dateFormat.format(calendar.getTime());
        Random r = new Random();
        final int questionID = r.nextInt(7 - 1) + 1;
        final int patientID = r.nextInt(2 - 1) + 1;
        JSONObject postparams = new JSONObject();
        postparams.put("patient", String.valueOf(patientID));
        postparams.put("question", String.valueOf(questionID));
        postparams.put("answer",answer);
        postparams.put("date",date);
        String url ="https://calm-stream-70416.herokuapp.com/api/answers";
        RequestQueue ExampleRequestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest ExampleRequest = new JsonObjectRequest(Request.Method.POST, url,postparams, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {


                    String hello = "hello";


            }


        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                //This code is executed if there is an error.
            }

        }){
        /*Override
        protected Map<String, String> getParams()
        {
            Map<String, String> params = new HashMap<>();
            // the POST parameters:
            params.put("patient", String.valueOf(patientID));
            params.put("question", String.valueOf(questionID));
            params.put("answer",answer);
            params.put("date",date);
            return params;
        }*/
            @Override
            public Map getHeaders()  {
                HashMap headers = new HashMap();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                return headers;
            }
        };
        ExampleRequestQueue.add(ExampleRequest);

    }
}
