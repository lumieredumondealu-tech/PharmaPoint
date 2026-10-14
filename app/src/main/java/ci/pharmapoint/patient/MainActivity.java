package ci.pharmapoint.patient;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.firebase.messaging.FirebaseMessaging;

/**
 * PharmaPoint Patient - Java 8 / Android SDK 29 compatible.
 * Real data are loaded from Supabase when SUPABASE_URL and SUPABASE_ANON_KEY are configured.
 * Never put a Supabase service_role key in an Android application.
 */
public class MainActivity extends Activity {
    // IMPORTANT: set these two values to the Supabase Project URL and publishable/anon key.
    private static final String SUPABASE_URL = "https://flegpvdzxywhankrbgdy.supabase.co";
    private static final String SUPABASE_ANON_KEY = "sb_publishable_UMpKbhGLKTuwIPsRIp_FJg_rZfTgArO";
    private static final String ADS_TABLE = "publicites";
    private static final String PHARMACIES_TABLE = "pharmacies_actives";
    private static final String ORDERS_TABLE = "commandes";
    private static final String STORAGE_BUCKET = "ordonnances"; // create this bucket in Supabase Storage

    private static final int BLUE = Color.rgb(18, 92, 150);
    private static final int NAVY = Color.rgb(8, 45, 78);
    private static final int GREEN = Color.rgb(31, 145, 55);
    private static final int BG = Color.rgb(245, 248, 252);
    private static final int INK = Color.rgb(32, 48, 64);
    private static final int MUTED = Color.rgb(104, 119, 134);
    private static final int PICK_PRESCRIPTION = 701;

