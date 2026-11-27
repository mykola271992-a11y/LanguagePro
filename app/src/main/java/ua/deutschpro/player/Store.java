package ua.deutschpro.player;
import android.content.*; import java.io.*; import java.nio.charset.StandardCharsets; import java.util.*; import org.json.*;
final class Store {
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("deutsch_player",0);}
 static String read(InputStream in)throws IOException {try(InputStream x=in; ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=x.read(b))!=-1){o.write(b,0,n);if(o.size()>2_000_000)throw new IOException("Файл завеликий");}return new String(o.toByteArray(),StandardCharsets.UTF_8).replace("\uFEFF","");}}
 static JSONArray lessons(Context c){try{String s=prefs(c).getString("lessons",null);return new JSONArray(s==null?read(c.getAssets().open("lessons.json")):s);}catch(Exception e){return new JSONArray();}}
 static JSONObject lesson(Context c,int i){JSONArray a=lessons(c);return a.optJSONObject(i);}
 static JSONArray phrases(Context c,int i){JSONObject o=lesson(c,i);JSONArray a=o==null?null:o.optJSONArray("phrases");return a==null?new JSONArray():a;}
 static String title(Context c,int i){JSONObject o=lesson(c,i);return o==null?"Немає уроків":o.optString("title");}
 static String phrase(Context c,int i,int p,String lang){JSONObject o=phrases(c,i).optJSONObject(p);return o==null?"":o.optString(lang);}
 static final String[] LANGS={"de","en","cs","pl","fr","es","it"};
 static final String[] NAMES={"Німецька","Англійська","Чеська","Польська","Французька","Іспанська","Італійська"};
 static String language(Context c,int i){JSONObject o=lesson(c,i);return o==null?"de":o.optString("language","de");}
 static String name(String code){for(int i=0;i<LANGS.length;i++)if(LANGS[i].equals(code))return NAMES[i];return code;}
 static Locale locale(String code){switch(code){case "en":return Locale.UK;case "de":return Locale.GERMANY;case "cs":return Locale.forLanguageTag("cs-CZ");case "pl":return Locale.forLanguageTag("pl-PL");case "fr":return Locale.FRANCE;case "es":return Locale.forLanguageTag("es-ES");case "it":return Locale.ITALY;default:return Locale.forLanguageTag(code);}}
 static int adjacent(Context c,int i,int dir){String lang=language(c,i);JSONArray a=lessons(c);for(int k=i+dir;k>=0&&k<a.length();k+=dir)if(language(c,k).equals(lang))return k;return -1;}
 static void saveTranslated(Context c,String title,String lang,JSONArray pairs)throws JSONException{if(pairs.length()==0)throw new JSONException("Порожній урок");JSONArray all=lessons(c);all.put(new JSONObject().put("title",title).put("language",lang).put("phrases",pairs));if(!prefs(c).edit().putString("lessons",all.toString()).commit())throw new JSONException("Не вдалося зберегти урок");}
 static boolean cyr(String s){return s.matches("(?s).*[А-Яа-яІіЇїЄєҐґ].*");}
 static JSONObject pair(String a,String b)throws JSONException{JSONObject p=new JSONObject();p.put("de",cyr(a)&&!cyr(b)?b:a);p.put("uk",cyr(a)&&!cyr(b)?a:b);return p;}
 static int add(Context c,String name,String raw,String lang)throws Exception{
  JSONArray all=lessons(c), pairs=new JSONArray();ArrayList<String> plain=new ArrayList<>();int count=0;
  for(String l:raw.split("\\R")){l=l.trim();if(l.isEmpty())continue;if(l.matches("(?i)^урок\\s+\\d+.*")){if(pairs.length()>0||plain.size()>0){count+=finish(all,name,pairs,plain,lang);pairs=new JSONArray();plain.clear();}name=l;continue;}
   String[] p=l.split("\\s*[|;\\t]\\s*",2);if(p.length==1 && l.contains(",")){String[] q=l.split(",",2);if(cyr(q[0])!=cyr(q[1]))p=q;}
   if(p.length==2&&cyr(p[0])!=cyr(p[1])){if(pairs.length()<1000)pairs.put(pair(p[0].trim(),p[1].trim()));}else plain.add(l);
  }count+=finish(all,name,pairs,plain,lang);if(count==0)throw new IOException("Не знайдено двомовних фраз. Формат: іноземна фраза | Українська");prefs(c).edit().putString("lessons",all.toString()).apply();return count;
 }
 static int finish(JSONArray all,String name,JSONArray p,ArrayList<String> plain,String lang)throws JSONException{for(int i=0;i+1<plain.size()&&p.length()<1000;i++){String a=plain.get(i),b=plain.get(i+1);if(cyr(a)==cyr(b)||a.length()>400||b.length()>400)continue;p.put(pair(a,b));i++;if(i+1<plain.size()&&plain.get(i+1).equals(a))i++;}if(p.length()>0)all.put(new JSONObject().put("title",name).put("language",lang).put("phrases",p));return p.length();}
 static void delete(Context c,int i){JSONArray a=lessons(c);a.remove(i);prefs(c).edit().putString("lessons",a.toString()).putInt("lesson",0).putInt("phrase",0).apply();}
}
