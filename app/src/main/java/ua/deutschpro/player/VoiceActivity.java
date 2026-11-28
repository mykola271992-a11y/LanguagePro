package ua.deutschpro.player;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.*;
import org.json.*;
import java.util.*;

public class VoiceActivity extends Activity {
    private int BG,CARD,INK,MUTED,ACCENT;
    private static final int SPEECH=42;
    private Spinner language;
    private EditText source;
    private TextView direction, result, state, speakButton, saveButton;
    private CheckBox autoSpeak;
    private boolean reverse=false, ready=false, recognizing=false, destroyed=false;
    private Translator translator;
    private TextToSpeech tts;
    private String translated="", original="", translatedLang="", inputLang="", speechLang="";
    private int generation=0;
    private final Handler handler=new Handler(Looper.getMainLooper());

    void applyTheme(){ThemePalette t=ThemePalette.current(this);BG=t.bg;CARD=t.card;INK=t.ink;MUTED=t.muted;ACCENT=t.accent;getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);int flags=t==ThemePalette.ALL[1]?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR:0;if(android.os.Build.VERSION.SDK_INT>=26&&t==ThemePalette.ALL[1])flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;getWindow().getDecorView().setSystemUiVisibility(flags);}
    int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    GradientDrawable bg(int color,int r){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(r));return d;}
    LinearLayout col(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);return x;}
    LinearLayout row(){LinearLayout x=new LinearLayout(this);x.setGravity(Gravity.CENTER_VERTICAL);return x;}
    TextView text(String value,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    void add(LinearLayout parent,View child,int margin){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(margin);parent.addView(child,lp);}
    TextView button(String title,boolean primary,Runnable action){TextView v=text(title,16,primary?BG:INK,true);v.setGravity(Gravity.CENTER);v.setPadding(dp(12),dp(15),dp(12),dp(15));v.setBackground(bg(primary?ACCENT:CARD,18));v.setMinHeight(dp(50));v.setOnClickListener(w->action.run());return v;}
    void weighted(LinearLayout parent,View child){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(3),0,dp(3),0);parent.addView(child,lp);}
    String selected(){int i=language.getSelectedItemPosition();return Store.LANGS[Math.max(0,i)];}
    String from(){return reverse?selected():"uk";}
    String to(){return reverse?"uk":selected();}
    String name(String code){return "uk".equals(code)?"Українська":Store.name(code);}
    Locale locale(String code){return "uk".equals(code)?Locale.forLanguageTag("uk-UA"):Store.locale(code);}

    @Override public void onCreate(Bundle b){super.onCreate(b);applyTheme();
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(BG);scroll.setFillViewport(true);setContentView(scroll);
        LinearLayout root=col();root.setPadding(dp(20),dp(18),dp(20),dp(28));scroll.addView(root);
        add(root,button("‹  Назад до плеєра",false,this::finish),0);
        add(root,text("Голосовий переклад",28,INK,true),20);
        add(root,text("Говори, перекладай та слухай",15,MUTED,false),5);
        language=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,Store.NAMES);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);language.setAdapter(a);
        int chosen=0;String pref=b==null?Store.prefs(this).getString("voiceLanguage","de"):b.getString("selected","de");for(int i=0;i<Store.LANGS.length;i++)if(Store.LANGS[i].equals(pref))chosen=i;language.setSelection(chosen);add(root,language,15);
        direction=button("",false,()->{reverse=!reverse;clearResult();direction.setText(name(from())+"  ⇄  "+name(to()));});add(root,direction,10);
        source=new EditText(this);source.setTextColor(INK);source.setHintTextColor(MUTED);source.setHint("Скажи або напиши фразу…");source.setGravity(Gravity.TOP);source.setMinLines(3);source.setMaxLines(7);source.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);add(root,source,15);
        add(root,button("◉  Говорити",true,this::recognize),12);
        add(root,button("Перекласти текст",false,this::translateText),8);
        state=text("Обери мову та напрямок",13,ACCENT,false);add(root,state,12);
        LinearLayout panel=col();panel.setPadding(dp(16),dp(16),dp(16),dp(16));panel.setBackground(bg(CARD,22));add(panel,text("ПЕРЕКЛАД",12,ACCENT,true),0);result=text("Тут з’явиться переклад",23,INK,true);result.setTextIsSelectable(true);add(panel,result,12);add(root,panel,12);
        autoSpeak=new CheckBox(this);autoSpeak.setText("Автоматично озвучувати переклад");autoSpeak.setTextColor(INK);autoSpeak.setChecked(Store.prefs(this).getBoolean("voiceAuto",true));autoSpeak.setOnCheckedChangeListener((v,checked)->Store.prefs(this).edit().putBoolean("voiceAuto",checked).apply());add(root,autoSpeak,8);
        LinearLayout controls=row();speakButton=button("♫ Слухати",false,this::speak);saveButton=button("＋ В уроки",false,this::save);weighted(controls,speakButton);weighted(controls,saveButton);add(root,controls,10);enableResult(false);
        add(root,text("Для першого перекладу потрібен інтернет, щоб завантажити мовні моделі. Розпізнавання голосу залежить від сервісу Android і може потребувати мережі.",13,MUTED,false),16);
        source.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){clearResult();}public void afterTextChanged(Editable s){}});
        language.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){Store.prefs(VoiceActivity.this).edit().putString("voiceLanguage",selected()).apply();String next=name(from())+"  ⇄  "+name(to());if(!next.contentEquals(direction.getText())){clearResult();direction.setText(next);}}public void onNothingSelected(AdapterView<?> p){}});
        direction.setText(name(from())+"  ⇄  "+name(to()));
        tts=new TextToSpeech(this,status->handler.post(()->{if(!destroyed)ready=status==TextToSpeech.SUCCESS;}));
        if(b!=null){reverse=b.getBoolean("reverse",false);source.setText(b.getString("source",""));direction.setText(name(from())+"  ⇄  "+name(to()));speechLang=b.getString("speechLang","");recognizing=b.getBoolean("recognizing",false);}
    }
    void enableResult(boolean yes){if(speakButton!=null){speakButton.setEnabled(yes);speakButton.setAlpha(yes?1:.45f);saveButton.setEnabled(yes);saveButton.setAlpha(yes?1:.45f);}}
    void clearResult(){generation++;if(translator!=null){translator.close();translator=null;}if(tts!=null)tts.stop();translated="";original="";if(result!=null)result.setText("Тут з’явиться переклад");enableResult(false);if(state!=null)state.setText("Готово до перекладу");}
    void recognize(){if(recognizing)return;clearResult();PlayerService.command(this,"PAUSE");speechLang=from();Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,locale(speechLang).toLanguageTag());intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Говори: "+name(speechLang));try{recognizing=true;startActivityForResult(intent,SPEECH);}catch(ActivityNotFoundException e){recognizing=false;state.setText("Немає сервісу розпізнавання голосу. Можна ввести текст.");}catch(SecurityException e){recognizing=false;state.setText("Перевір дозвіл мікрофона в сервісі розпізнавання.");}}
    @Override protected void onActivityResult(int request,int response,Intent data){super.onActivityResult(request,response,data);if(request!=SPEECH)return;recognizing=false;if(response!=RESULT_OK||data==null){state.setText("Запис скасовано");return;}if(!speechLang.equals(from())){state.setText("Мова змінилася. Запиши фразу ще раз.");return;}ArrayList<String> values=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(values==null||values.isEmpty()){state.setText("Не вдалося розпізнати фразу");return;}source.setText(values.get(0));translateText();}
    void translateText(){String value=source.getText().toString().trim();if(value.isEmpty()){source.setError("Введи або продиктуй фразу");return;}clearResult();final int g=generation;final String src=from(),dst=to();state.setText("Підготовка мовної моделі…");final Translator t=Translation.getClient(new TranslatorOptions.Builder().setSourceLanguage(src).setTargetLanguage(dst).build());translator=t;t.downloadModelIfNeeded(new DownloadConditions.Builder().build()).addOnSuccessListener(v->{if(!current(g,t))return;state.setText("Перекладаю…");t.translate(value).addOnSuccessListener(answer->{if(!current(g,t))return;translator=null;t.close();original=value;inputLang=src;translated=answer;translatedLang=dst;result.setText(answer);state.setText("Переклад готовий");enableResult(!answer.trim().isEmpty());if(autoSpeak.isChecked()&&!answer.trim().isEmpty())speak();}).addOnFailureListener(e->error(g,t));}).addOnFailureListener(e->error(g,t));}
    boolean current(int g,Translator t){return !destroyed&&generation==g&&translator==t;}
    void error(int g,Translator t){if(!current(g,t))return;translator=null;t.close();state.setText("Помилка перекладу. Перевір мережу та вільне місце й повтори.");}
    void speak(){if(translated.isEmpty())return;if(!ready||tts==null){state.setText("Голос ще не готовий. Спробуй знову.");return;}PlayerService.command(this,"PAUSE");int lang=tts.setLanguage(locale(translatedLang));if(lang==TextToSpeech.LANG_MISSING_DATA||lang==TextToSpeech.LANG_NOT_SUPPORTED){state.setText("Встанови голос: "+name(translatedLang)+" у налаштуваннях Android");return;}tts.setSpeechRate(Store.prefs(this).getFloat("speed",.85f));if(tts.speak(translated,TextToSpeech.QUEUE_FLUSH,null,"voice-"+generation)==TextToSpeech.ERROR)state.setText("Не вдалося озвучити переклад");}
    void save(){if(translated.isEmpty())return;String lang="uk".equals(inputLang)?translatedLang:inputLang;String uk="uk".equals(inputLang)?original:translated;String foreign="uk".equals(inputLang)?translated:original;try{Store.saveTranslated(this,"Голосова фраза • "+name(lang),lang,new JSONArray().put(new JSONObject().put("uk",uk).put("de",foreign)));state.setText("Збережено в бібліотеці уроків");saveButton.setEnabled(false);saveButton.setAlpha(.45f);}catch(Exception e){state.setText("Не вдалося зберегти фразу");}}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("selected",selected());b.putBoolean("reverse",reverse);b.putString("source",source.getText().toString());b.putString("speechLang",speechLang);b.putBoolean("recognizing",recognizing);}
    @Override protected void onPause(){super.onPause();if(tts!=null)tts.stop();}
    @Override protected void onDestroy(){destroyed=true;clearResult();if(tts!=null)tts.shutdown();handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
