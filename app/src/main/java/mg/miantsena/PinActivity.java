package mg.miantsena;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PinActivity extends AppCompatActivity {

    public static final int VERIFIER = 0, CREER = 1, CHANGER = 2, DESACTIVER = 3;
    private static final int E_VERIF = 0, E_NOUVEAU = 1, E_CONFIRMER = 2;

    private int mode, etape;
    private String nouveauPin = "";
    private final StringBuilder saisie = new StringBuilder();
    private final View[] points = new View[Verrou.LONGUEUR];
    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView titre, sousTitre, erreur;
    private LinearLayout rangeePoints;
    private boolean occupe = false;
    private boolean bioAuto = true;
    private final Runnable tic = this::afficherBlocage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        mode = getIntent().getIntExtra("mode", VERIFIER);
        etape = mode == CREER ? E_NOUVEAU : E_VERIF;
        if (mode == VERIFIER) Verrou.pinAffiche = true;

        titre = findViewById(R.id.titre);
        sousTitre = findViewById(R.id.sous_titre);
        erreur = findViewById(R.id.erreur);
        rangeePoints = findViewById(R.id.points);

        for (int i = 0; i < Verrou.LONGUEUR; i++) {
            View p = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(16), dp(16));
            lp.setMargins(dp(10), 0, dp(10), 0);
            p.setLayoutParams(lp);
            rangeePoints.addView(p);
            points[i] = p;
        }
        construireClavier();

        View annuler = findViewById(R.id.btn_annuler);
        annuler.setVisibility(mode == VERIFIER ? View.GONE : View.VISIBLE);
        annuler.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        View oublie = findViewById(R.id.btn_oublie);
        oublie.setVisibility(mode == VERIFIER ? View.VISIBLE : View.GONE);
        oublie.setOnClickListener(v -> pinOublie());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mode == VERIFIER) {
                    moveTaskToBack(true);
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        });

        majTextes();
        majPoints();
        afficherBlocage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mode == VERIFIER && bioAuto && Verrou.bioActive(this) && Bio.disponible(this)) {
            bioAuto = false;
            lancerBio();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        bioAuto = true;
    }

    private void lancerBio() {
        Bio.demander(this, "Déverrouillez avec votre empreinte digitale", () -> {
            Verrou.reinitialiserEchecs(this);
            Verrou.deverrouiller();
            setResult(RESULT_OK);
            finish();
            overridePendingTransition(0, 0);
        }, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        h.removeCallbacksAndMessages(null);
        if (mode == VERIFIER) Verrou.pinAffiche = false;
    }

    // ----- Interface -----
    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void construireClavier() {
        LinearLayout clavier = findViewById(R.id.clavier);
        String[][] rangees = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"", "0", "<"}};
        for (String[] r : rangees) {
            LinearLayout ligne = new LinearLayout(this);
            ligne.setOrientation(LinearLayout.HORIZONTAL);
            ligne.setGravity(Gravity.CENTER);
            for (final String t : r) {
                int taille = dp(72);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(taille, taille);
                lp.setMargins(dp(14), dp(7), dp(14), dp(7));
                if (t.isEmpty() && mode == VERIFIER && Verrou.bioActive(this) && Bio.disponible(this)) {
                    ImageView b = new ImageView(this);
                    b.setLayoutParams(lp);
                    b.setImageResource(R.drawable.ic_fingerprint);
                    b.setColorFilter(0xFFFFFFFF);
                    b.setPadding(dp(20), dp(20), dp(20), dp(20));
                    b.setBackgroundResource(R.drawable.bg_key);
                    b.setContentDescription("Empreinte digitale");
                    b.setOnClickListener(v -> lancerBio());
                    ligne.addView(b);
                } else if (t.isEmpty()) {
                    View vide = new View(this);
                    vide.setLayoutParams(lp);
                    ligne.addView(vide);
                } else if (t.equals("<")) {
                    ImageView b = new ImageView(this);
                    b.setLayoutParams(lp);
                    b.setImageResource(R.drawable.ic_backspace);
                    b.setColorFilter(0xFFFFFFFF);
                    b.setPadding(dp(22), dp(22), dp(22), dp(22));
                    b.setBackgroundResource(R.drawable.bg_key);
                    b.setContentDescription("Effacer");
                    b.setOnClickListener(v -> effacer());
                    ligne.addView(b);
                } else {
                    TextView b = new TextView(this);
                    b.setLayoutParams(lp);
                    b.setText(t);
                    b.setGravity(Gravity.CENTER);
                    b.setTextColor(0xFFFFFFFF);
                    b.setTextSize(28);
                    b.setBackgroundResource(R.drawable.bg_key);
                    b.setOnClickListener(v -> appuyer(t));
                    ligne.addView(b);
                }
            }
            clavier.addView(ligne);
        }
    }

    private void majTextes() {
        erreur.setText("");
        if (etape == E_VERIF) {
            titre.setText(mode == VERIFIER ? "Miantsena est verrouillé" : "Code PIN actuel");
            sousTitre.setText(mode == DESACTIVER ? "Entrez votre code pour désactiver le verrouillage"
                    : mode == CHANGER ? "Entrez votre code actuel" : "Entrez votre code PIN");
        } else if (etape == E_NOUVEAU) {
            titre.setText("Nouveau code PIN");
            sousTitre.setText("Choisissez un code à " + Verrou.LONGUEUR + " chiffres");
        } else {
            titre.setText("Confirmez le code");
            sousTitre.setText("Saisissez à nouveau le même code");
        }
    }

    private void majPoints() {
        for (int i = 0; i < points.length; i++) {
            points[i].setBackgroundResource(i < saisie.length() ? R.drawable.dot_plein : R.drawable.dot_vide);
        }
    }

    private void secouer() {
        TranslateAnimation a = new TranslateAnimation(0, dp(14), 0, 0);
        a.setDuration(350);
        a.setInterpolator(new CycleInterpolator(3));
        rangeePoints.startAnimation(a);
    }

    private void afficherBlocage() {
        long reste = Verrou.tempsBloque(this);
        if (reste > 0) {
            erreur.setText("Trop d'essais. Réessayez dans " + ((reste + 999) / 1000) + " s");
            h.removeCallbacks(tic);
            h.postDelayed(tic, 1000);
        } else if (erreur.getText().toString().startsWith("Trop d'essais")) {
            erreur.setText("");
        }
    }

    // ----- Saisie -----
    private void appuyer(String chiffre) {
        if (occupe || Verrou.tempsBloque(this) > 0) return;
        if (saisie.length() >= Verrou.LONGUEUR) return;
        saisie.append(chiffre);
        erreur.setText("");
        majPoints();
        if (saisie.length() == Verrou.LONGUEUR) {
            occupe = true;
            h.postDelayed(() -> {
                occupe = false;
                valider();
            }, 150);
        }
    }

    private void effacer() {
        if (occupe || saisie.length() == 0) return;
        saisie.deleteCharAt(saisie.length() - 1);
        majPoints();
    }

    private void valider() {
        String pin = saisie.toString();
        saisie.setLength(0);
        majPoints();

        if (etape == E_VERIF) {
            if (Verrou.verifier(this, pin)) {
                Verrou.reinitialiserEchecs(this);
                if (mode == VERIFIER) {
                    Verrou.deverrouiller();
                    setResult(RESULT_OK);
                    finish();
                    overridePendingTransition(0, 0);
                } else if (mode == DESACTIVER) {
                    Verrou.supprimer(this);
                    terminer("Verrouillage par code PIN désactivé");
                } else { // CHANGER
                    etape = E_NOUVEAU;
                    majTextes();
                }
            } else {
                int restants = Verrou.enregistrerEchec(this);
                secouer();
                if (restants == 0) afficherBlocage();
                else erreur.setText("Code incorrect • " + restants + " essai" + (restants > 1 ? "s" : "") + " restant" + (restants > 1 ? "s" : ""));
            }
        } else if (etape == E_NOUVEAU) {
            nouveauPin = pin;
            etape = E_CONFIRMER;
            majTextes();
        } else {
            if (pin.equals(nouveauPin)) {
                boolean modif = mode == CHANGER;
                Verrou.definir(this, pin);
                Verrou.deverrouiller();
                terminer(modif ? "Code PIN modifié" : "Verrouillage par code PIN activé");
            } else {
                secouer();
                etape = E_NOUVEAU;
                majTextes();
                erreur.setText("Les codes ne correspondent pas. Recommencez.");
            }
        }
    }

    private void terminer(String message) {
        setResult(RESULT_OK, new Intent().putExtra("message", message));
        finish();
    }

    private void pinOublie() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Code PIN oublié ?")
                .setMessage("Vous serez déconnecté et votre code PIN sera supprimé. Reconnectez-vous avec Google : vos listes reviendront depuis le cloud, et vous pourrez définir un nouveau code.")
                .setPositiveButton("Se déconnecter", (d, w) -> {
                    Verrou.supprimerPour(this, Sync.email(this));
                    Sync.deconnecter(this);
                    Verrou.deverrouiller();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}
