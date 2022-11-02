package com.te.projecttranslate;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import te.projecttranslate.R;


public class TranslateActivity extends AppCompatActivity {


    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private TextView mSourceLang;
    private EditText mSourceText;
    private Button mTranslateBtn;
    private Button mSpeakBtn;
    private Button mClipBtn;
    private Button mTtsBtn;
    private TextView mTranslatedText;
    private String sourceText;
    private Spinner mLanguageSelector;
    private String targetCode;
    private List<String> language;
    private String[] langOptions;
    private String languagePref;
    private String[] ttsLangOptions;
    private TextToSpeech tts;
    private boolean currentNetworkStatus;
    private MenuItem item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);
        targetCode = TranslateLanguage.ENGLISH;
        mSourceLang = findViewById(R.id.sourceLang);
        mSourceText = findViewById(R.id.sourceText);
        mTranslateBtn = findViewById(R.id.translate);
        mTranslatedText = findViewById(R.id.translatedText);
        mLanguageSelector = findViewById(R.id.langSelector);
        //language = new ArrayList<>(TranslateLanguage.getAllLanguages().size());
        language = TranslateLanguage.getAllLanguages();
        ActionBar actionBar = getSupportActionBar();

        currentNetworkStatus = isNetworkAvailable();

        Log.d("Language", "Initial all languages - " + language);
        //final TextToSpeech tts = new TextToSpeech(this, this, "android.speech.tts");
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int ttsLang = tts.setLanguage(Locale.getDefault());
                    Log.d("Languages TTS" , "TTs initial available languages: " + tts.getAvailableLanguages().toString());

                    if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                            || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("Language TTS", "The Language is not supported!");
                    } else {
                        Log.i("Language TTS", "Language Supported.");
                    }
                    Log.i("Language TTS", "Initialization success. Language = " + Locale.getDefault());
                } else {
                    Toast.makeText(getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        langOptions = new String[]
                        {"Arabic","Bengali","Bulgarian", "Burmese", "Catalan", "Chinese (Cantonese)", "Chinese (Mandarin)", "Croatian", "Czech", "Danish",
                        "Dutch", "English (USA)", "English (UK)", "Estonian", "Filipino", "Finnish", "French", "Georgian", "German", "Greek", "Gujarati",
                        "Hebrew", "Hindi", "Hungarian", "Icelandic", "Indonesia", "Italian", "Japanese", "Kannada", "Khmer", "Korean", "Latvian", "Lithuanian",
                        "Macedonian", "Malay", "Malayalam", "Marathi", "Mongolian", "Nepali", "Persian", "Polish", "Portuguese (brazil)", "Portuguese (Portugal)",
                        "Punjabi", "Romanian" , "Russian", "Serbian", "Sinhala", "Slovak", "Slovenian", "Spanish", "Swahili", "Sweden", "Tamil", "Telugu", "Thai",
                        "Turkish", "Ukrainian", "Urdu", "Uzbek", "Vietnamese", "Zulu"};

        ttsLangOptions = new String[]
                            {"en", "De","fr","zh","zh-Latn","ko","ja","it"};

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("Value")) {
                String st = extras.getString("Value");
                mSourceText.setText(st);
            }
        }
        //mSourcetext.setText(getIntent().getExtras().getString("Value"));

        mSourceText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(currentNetworkStatus != isNetworkAvailable()) {
                    currentNetworkStatus = isNetworkAvailable();
                    setSpinnerLanguages(currentNetworkStatus);
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(currentNetworkStatus != isNetworkAvailable()) {
                    currentNetworkStatus = isNetworkAvailable();
                    setSpinnerLanguages(currentNetworkStatus);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(currentNetworkStatus != isNetworkAvailable()) {
                    currentNetworkStatus = isNetworkAvailable();
                    setSpinnerLanguages(currentNetworkStatus);
                }
            }
        });

        mSpeakBtn = findViewById(R.id.speakBtn);
        mSpeakBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isNetworkAvailable()) {
                    chooseListenLanguage();
                } else {
                    languagePref=  "";
                    listen();
                }
            }
        });

        mTtsBtn = findViewById(R.id.ttsBtn);
        mTtsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d("Language TTS", "Initialization -> Language = " + tts.getLanguage());
                int speechStatus = tts.speak(mTranslatedText.getText().toString(), TextToSpeech.QUEUE_ADD, null);
                if (speechStatus == TextToSpeech.ERROR) {
                    Log.e("Language TTS", "Error in converting Text to Speech!");
                }
            }
        });


        mClipBtn = findViewById(R.id.clipBtn);
        mClipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setClipboard(TranslateActivity.this, mTranslatedText.getText().toString());
            }
        });

        // Log.d("Language ", "Final spinner Models: " + language.toString());
        final ArrayAdapter<String> aa = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, language);

        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mLanguageSelector.setAdapter(aa);
        mLanguageSelector.setSelection(12);
        mLanguageSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                targetCode = TranslateLanguage.fromLanguageTag(aa.getItem(i));
                Log.d("Language Identify", "Spinner Language: " + aa.getItem(i));
                if(!ArrayUtils.contains( ttsLangOptions, targetCode)) {
                    mTtsBtn.setEnabled(false);
                } else {
                    mTtsBtn.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });






        mTranslateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                identifyLanguage();
            }
        });

        setSpinnerLanguages(currentNetworkStatus);
    }

    //actionbar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate menu
        getMenuInflater().inflate(R.menu.menu_translate,menu);
        item = menu.findItem(R.id.offlineIcon);
            item.setVisible(!currentNetworkStatus);

        return true;
    }
    //handle actionbar click items
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.offlineIcon) {
            Toast.makeText(this, "No Network Connection, Only downloaded models available for translation", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void listen() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languagePref);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hi, Welcome to DocsCat Speak something");
        Log.d("Language local speech", " Local Speech - " + Locale.getAvailableLocales());

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            
        }
    }

    private void chooseListenLanguage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(TranslateActivity.this);
        builder.setTitle("Choose Language")
        .setItems(langOptions, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                switch(langOptions[i]) {
                    case "Arabic":
                        languagePref = "ar-QA";
                        break;

                    case "Bengali":
                        languagePref = "bn-IN";
                        break;

                    case "Bulgarian":
                        languagePref = "bg-BG";
                        break;

                    case "Burmese":
                        languagePref = "my-MM";
                        break;

                    case "Catalan":
                        languagePref = "ca-ES";
                        break;

                    case "Chinese (Cantonese)":
                        languagePref = "yue-Hant-HK";
                        break;

                    case "Chinese (Mandarin)":
                        languagePref = "cmn-Hans-CN";
                        break;

                    case "Croatian":
                        languagePref = "hr-HR";
                        break;

                    case "Czech":
                        languagePref = "cs-CZ";
                        break;

                    case "Danish":
                        languagePref = "da-DK";
                        break;

                    case "Dutch":
                        languagePref = "nl-BE";
                        break;

                    case "English (USA)":
                        languagePref = "en-US";
                        break;

                    case "English (UK)":
                        languagePref = "en-GB";
                        break;

                    case "Estonian":
                        languagePref = "et-EE";
                        break;

                    case "Filipino":
                        languagePref = "fil-PH";
                        break;

                    case "Finnish":
                        languagePref = "fi-FI";
                        break;

                    case "French":
                        languagePref = "fr-FR";
                        break;

                    case "Georgian":
                        languagePref = "ka-GE";
                        break;

                    case "German":
                        languagePref = "de-DE";
                        break;

                    case "Greek":
                        languagePref = "el-GR";
                        break;

                    case "Gujarati":
                        languagePref = "gu-IN";
                        break;

                    case "Hebrew":
                        languagePref = "iw-IL";
                        break;

                    case "Hindi":
                        languagePref = "hi-IN";
                        break;

                    case "Hungarian":
                        languagePref = "hu-HU";
                        break;

                    case "Icelandic":
                        languagePref = "is-IS";
                        break;

                    case "Indonesia":
                        languagePref = "id-ID";
                        break;

                    case "Italian":
                        languagePref = "it-IT";
                        break;

                    case "Japanese":
                        languagePref = "ja-JP";
                        break;

                    case "Kannada":
                        languagePref = "kn-IN";
                        break;

                    case "Khmer":
                        languagePref = "km-KH";
                        break;

                    case "Korean":
                        languagePref = "ko-KR";
                        break;

                    case "Latvian":
                        languagePref = "lv-LV";
                        break;

                    case "Lithuanian":
                        languagePref = "lt-LT";
                        break;

                    case "Macedonian":
                        languagePref = "mk-MK";
                        break;

                    case "Malay":
                        languagePref = "ms-MY";
                        break;

                    case "Malayalam":
                        languagePref = "ml-IN";
                        break;

                    case "Marathi":
                        languagePref = "mr-IN";
                        break;

                    case "Mongolian":
                        languagePref = "mn-MN";
                        break;

                    case "Nepali":
                        languagePref = "ne-NP";
                        break;

                    case "Persian":
                        languagePref = "fa-IR";
                        break;

                    case "Polish":
                        languagePref = "pl-PL";
                        break;

                    case "Portuguese (brazil)":
                        languagePref = "pt-BR";
                        break;

                    case "Portuguese (Portugal)":
                        languagePref = "pt-PT";
                        break;

                    case "Punjabi":
                        languagePref = "pa-Guru-IN";
                        break;
                    case "Romanian":
                        languagePref = "ro-RO";
                        break;
                    case "Russian":
                        languagePref = "ru-RU";
                        break;
                    case "Serbian":
                        languagePref = "sr-RS";
                        break;
                    case "Sinhala":
                        languagePref = "si-LK";
                        break;
                    case "Slovak":
                        languagePref = "sk-SK";
                        break;
                    case "Slovenian":
                        languagePref = "sl-SI";
                        break;
                    case "Spanish":
                        languagePref = "es-ES";
                        break;
                    case "Swahili":
                        languagePref = "sw-KE";
                        break;
                    case "Sweden":
                        languagePref = "sv-SE";
                        break;
                    case "Tamil":
                        languagePref = "ta-IN";
                        break;
                    case "Telugu":
                        languagePref = "te-IN";
                        break;
                    case "Thai":
                        languagePref = "th-TH";
                        break;

                    case "Turkish":
                        languagePref = "tr-TR";
                        break;
                    case "Ukrainian":
                        languagePref = "uk-UA";
                        break;
                    case "Urdu":
                        languagePref = "ur-IN";
                        break;
                    case "Uzbek":
                        languagePref = "uz-UZ";
                        break;
                    case "Vietnamese":
                        languagePref = "vi-VN";
                        break;
                    case "Zulu":
                        languagePref = "zu-ZA";
                        break;

                    default:
                        languagePref = "";
                        break;

                }
                listen();
            }
        }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_CODE_SPEECH_INPUT:
                if(resultCode == RESULT_OK && null!=data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    mSourceText.setText(result.get(0));
                }
                break;
        }
    }

    private void identifyLanguage() {
        sourceText = mSourceText.getText().toString();

        if(sourceText.isEmpty()) {
            Toast.makeText(getApplicationContext(),"Please enter text to be translated", Toast.LENGTH_SHORT).show();
        } else {

            LanguageIdentifier languageIdentifier =
                    LanguageIdentification.getClient();
            languageIdentifier.identifyLanguage(sourceText)
                    .addOnSuccessListener(
                            new OnSuccessListener<String>() {
                                @Override
                                public void onSuccess(@Nullable String languageCode) {
                                    if (languageCode.equals("und")) {
                                        Log.d("Language Identify", "Can't identify language.");
                                        Toast.makeText(getApplicationContext(), "Language Not Identified", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.d("Language Identify", "Language: " + languageCode);

                                        translateText(languageCode);


                                    }
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Model couldn’t be loaded or other internal error.
                                    // ...
                                }
                            });
        }
    }

    private void translateText(String langCode) {
        mTranslatedText.setText("Translating..");

        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(langCode)
                        .setTargetLanguage(targetCode)
                        .build();


        final Translator translator =
                Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder()
                .build();
        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void v) {
                                // Model downloaded successfully. Okay to start translating.
                                // (Set a flag, unhide the translation UI, etc.)
                                translator.translate(sourceText)
                                        .addOnSuccessListener(
                                                new OnSuccessListener<String>() {
                                                    @Override
                                                    public void onSuccess(@NonNull String s) {
                                                        // Translation successful.
                                                        Log.d("Language Translation", "Translation: " + s);
                                                        mTranslatedText.setText(s);
                                                        setTtsLanguage();
                                                    }
                                                })
                                        .addOnFailureListener(
                                                new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Error.
                                                        // ...

                                                    }
                                                });

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Model couldn’t be downloaded or other internal error.
                                // ...
                                Log.d("Language Translation", "Model not found ");
                            }
                        });

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void setSpinnerLanguages(boolean i) {
        if(i) {
            Log.d("Language Identify", "Internet access available");
            mSourceLang.setText("All models available, first time translation to a new language will download the model");
            language = TranslateLanguage.getAllLanguages();
            setLanguageModels(true);

        } else {
            Log.d("Language Identify", "No Internet");
            RemoteModelManager modelManager = RemoteModelManager.getInstance();
            // Get translation models stored on the device.

            modelManager.getDownloadedModels(TranslateRemoteModel.class)
                    .addOnSuccessListener(new OnSuccessListener<Set<TranslateRemoteModel>>() {
                        @Override
                        public void onSuccess(Set<TranslateRemoteModel> models) {
                            // ...

                            List<String> finalLanguages = new ArrayList<>(models.size());
                            for(TranslateRemoteModel model: models) {
                                //Log.d("Language Models", "Models: " + model.getLanguage());
                                finalLanguages.add(model.getLanguage());
                            }
                            mSourceLang.setText("Available offline models: " + finalLanguages.toString());
                            Log.d("Language Models", "Available offline Models: " + finalLanguages.toString());
                            language = finalLanguages;
                            setLanguageModels(false);

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Error.
                        }
                    });
        }

        this.invalidateOptionsMenu();

    }

    private void setClipboard(Context context, String text) {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to clipboard: " + text, Toast.LENGTH_SHORT).show();
        }
    }

    private void setLanguageModels(boolean i) {
        // Log.d("Language ", "Final spinner Models: " + language.toString());
        final ArrayAdapter<String> aa = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, language);

        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mLanguageSelector.setAdapter(aa);
        mLanguageSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                targetCode = TranslateLanguage.fromLanguageTag(aa.getItem(i));
                Log.d("Language Identify", "Spinner Language: " + aa.getItem(i));
                mTtsBtn.setEnabled(false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if(i) {
            mLanguageSelector.setSelection(12);
        }

    }

    private void setTtsLanguage() {
        switch(targetCode) {
            case "en":
                tts.setLanguage(Locale.ENGLISH);
                mTtsBtn.setEnabled(true);
                break;

            case "de":
                tts.setLanguage(Locale.GERMAN);
                mTtsBtn.setEnabled(true);
                break;

            case "fr":
                tts.setLanguage(Locale.FRENCH);
                mTtsBtn.setEnabled(true);
                break;

            case "it":
                tts.setLanguage(Locale.ITALIAN);
                break;

            default:
                tts.setLanguage(Locale.getDefault());
                mTtsBtn.setEnabled(false);
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