    private LinearLayout root, content;
    private SharedPreferences prefs;
    private final Handler handler = new Handler();
    private final List<JSONObject> pharmacies = new ArrayList<>();
    private final List<JSONObject> ads = new ArrayList<>();
    private Uri prescriptionUri;
    private int adIndex = 0;
    private Runnable adRotation;
    private TextView adCaption;
    private FrameLayout adMediaFrame;
    private boolean loadingPharmacies = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("pharmapoint_patient", MODE_PRIVATE);
        requestNotificationPermissionIfNeeded();
        registerForPushToken();
        showHome();
    }

    private boolean configured() {
        return !SUPABASE_URL.contains("VOTRE-PROJET") && !SUPABASE_ANON_KEY.contains("VOTRE_CLE")
                && SUPABASE_URL.startsWith("https://") && SUPABASE_ANON_KEY.length() > 20;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 77);
        }
    }

    private void registerForPushToken() {
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    prefs.edit().putString("fcm_token", task.getResult()).apply();
                    // Token is retained locally. A secure backend must associate it with the patient account.
                }
            });
        } catch (Exception ignored) { }
    }

    private void screen(String title) {
        if (adRotation != null) handler.removeCallbacks(adRotation);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(15), dp(20), dp(15));
        header.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{NAVY, BLUE}));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("PharmaPoint", 25, Color.WHITE, true);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        Button cart = button("Mon panier", GREEN);
        cart.setTextSize(12);
        cart.setOnClickListener(v -> showCart());
        top.addView(cart);
        header.addView(top);
        TextView slogan = text("Votre pharmacie, partout avec vous.", 13, Color.WHITE, false);
        slogan.setPadding(0, dp(5), 0, 0);
        header.addView(slogan);
        root.addView(header);

        LinearLayout nav = new LinearLayout(this);
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(dp(5), dp(3), dp(5), dp(3));
        navButton(nav, "Accueil", this::showHome);
        navButton(nav, "Pharmacies", this::showPharmacies);
        navButton(nav, "M茅decins", this::showDoctors);
        navButton(nav, "Commandes", this::showOrders);
        root.addView(nav);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(17), dp(17), dp(17), dp(28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        setTitle(title);
    }

    private void showHome() {
        screen("PharmaPoint Patient");
        String name = prefs.getString("profile_name", "").trim();
        addText(name.isEmpty() ? "Bonjour et bienvenue" : "Bonjour, " + name, 25, NAVY, true);
        addText("Soigner  鈥�  Simplifier  鈥�  Connecter", 13, GREEN, true);
        addSpace(12);

        addText("Actualit茅s & services", 18, NAVY, true);
        buildAdCarousel();
        addSpace(16);
        addText("Que souhaitez-vous faire ?", 19, NAVY, true);
        addFeatureTile("鉁�  Pharmacies & M茅dicaments", "Rechercher un produit, voir les pharmacies partenaires et envoyer une ordonnance.", BLUE, this::showPharmacies);
        addFeatureTile("鈿�  Consulter un m茅decin", "Pr茅-茅valuation, rendez-vous, t茅l茅consultation et dossier m茅dical.", GREEN, this::showDoctors);
        addSpace(5);
        addAction("Rechercher un m茅dicament", this::showMedicines, BLUE);
        addAction("Envoyer une ordonnance", this::showPrescription, NAVY);
        addAction("Mes dossiers m茅dicaux", this::showMedicalRecord, GREEN);
        addAction("Mon profil", this::showProfile, BLUE);
        addText("Les informations m茅dicales ne remplacent pas un avis professionnel. En cas d'urgence, contactez les services locaux.", 12, MUTED, false);
        loadAds();
    }

    private void buildAdCarousel() {
        LinearLayout card = card();
        adMediaFrame = new FrameLayout(this);
        adMediaFrame.setBackground(round(Color.rgb(225, 237, 246), 16, 0, Color.TRANSPARENT));
        card.addView(adMediaFrame, new LinearLayout.LayoutParams(-1, dp(175)));
        adCaption = text("PharmaPoint 鈥� des services de sant茅 plus proches de vous", 14, NAVY, true);
        adCaption.setPadding(dp(2), dp(10), dp(2), dp(2));
        card.addView(adCaption);
        LinearLayout.LayoutParams lp = match(-2); lp.bottomMargin = dp(12); content.addView(card, lp);
        TextView dots = text("鈼�  鈼�  鈼�", 12, GREEN, true); dots.setGravity(Gravity.CENTER); card.addView(dots);
        showAdPlaceholder();
    }

    private void showAdPlaceholder() {
        if (adMediaFrame == null) return;
        adMediaFrame.removeAllViews();
        LinearLayout placeholder = new LinearLayout(this);
        placeholder.setOrientation(LinearLayout.VERTICAL);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setPadding(dp(12), dp(12), dp(12), dp(12));
        TextView symbol = text("鉁�", 42, GREEN, true); symbol.setGravity(Gravity.CENTER);
        placeholder.addView(symbol);
        TextView title = text("La sant茅, plus proche de vous", 16, NAVY, true); title.setGravity(Gravity.CENTER);
        placeholder.addView(title);
        TextView sub = text("Les campagnes actives appara卯tront ici", 12, MUTED, false); sub.setGravity(Gravity.CENTER);
        placeholder.addView(sub);
        adMediaFrame.addView(placeholder, new FrameLayout.LayoutParams(-1, -1));
    }

    private void loadAds() {
        if (!configured()) { showAdPlaceholder(); return; }
        getJson(ADS_TABLE + "?select=*&actif=eq.true&order=ordre.asc", result -> {
            ads.clear();
            if (result != null) for (int i = 0; i < result.length(); i++) ads.add(result.optJSONObject(i));
            adIndex = 0;
            renderCurrentAd();
            if (adRotation != null) handler.removeCallbacks(adRotation);
            adRotation = new Runnable() { @Override public void run() {
                if (!ads.isEmpty()) { adIndex = (adIndex + 1) % ads.size(); renderCurrentAd(); }
                handler.postDelayed(this, 5000);
            }};
            handler.postDelayed(adRotation, 5000);
        });
    }

    private void renderCurrentAd() {
        if (adMediaFrame == null) return;
        if (ads.isEmpty()) { showAdPlaceholder(); if (adCaption != null) adCaption.setText("PharmaPoint 鈥� Votre pharmacie, partout avec vous."); return; }
        JSONObject item = ads.get(adIndex);
        String title = first(item, "titre", "title", "nom");
        String media = first(item, "url_image", "image_url", "url", "media_url", "URL de l鈥檌mage", "URL de l'image");
        String type = first(item, "type_media", "media_type", "type").toLowerCase(Locale.ROOT);
        if (adCaption != null) adCaption.setText(title.isEmpty() ? "D茅couvrez les services PharmaPoint" : title);
        adMediaFrame.removeAllViews();
        if (media.isEmpty()) { showAdPlaceholder(); return; }
        if (type.contains("video") || media.toLowerCase(Locale.ROOT).matches(".*\\.(mp4|webm)(\\?.*)?$")) {
            VideoView video = new VideoView(this);
            adMediaFrame.addView(video, new FrameLayout.LayoutParams(-1, -1));
            video.setVideoURI(Uri.parse(media));
            video.setOnPreparedListener(mp -> { mp.setLooping(true); video.start(); });
            video.setOnErrorListener((mp, what, extra) -> { showAdPlaceholder(); return true; });
        } else {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            adMediaFrame.addView(image, new FrameLayout.LayoutParams(-1, -1));
            new AsyncTask<Void, Void, android.graphics.Bitmap>() {
                protected android.graphics.Bitmap doInBackground(Void... v) {
                    try { HttpURLConnection c = (HttpURLConnection)new URL(media).openConnection(); c.setConnectTimeout(7000); c.setReadTimeout(7000); c.connect();
                        InputStream in = c.getInputStream(); android.graphics.Bitmap b = android.graphics.BitmapFactory.decodeStream(in); in.close(); c.disconnect(); return b;
                    } catch (Exception e) { return null; }
                }
                protected void onPostExecute(android.graphics.Bitmap b) { if (b != null && image.getParent() != null) image.setImageBitmap(b); else if (b == null) showAdPlaceholder(); }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void showPharmacies() {
        screen("Pharmacies partenaires");
        addText("Pharmacies partenaires", 24, NAVY, true);
        addText("Les 茅tablissements actifs sont charg茅s depuis votre base de donn茅es.", 13, MUTED, false);
        addAction("Rechercher un m茅dicament", this::showMedicines, BLUE);
        addAction("Envoyer une ordonnance", this::showPrescription, GREEN);
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); content.addView(list);
        if (!configured()) { addNotice("Connexion Supabase 脿 configurer : renseigner l'URL du projet et la cl茅 publiable dans MainActivity.java."); return; }
        addText("Chargement des pharmacies鈥�", 14, MUTED, false);
        loadPharmacies(list, true);
    }

    private void loadPharmacies(LinearLayout list, boolean showMessage) {
        if (loadingPharmacies) return; loadingPharmacies = true;
        getJson(PHARMACIES_TABLE + "?select=*", result -> {
            loadingPharmacies = false; list.removeAllViews(); pharmacies.clear();
            if (result == null) { addNotice("Impossible de charger les pharmacies. V茅rifiez la connexion, les r猫gles RLS et les noms de colonnes Supabase."); return; }
            for (int i=0;i<result.length();i++) {
                JSONObject p = result.optJSONObject(i); if (p == null) continue;
                String status = first(p,"statut","status","actif","active").toLowerCase(Locale.ROOT);
                if ("false".equals(status) || "inactif".equals(status) || "inactive".equals(status)) continue;
                pharmacies.add(p);
                String name = first(p,"nom","pharmacie_nom","name","nom_pharmacie");
                if (name.isEmpty()) name = "Pharmacie partenaire " + pharmacies.size();
                String address = first(p,"adresse","address","localisation","commune","ville");
                String phone = first(p,"telephone","t茅l茅phone","phone","contact");
                LinearLayout c = card(); c.addView(text(name,17,NAVY,true));
                if (!address.isEmpty()) c.addView(text(address,13,MUTED,false));
                if (!phone.isEmpty()) c.addView(text("T茅l. " + phone,13,INK,false));
                c.addView(text("Disponibilit茅 脿 confirmer aupr猫s de la pharmacie.",12,MUTED,false));
                Button choose = button("Choisir cette pharmacie", BLUE);
                final JSONObject selected = p;
                choose.setOnClickListener(v -> orderForPharmacy(selected)); c.addView(choose);
                list.addView(c, spacedMatch(-2));
            }
            if (list.getChildCount()==0) addNotice("Aucune pharmacie active retourn茅e. V茅rifiez public.pharmacies_actives et ses r猫gles d'acc猫s.");
        });
    }

    private void orderForPharmacy(JSONObject pharmacy) {
        EditText product = new EditText(this); product.setHint("M茅dicament / produit souhait茅");
        EditText quantity = new EditText(this); quantity.setHint("Quantit茅 (ex. 1)"); quantity.setInputType(2);
        EditText address = new EditText(this); address.setHint("Adresse de livraison (facultatif)");
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.addView(product); form.addView(quantity); form.addView(address);
        new AlertDialog.Builder(this).setTitle("Nouvelle demande").setView(form)
            .setNegativeButton("Annuler",null).setPositiveButton("Envoyer",(d,w)->{
                String item=product.getText().toString().trim(); if(item.isEmpty()){toast("Indiquez le produit souhait茅.");return;}
                submitOrder(pharmacy,item,parseQty(quantity.getText().toString()),address.getText().toString().trim(),"");
            }).show();
    }

    private int parseQty(String s) { try { return Math.max(1,Integer.parseInt(s.trim())); } catch(Exception e){return 1;} }

    private void showMedicines() {
        screen("Recherche m茅dicaments");
        addText("Rechercher un m茅dicament",24,NAVY,true);
        EditText query = new EditText(this); query.setSingleLine(true); query.setHint("Nom du m茅dicament ou principe actif");
        query.setPadding(dp(14),dp(8),dp(14),dp(8)); query.setBackground(round(Color.WHITE,14,1,Color.rgb(220,228,235)));
        content.addView(query,match(dp(52)));
        addText("La recherche de disponibilit茅 n茅cessite une table de stock reli茅e aux pharmacies.",12,MUTED,false);
        addAction("Voir les pharmacies partenaires",this::showPharmacies,BLUE);
        addAction("Envoyer une ordonnance",this::showPrescription,GREEN);
        addText("Conseil : les stocks doivent 锚tre confirm茅s directement par la pharmacie.",12,MUTED,false);
    }

    private void showPrescription() {
        screen("Envoyer une ordonnance");
        addText("Envoyer une ordonnance",24,NAVY,true);
        addText("Choisissez une photo ou un document. L'envoi dans Supabase Storage doit 锚tre activ茅 avec le bucket et les r猫gles d'acc猫s adapt茅s.",14,INK,false);
        addAction("Choisir une photo / un PDF",()->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
            i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf"}); startActivityForResult(i,PICK_PRESCRIPTION);
        },BLUE);
        if(prescriptionUri!=null) addText("Fichier s茅lectionn茅 : "+fileName(prescriptionUri),13,GREEN,true);
        addAction("Voir les pharmacies partenaires",this::showPharmacies,GREEN);
        addNotice("Important : ne partagez pas de donn茅es m茅dicales sensibles via un espace de stockage public. Le bucket ordonnances doit 锚tre priv茅 avec des politiques d'acc猫s s茅curis茅es.");
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_PRESCRIPTION && resultCode==RESULT_OK && data!=null && data.getData()!=null) {
            prescriptionUri=data.getData(); try { getContentResolver().takePersistableUriPermission(prescriptionUri,data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION)); } catch(Exception ignored){}
            showPrescription(); toast("Fichier s茅lectionn茅. L'envoi s茅curis茅 doit 锚tre configur茅.");
        }
    }

    private String fileName(Uri uri) {
        android.database.Cursor c=null; try { c=getContentResolver().query(uri,null,null,null,null); if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)return c.getString(i);} } catch(Exception ignored){} finally {if(c!=null)c.close();} return "document s茅lectionn茅";
    }

    private void submitOrder(JSONObject pharmacy,String product,int qty,String address,String prescriptionUrl) {
        if(!configured()){toast("Configurez Supabase avant d'envoyer une commande.");return;}
        try {
            JSONObject body=new JSONObject();
            body.put("patient_id",prefs.getString("patient_id","patient-"+android.provider.Settings.Secure.getString(getContentResolver(),android.provider.Settings.Secure.ANDROID_ID)));
            body.put("patient_nom",prefs.getString("profile_name","Patient"));
            body.put("telephone",prefs.getString("profile_phone",""));
            body.put("pharmacie_id",first(pharmacy,"id","identifiant","pharmacie_id"));
            body.put("pharmacie_nom",first(pharmacy,"nom","pharmacie_nom","name"));
            body.put("produit",product); body.put("quantite",qty); body.put("prescription_url",prescriptionUrl);
            body.put("statut","EN_ATTENTE"); body.put("adresse_livraison",address);
            postJson(ORDERS_TABLE,body,ok->{ if(ok){toast("Demande envoy茅e 脿 la pharmacie.");showOrders();}else toast("脡chec de l'envoi. V茅rifiez les colonnes de public.commandes et les r猫gles RLS.");});
        } catch(Exception e){toast("Impossible de pr茅parer la commande.");}
    }

    private void showCart() {
        screen("Mon panier"); addText("Mon panier",24,NAVY,true);
        addText("Le panier et le paiement en ligne seront reli茅s aux commandes r茅elles apr猫s validation du parcours de commande.",14,INK,false);
        addAction("Rechercher un m茅dicament",this::showMedicines,BLUE);
        addAction("Choisir une pharmacie",this::showPharmacies,GREEN);
    }

    private void showOrders() {
        screen("Mes commandes"); addText("Suivi de mes commandes",24,NAVY,true);
        if(!configured()){addNotice("Configurez Supabase pour afficher les commandes r茅elles.");return;}
        addText("Chargement鈥�",13,MUTED,false);
        String patientId=prefs.getString("patient_id","patient-"+android.provider.Settings.Secure.getString(getContentResolver(),android.provider.Settings.Secure.ANDROID_ID));
        String path=ORDERS_TABLE+"?select=*&patient_id=eq."+encode(patientId)+"&order=created_at.desc";
        getJson(path,result->{ if(result==null){addNotice("Impossible de charger les commandes. V茅rifiez l'acc猫s 脿 public.commandes.");return;}
            content.removeViews(Math.max(1,content.getChildCount()-1),Math.min(1,content.getChildCount()));
            if(result.length()==0){addText("Aucune commande pour le moment.",14,MUTED,false);return;}
            for(int i=0;i<result.length();i++){JSONObject o=result.optJSONObject(i);if(o==null)continue;String prod=first(o,"produit");String status=first(o,"statut");String pharmacy=first(o,"pharmacie_nom");LinearLayout c=card();c.addView(text(prod.isEmpty()?"Commande":prod,16,NAVY,true));c.addView(text("Statut : "+(status.isEmpty()?"EN_ATTENTE":status),14,GREEN,true));if(!pharmacy.isEmpty())c.addView(text(pharmacy,13,MUTED,false));content.addView(c,spacedMatch(-2));}
        });
    }

    private void showDoctors() {
        screen("Consulter un m茅decin"); addText("Votre sant茅, accompagn茅e",24,NAVY,true);
        addText("Un parcours simple pour pr茅parer et organiser votre consultation.",14,MUTED,false);
        addFeatureTile("Pr茅-茅valuation", "Pr茅parer les sympt么mes 脿 partager avec un professionnel.", BLUE, this::showPreEvaluation);
        addFeatureTile("Rendez-vous m茅dicaux", "Demande de rendez-vous et informations pratiques.", GREEN, this::showAppointments);
        addFeatureTile("T茅l茅consultation", "La visioconf茅rence n茅cessite un service m茅dical int茅gr茅.", BLUE, this::showAppointments);
        addFeatureTile("Dossiers m茅dicaux", "Carnet de sant茅, vaccinations et rappels.", GREEN, this::showMedicalRecord);
    }

    private void showPreEvaluation() {
        screen("Pr茅-茅valuation"); addText("Pr茅parer ma consultation",24,NAVY,true);
        addText("Cette aide ne pose pas de diagnostic et ne remplace pas un professionnel de sant茅.",13,MUTED,false);
        EditText reason=new EditText(this);reason.setHint("D茅crivez votre motif de consultation");reason.setMinLines(4);reason.setGravity(Gravity.TOP);content.addView(reason,match(dp(125)));
        addAction("Pr茅parer le r茅sum茅",()->{String s=reason.getText().toString().trim();if(s.isEmpty())s="Aucun motif renseign茅.";new AlertDialog.Builder(this).setTitle("R茅sum茅 脿 partager avec le m茅decin").setMessage(s).setPositiveButton("Fermer",null).show();},GREEN);
    }

    private void showAppointments() {
        screen("Rendez-vous m茅dicaux"); addText("Rendez-vous & t茅l茅consultation",24,NAVY,true);
        addText("Le calendrier, la r茅servation confirm茅e et la vid茅o doivent 锚tre reli茅s 脿 un service de prise de rendez-vous et 脿 un prestataire de t茅l茅consultation. Cette page ne confirme aucune r茅servation.",14,INK,false);
        addAction("Pr茅parer une pr茅-茅valuation",this::showPreEvaluation,BLUE);
    }

    private void showMedicalRecord() {
        screen("Dossiers m茅dicaux"); addText("Mon dossier de sant茅",24,NAVY,true);
        addNotice("Les donn茅es de sant茅 doivent 锚tre stock茅es dans un espace priv茅, avec authentification et r猫gles d'acc猫s par patient.");
        addAction("Carnet de sant茅",()->showInfo("Carnet de sant茅","Le stockage s茅curis茅 du carnet n'est pas encore reli茅 脿 une table de donn茅es d茅di茅e."),BLUE);
        addAction("Vaccinations et rappels",()->showInfo("Vaccinations","Les rappels n茅cessitent un dossier s茅curis茅 et un m茅canisme de notification programm茅."),GREEN);
        addAction("Suivi du poids",()->showInfo("Suivi du poids","Le suivi sera activ茅 apr猫s cr茅ation et s茅curisation de la table correspondante."),BLUE);
    }

    private void showProfile() {
        screen("Mon profil"); addText("Mon profil",24,NAVY,true);
        EditText name=new EditText(this);name.setSingleLine(true);name.setHint("Nom complet");name.setText(prefs.getString("profile_name",""));content.addView(name,match(dp(52)));
        EditText phone=new EditText(this);phone.setSingleLine(true);phone.setHint("T茅l茅phone");phone.setInputType(3);phone.setText(prefs.getString("profile_phone",""));content.addView(phone,match(dp(52)));
        addAction("Enregistrer",()->{prefs.edit().putString("profile_name",name.getText().toString().trim()).putString("profile_phone",phone.getText().toString().trim()).apply();toast("Profil enregistr茅 sur cet appareil.");},GREEN);
    }

    private void getJson(String path,JsonCallback callback) {
        if(!configured()){callback.done(null);return;}
        new AsyncTask<Void,Void,JSONArray>() { protected JSONArray doInBackground(Void... v){HttpURLConnection c=null;try{
            URL u=new URL(SUPABASE_URL+"/rest/v1/"+path);c=(HttpURLConnection)u.openConnection();c.setRequestMethod("GET");c.setConnectTimeout(12000);c.setReadTimeout(12000);c.setRequestProperty("apikey",SUPABASE_ANON_KEY);c.setRequestProperty("Authorization","Bearer "+SUPABASE_ANON_KEY);c.setRequestProperty("Accept","application/json");
            int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String raw=read(in);if(code<200||code>=300)return null;return new JSONArray(raw);
        }catch(Exception e){return null;}finally{if(c!=null)c.disconnect();}}
            protected void onPostExecute(JSONArray a){callback.done(a);}
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void postJson(String table,JSONObject body,BooleanCallback callback) {
        new AsyncTask<Void,Void,Boolean>() { protected Boolean doInBackground(Void... v){HttpURLConnection c=null;try{
            URL u=new URL(SUPABASE_URL+"/rest/v1/"+table);c=(HttpURLConnection)u.openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(12000);c.setReadTimeout(12000);c.setRequestProperty("apikey",SUPABASE_ANON_KEY);c.setRequestProperty("Authorization","Bearer "+SUPABASE_ANON_KEY);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Prefer","return=minimal");byte[] bytes=body.toString().getBytes("UTF-8");OutputStream out=c.getOutputStream();out.write(bytes);out.close();int code=c.getResponseCode();return code>=200&&code<300;
        }catch(Exception e){return false;}finally{if(c!=null)c.disconnect();}}
            protected void onPostExecute(Boolean ok){callback.done(ok!=null&&ok);}
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private String read(InputStream in)throws Exception {if(in==null)return "";ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);in.close();return b.toString("UTF-8");}
    private String encode(String s){try{return URLEncoder.encode(s,"UTF-8");}catch(Exception e){return s;}}
    private String first(JSONObject o,String... keys){if(o==null)return "";for(String k:keys){Object v=o.opt(k);if(v!=null&&!JSONObject.NULL.equals(v)){String s=String.valueOf(v).trim();if(!s.isEmpty()&&!"null".equalsIgnoreCase(s))return s;}}return "";}

    private void addFeatureTile(String title,String description,int accent,Runnable action){LinearLayout c=card();c.setPadding(dp(17),dp(17),dp(17),dp(17));c.setBackground(round(Color.WHITE,20,2,accent));c.addView(text(title,19,accent,true));TextView d=text(description,13,INK,false);d.setPadding(0,dp(8),0,dp(12));c.addView(d);Button b=button("D茅couvrir",accent);b.setOnClickListener(v->action.run());c.addView(b);content.addView(c,spacedMatch(-2));}
    private void addAction(String label,Runnable action,int color){Button b=button(label,color);b.setOnClickListener(v->action.run());content.addView(b,spacedMatch(dp(51)));}
    private void addNotice(String message){LinearLayout c=card();TextView t=text(message,13,INK,false);c.addView(t);content.addView(c,spacedMatch(-2));}
    private void addText(String s,int size,int color,boolean bold){TextView t=text(s,size,color,bold);t.setPadding(0,dp(3),0,dp(7));content.addView(t,match(-2));}
    private void addSpace(int h){View v=new View(this);content.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(14),dp(14),dp(14));c.setBackground(round(Color.WHITE,16,1,Color.rgb(229,235,241)));return c;}
    private void navButton(LinearLayout nav,String label,Runnable r){Button b=new Button(this);b.setText(label);b.setTextSize(10);b.setAllCaps(false);b.setTextColor(BLUE);b.setPadding(0,0,0,0);b.setBackgroundColor(Color.TRANSPARENT);b.setOnClickListener(v->r.run());nav.addView(b,new LinearLayout.LayoutParams(0,dp(45),1));}
    private Button button(String label,int color){Button b=new Button(this);b.setText(label);b.setTextSize(14);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(Color.WHITE);b.setBackground(round(color,13,0,color));b.setPadding(dp(12),dp(5),dp(12),dp(5));return b;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private GradientDrawable round(int color,int radius,int stroke,int strokeColor){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke>0)d.setStroke(dp(stroke),strokeColor);return d;}
    private LinearLayout.LayoutParams match(int h){return new LinearLayout.LayoutParams(-1,h);}
    private LinearLayout.LayoutParams spacedMatch(int h){LinearLayout.LayoutParams p=match(h);p.bottomMargin=dp(11);return p;}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void showInfo(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Compris",null).show();}
    @Override protected void onDestroy(){super.onDestroy();if(adRotation!=null)handler.removeCallbacks(adRotation);}
    private interface JsonCallback{void done(JSONArray result);} private interface BooleanCallback{void done(boolean ok);}
}