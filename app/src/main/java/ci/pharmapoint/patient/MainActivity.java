package ci.pharmapoint.patient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PharmaPoint Patient - stable, SDK-only MainActivity.
 * Uses programmatic views so no custom layout/resource is required.
 *
 * This compile-first version keeps navigation and local demo flows independent
 * of network availability. Supabase calls should be added back in small,
 * separately-tested methods after this version builds and launches.
 */
public class MainActivity extends Activity {
    private static final int BLUE = Color.rgb(18, 92, 150);
    private static final int DARK_BLUE = Color.rgb(8, 55, 95);
    private static final int GREEN = Color.rgb(31, 145, 55);
    private static final int LIGHT = Color.rgb(244, 248, 251);
    private static final int TEXT = Color.rgb(35, 48, 60);

    private LinearLayout root;
    private LinearLayout content;
    private TextView cartBadge;
    private final List<CartLine> cart = new ArrayList<>();
    private final List<Medicine> medicines = new ArrayList<>();
    private final List<String> demoPharmacies = new ArrayList<>();
    private SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("pharmapoint_patient", MODE_PRIVATE);
        seedDemoData();
        showHome();
    }

    private void seedDemoData() {
        medicines.add(new Medicine("Paracétamol 500 mg", "Antalgique", 1000));
        medicines.add(new Medicine("Ibuprofène 200 mg", "Anti-inflammatoire", 1800));
        medicines.add(new Medicine("Vitamine C", "Complément", 2500));
        medicines.add(new Medicine("Sérum physiologique", "Soin courant", 1200));
        demoPharmacies.add("Pharmacie partenaire — disponibilité à confirmer");
        demoPharmacies.add("Pharmacie de garde — informations à vérifier");
    }

    private void beginScreen(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(LIGHT);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(14), dp(18), dp(14));
        header.setBackgroundColor(BLUE);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("PharmaPoint", 24, Color.WHITE, true);
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        cartBadge = text("Panier (" + cartCount() + ")", 14, Color.WHITE, true);
        cartBadge.setGravity(Gravity.CENTER);
        cartBadge.setPadding(dp(10), dp(8), dp(10), dp(8));
        cartBadge.setBackground(round(GREEN, 18, 0, Color.TRANSPARENT));
        cartBadge.setOnClickListener(v -> showCart());
        top.addView(cartBadge);
        header.addView(top);

        TextView slogan = text("Votre pharmacie, partout avec vous.", 14, Color.WHITE, false);
        slogan.setPadding(0, dp(5), 0, 0);
        header.addView(slogan);
        root.addView(header);

        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(8), dp(6), dp(8), dp(6));
        nav.setBackgroundColor(Color.WHITE);
        addNavButton(nav, "Accueil", this::showHome);
        addNavButton(nav, "Commandes", this::showOrders);
        addNavButton(nav, "Profil", this::showProfile);
        root.addView(nav);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        setTitle(title);
    }

    private void showHome() {
        beginScreen("PharmaPoint Patient");
        addText("Bonjour et bienvenue", 23, DARK_BLUE, true);
        addText("Soigner • Simplifier • Connecter", 14, GREEN, true);
        addSpace(14);

        addTile("Pharmacies & Médicaments",
                "Rechercher un médicament, consulter les pharmacies partenaires ou envoyer une ordonnance.",
                BLUE, this::showPharmacies);
        addTile("Consulter un médecin",
                "Pré-évaluation, rendez-vous et téléconsultation.",
                GREEN, this::showDoctors);

        addSpace(14);
        addText("Accès rapide", 18, DARK_BLUE, true);
        addActionButton("Rechercher un médicament", this::showMedicines, BLUE);
        addActionButton("Envoyer une ordonnance", this::showPrescription, BLUE);
        addActionButton("Mes dossiers médicaux", this::showMedicalRecord, GREEN);
        addActionButton("Urgence : appeler les secours", () ->
                new AlertDialog.Builder(this)
                        .setTitle("Urgence")
                        .setMessage("En cas de danger immédiat, contactez les services d'urgence locaux.")
                        .setPositiveButton("Compris", null).show(), Color.rgb(180, 45, 45));
        addSpace(14);
        addText("Les disponibilités et informations affichées dans cette version de démonstration doivent être confirmées auprès des professionnels de santé.", 12, Color.GRAY, false);
    }

    private void showPharmacies() {
        beginScreen("Pharmacies partenaires");
        addText("Pharmacies & Médicaments", 22, DARK_BLUE, true);
        addText("La liste ci-dessous est une démonstration locale. Elle sera remplacée par les données Supabase une fois la connexion réintégrée.", 13, Color.GRAY, false);
        addActionButton("Rechercher un médicament", this::showMedicines, BLUE);
        addActionButton("Envoyer une ordonnance", this::showPrescription, GREEN);
        addSpace(8);
        for (String pharmacy : demoPharmacies) {
            addCard(pharmacy, "Contactez la pharmacie pour confirmer le stock et les horaires.");
        }
    }

    private void showMedicines() {
        beginScreen("Recherche médicaments");
        addText("Rechercher un médicament", 22, DARK_BLUE, true);
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Nom du médicament");
        search.setPadding(dp(12), dp(8), dp(12), dp(8));
        search.setBackground(round(Color.WHITE, 12, 1, Color.rgb(210, 220, 230)));
        content.addView(search, matchWrap(dp(50)));
        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        content.addView(results);
        Runnable render = () -> {
            results.removeAllViews();
            String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            for (Medicine m : medicines) {
                if (q.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(q)
                        || m.category.toLowerCase(Locale.ROOT).contains(q)) {
                    LinearLayout card = makeCard();
                    TextView name = text(m.name, 17, DARK_BLUE, true);
                    card.addView(name);
                    card.addView(text(m.category, 13, Color.GRAY, false));
                    card.addView(text(String.format(Locale.FRANCE, "%,d FCFA", m.price), 15, GREEN, true));
                    Button add = button("Ajouter au panier", BLUE);
                    add.setOnClickListener(v -> {
                        addToCart(m);
                        Toast.makeText(this, "Ajouté au panier", Toast.LENGTH_SHORT).show();
                    });
                    card.addView(add);
                    results.addView(card);
                }
            }
            if (results.getChildCount() == 0) {
                TextView empty = text("Aucun résultat dans la liste de démonstration.", 14, Color.GRAY, false);
                results.addView(empty);
            }
        };
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { render.run(); }
            public void afterTextChanged(Editable e) {}
        });
        render.run();
    }

    private void showCart() {
        beginScreen("Mon panier");
        addText("Mon panier", 22, DARK_BLUE, true);
        if (cart.isEmpty()) {
            addText("Votre panier est vide.", 15, Color.GRAY, false);
        } else {
            for (CartLine line : new ArrayList<>(cart)) {
                addCard(line.medicine.name + " × " + line.quantity,
                        String.format(Locale.FRANCE, "%,d FCFA", line.medicine.price * line.quantity));
            }
            int subtotal = subtotal();
            int service = Math.round(subtotal * 0.05f);
            addSpace(8);
            addText(String.format(Locale.FRANCE, "Sous-total : %,d FCFA", subtotal), 16, TEXT, true);
            addText(String.format(Locale.FRANCE, "Frais de service (5%%) : %,d FCFA", service), 14, TEXT, false);
            addText(String.format(Locale.FRANCE, "Total : %,d FCFA", subtotal + service), 20, GREEN, true);
            addActionButton("Continuer vers le paiement", this::showPayment, GREEN);
            addActionButton("Vider le panier", () -> {
                cart.clear();
                updateBadge();
                showCart();
            }, Color.rgb(180, 45, 45));
        }
        addActionButton("Continuer mes achats", this::showMedicines, BLUE);
    }

    private void showPayment() {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Votre panier est vide.", Toast.LENGTH_SHORT).show();
            return;
        }
        beginScreen("Paiement");
        addText("Choisir un moyen de paiement", 22, DARK_BLUE, true);
        addText("Le paiement est présenté à titre de démonstration : aucun débit réel n'est effectué.", 13, Color.GRAY, false);
        String[] methods = {"Orange Money", "MTN Mobile Money", "Wave", "Moov Money", "Carte bancaire"};
        for (String method : methods) {
            addActionButton(method, () -> new AlertDialog.Builder(this)
                    .setTitle("Confirmation")
                    .setMessage("Le paiement " + method + " n'est pas connecté dans cette version. Aucune transaction n'a été effectuée.")
                    .setPositiveButton("Compris", null).show(), BLUE);
        }
    }

    private void showPrescription() {
        beginScreen("Envoyer une ordonnance");
        addText("Envoyer une ordonnance", 22, DARK_BLUE, true);
        addText("Pour cette version de base, la sélection et l'envoi sécurisé du fichier seront réactivés après validation de la compilation.", 14, TEXT, false);
        addActionButton("Retour aux pharmacies", this::showPharmacies, BLUE);
    }

    private void showDoctors() {
        beginScreen("Consulter un médecin");
        addText("Consulter un médecin", 22, DARK_BLUE, true);
        addText("Accédez aux parcours de consultation.", 14, Color.GRAY, false);
        addTile("Pré-évaluation", "Préparer les informations à partager avec un professionnel.", BLUE, this::showPreEvaluation);
        addTile("Rendez-vous", "La réservation en ligne sera activée après raccordement du service.", GREEN, this::showAppointments);
        addTile("Téléconsultation", "La vidéo nécessite une intégration de service dédiée.", BLUE, this::showAppointments);
        addActionButton("Dossiers médicaux", this::showMedicalRecord, GREEN);
    }

    private void showPreEvaluation() {
        beginScreen("Pré-évaluation");
        addText("Pré-évaluation", 22, DARK_BLUE, true);
        addText("Cette aide ne pose pas de diagnostic et ne remplace pas un professionnel de santé.", 14, Color.GRAY, false);
        EditText symptoms = new EditText(this);
        symptoms.setHint("Décrivez brièvement votre motif de consultation");
        symptoms.setMinLines(3);
        symptoms.setGravity(Gravity.TOP);
        content.addView(symptoms, matchWrap(dp(110)));
        addActionButton("Préparer le résumé", () -> {
            String value = symptoms.getText().toString().trim();
            if (value.isEmpty()) value = "Aucun motif renseigné.";
            new AlertDialog.Builder(this).setTitle("Résumé à partager")
                    .setMessage(value).setPositiveButton("Fermer", null).show();
        }, GREEN);
    }

    private void showAppointments() {
        beginScreen("Rendez-vous");
        addText("Rendez-vous et téléconsultation", 22, DARK_BLUE, true);
        addText("La prise de rendez-vous et l'appel vidéo ne sont pas activés dans cette version de compilation de base.", 14, Color.GRAY, false);
    }

    private void showMedicalRecord() {
        beginScreen("Dossiers médicaux");
        addText("Mes dossiers médicaux", 22, DARK_BLUE, true);
        addText("Vos informations de santé personnelles ne sont pas synchronisées dans cette version de démonstration.", 14, Color.GRAY, false);
        addActionButton("Carnet de santé", () -> showInfo("Carnet de santé", "La gestion du carnet sera réactivée avec le module de données sécurisé."), BLUE);
        addActionButton("Vaccinations et rappels", () -> showInfo("Vaccinations", "Les rappels seront disponibles après réintégration du stockage sécurisé."), GREEN);
    }

    private void showOrders() {
        beginScreen("Mes commandes");
        addText("Mes commandes", 22, DARK_BLUE, true);
        addText("L'historique Supabase n'est pas interrogé au démarrage dans cette version afin que l'application puisse s'ouvrir sans réseau.", 14, Color.GRAY, false);
        addActionButton("Voir mon panier", this::showCart, BLUE);
    }

    private void showProfile() {
        beginScreen("Mon profil");
        addText("Mon profil", 22, DARK_BLUE, true);
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("Nom complet");
        name.setText(preferences.getString("profile_name", ""));
        content.addView(name, matchWrap(dp(52)));
        EditText phone = new EditText(this);
        phone.setSingleLine(true);
        phone.setHint("Téléphone");
        phone.setInputType(3);
        phone.setText(preferences.getString("profile_phone", ""));
        content.addView(phone, matchWrap(dp(52)));
        addActionButton("Enregistrer le profil", () -> {
            preferences.edit().putString("profile_name", name.getText().toString().trim())
                    .putString("profile_phone", phone.getText().toString().trim()).apply();
            Toast.makeText(this, "Profil enregistré sur cet appareil.", Toast.LENGTH_SHORT).show();
        }, GREEN);
        addText("Les données sont enregistrées localement sur cet appareil dans cette version.", 12, Color.GRAY, false);
    }

    private void showInfo(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton("OK", null).show();
    }

    private void addToCart(Medicine medicine) {
        for (CartLine line : cart) {
            if (line.medicine.name.equals(medicine.name)) {
                line.quantity++;
                updateBadge();
                return;
            }
        }
        cart.add(new CartLine(medicine));
        updateBadge();
    }

    private int cartCount() {
        int count = 0;
        for (CartLine line : cart) count += line.quantity;
        return count;
    }

    private int subtotal() {
        int total = 0;
        for (CartLine line : cart) total += line.medicine.price * line.quantity;
        return total;
    }

    private void updateBadge() {
        if (cartBadge != null) cartBadge.setText("Panier (" + cartCount() + ")");
    }

    private void addTile(String title, String description, int accent, Runnable action) {
        LinearLayout card = makeCard();
        card.setBackground(round(Color.WHITE, 20, 2, accent));
        card.setPadding(dp(16), dp(18), dp(16), dp(18));
        TextView heading = text(title, 20, accent, true);
        card.addView(heading);
        TextView detail = text(description, 14, TEXT, false);
        detail.setPadding(0, dp(8), 0, dp(12));
        card.addView(detail);
        Button open = button("Ouvrir", accent);
        open.setOnClickListener(v -> action.run());
        card.addView(open);
        content.addView(card, matchWrap(-2));
    }

    private void addCard(String title, String detail) {
        LinearLayout card = makeCard();
        card.addView(text(title, 16, DARK_BLUE, true));
        TextView sub = text(detail, 13, Color.GRAY, false);
        sub.setPadding(0, dp(6), 0, 0);
        card.addView(sub);
        content.addView(card, matchWrap(-2));
    }

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(round(Color.WHITE, 14, 1, Color.rgb(226, 233, 239)));
        LinearLayout.LayoutParams lp = matchWrap(-2);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);
        return card;
    }

    private void addText(String value, int size, int color, boolean bold) {
        TextView view = text(value, size, color, bold);
        view.setPadding(0, dp(3), 0, dp(6));
        content.addView(view, matchWrap(-2));
    }

    private void addSpace(int heightDp) {
        View space = new View(this);
        content.addView(space, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private void addActionButton(String label, Runnable action, int color) {
        Button b = button(label, color);
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = matchWrap(dp(50));
        lp.bottomMargin = dp(9);
        content.addView(b, lp);
    }

    private void addNavButton(LinearLayout nav, String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTextColor(BLUE);
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setOnClickListener(v -> action.run());
        nav.addView(b, new LinearLayout.LayoutParams(0, dp(44), 1));
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setBackground(round(color, 12, 0, color));
        b.setPadding(dp(12), dp(4), dp(12), dp(4));
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int color, int radius, int strokeWidth, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (strokeWidth > 0) d.setStroke(dp(strokeWidth), strokeColor);
        return d;
    }

    private LinearLayout.LayoutParams matchWrap(int height) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class Medicine {
        final String name;
        final String category;
        final int price;
        Medicine(String name, String category, int price) {
            this.name = name;
            this.category = category;
            this.price = price;
        }
    }

    private static class CartLine {
        final Medicine medicine;
        int quantity = 1;
        CartLine(Medicine medicine) { this.medicine = medicine; }
    }
}