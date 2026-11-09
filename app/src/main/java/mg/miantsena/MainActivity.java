package mg.miantsena;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private boolean connecte;
    private DrawerLayout drawer;
    private MaterialSwitch switchPin, switchBio;
    private View ligneBio;
    private View ligneModifierPin;
    private TextView drawerNom, drawerEmail, drawerLettre;
    private ImageView drawerPhoto;
    private ActivityResultLauncher<Intent> lanceurPin;
    private final Sync.Ecouteur syncEcouteur = new Sync.Ecouteur() {
        @Override
        public void donneesChangees() {
            rafraichir();
        }

        @Override
        public void accesRefuse() {
            Toast.makeText(MainActivity.this, R.string.login_refuse, Toast.LENGTH_LONG).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    };

    private ProjetAdapter adapter;
    private TextView vide, resume;
    private EditText recherche;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Sync.estConnecte(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        connecte = true;
        setContentView(R.layout.activity_main);

        vide = findViewById(R.id.vide);
        resume = findViewById(R.id.resume);
        recherche = findViewById(R.id.recherche);
        RecyclerView rv = findViewById(R.id.liste);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProjetAdapter(new ProjetAdapter.Ecouteur() {
            @Override
            public void ouvrir(Projet p) {
                ouvrirProjet(p, false, false);
            }

            @Override
            public void modifier(Projet p) {
                ouvrirProjet(p, true, false);
            }

            @Override
            public void menu(Projet p, View ancre) {
                afficherMenu(p, ancre);
            }
        });
        rv.setAdapter(adapter);

        recherche.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                rafraichir();
            }
        });

        findViewById(R.id.fab).setOnClickListener(v -> nouveauProjet());
        initialiserTiroir();
        remplirCompte();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!connecte) return;
        Sync.ajouter(syncEcouteur);
        Sync.demarrer(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Sync.retirer(syncEcouteur);
    }

    // ----- Tiroir (compte + sécurité) -----
    private void initialiserTiroir() {
        drawer = findViewById(R.id.drawer_layout);
        switchPin = findViewById(R.id.switch_pin);
        ligneModifierPin = findViewById(R.id.ligne_modifier_pin);
        ligneBio = findViewById(R.id.ligne_bio);
        switchBio = findViewById(R.id.switch_bio);
        drawerNom = findViewById(R.id.drawer_nom);
        drawerEmail = findViewById(R.id.drawer_email);
        drawerLettre = findViewById(R.id.drawer_lettre);
        drawerPhoto = findViewById(R.id.drawer_photo);

        lanceurPin = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            majPin();
            if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                String m = r.getData().getStringExtra("message");
                if (m != null) Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_compte).setOnClickListener(v -> drawer.openDrawer(GravityCompat.START));
        findViewById(R.id.ligne_pin).setOnClickListener(v -> switchPin.performClick());
        switchPin.setOnClickListener(v -> lancerPin(switchPin.isChecked() ? PinActivity.CREER : PinActivity.DESACTIVER));
        ligneModifierPin.setOnClickListener(v -> lancerPin(PinActivity.CHANGER));
        findViewById(R.id.ligne_bio).setOnClickListener(v -> switchBio.performClick());
        switchBio.setOnClickListener(v -> {
            if (switchBio.isChecked()) {
                Bio.demander(this, "Confirmez avec votre empreinte digitale", () -> {
                    Verrou.definirBio(this, true);
                    majPin();
                    Toast.makeText(this, "Empreinte digitale activée", Toast.LENGTH_SHORT).show();
                }, this::majPin);
            } else {
                Verrou.definirBio(this, false);
                majPin();
                Toast.makeText(this, "Empreinte digitale désactivée", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.ligne_deconnexion).setOnClickListener(v -> confirmerDeconnexion());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void lancerPin(int mode) {
        lanceurPin.launch(new Intent(this, PinActivity.class).putExtra("mode", mode));
    }

    private void remplirCompte() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        String email = Sync.email(this);
        String nom = u != null && u.getDisplayName() != null ? u.getDisplayName().trim() : "";
        String base = nom.isEmpty() ? (email == null ? "" : email) : nom;
        drawerNom.setText(base);
        drawerEmail.setText(email == null ? "" : email);
        drawerLettre.setText(base.isEmpty() ? "?" : base.substring(0, 1).toUpperCase(Locale.ROOT));
        if (u != null && u.getPhotoUrl() != null) {
            Photo.charger(u.getPhotoUrl().toString(), drawerPhoto, drawerLettre);
        }
        majPin();
    }

    private void majPin() {
        boolean actif = Verrou.actif(this);
        switchPin.setChecked(actif);
        ligneModifierPin.setVisibility(actif ? View.VISIBLE : View.GONE);
        ligneBio.setVisibility(actif && Bio.disponible(this) ? View.VISIBLE : View.GONE);
        switchBio.setChecked(Verrou.bioActive(this));
    }

    private void confirmerDeconnexion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Se déconnecter ?")
                .setMessage("Vos listes seront retirées de ce téléphone mais restent sauvegardées dans le cloud. Elles reviendront à la prochaine connexion.")
                .setPositiveButton("Se déconnecter", (d, w) -> {
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

    @Override
    protected void onResume() {
        super.onResume();
        if (connecte) rafraichir();
    }

    private void rafraichir() {
        String q = recherche.getText().toString().trim().toLowerCase(Locale.FRANCE);
        List<Projet> tous = Store.get(this);
        List<Projet> filtres = new ArrayList<>();
        int restants = 0;
        for (Projet p : tous) {
            restants += p.articles.size() - p.nbPris();
            if (q.isEmpty() || p.titre().toLowerCase(Locale.FRANCE).contains(q)) filtres.add(p);
        }
        Collections.sort(filtres, (a, b) -> {
            if (a.termine() != b.termine()) return a.termine() ? 1 : -1;
            return Long.compare(a.date, b.date);
        });
        adapter.setItems(filtres);

        int n = tous.size();
        resume.setText(n + " projet" + (n > 1 ? "s" : "") + " • " + restants + " article" + (restants > 1 ? "s" : "") + " à acheter");
        vide.setVisibility(filtres.isEmpty() ? View.VISIBLE : View.GONE);
        vide.setText(tous.isEmpty()
                ? "Aucun projet pour le moment.\nAppuyez sur « Nouveau projet » pour créer votre première liste de courses."
                : "Aucun projet ne correspond à votre recherche.");
    }

    private void nouveauProjet() {
        Projet p = new Projet();
        p.date = Utils.debutJour(System.currentTimeMillis()) + 12L * 3600 * 1000;
        Store.get(this).add(0, p);
        Store.sauver(this);
        ouvrirProjet(p, true, true);
    }

    private void ouvrirProjet(Projet p, boolean edition, boolean nouveau) {
        Intent i = new Intent(this, ProjetActivity.class);
        i.putExtra("id", p.id);
        i.putExtra("edition", edition);
        i.putExtra("nouveau", nouveau);
        startActivity(i);
    }

    private void afficherMenu(final Projet p, View ancre) {
        PopupMenu pm = new PopupMenu(this, ancre);
        pm.getMenu().add(0, 1, 0, "Dupliquer");
        pm.getMenu().add(0, 2, 1, "Partager");
        pm.getMenu().add(0, 3, 2, "Supprimer");
        pm.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                Store.get(this).add(0, p.copie());
                Store.sauver(this);
                rafraichir();
            } else if (id == 2) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, Utils.texteProjet(p));
                Verrou.sortieVolontaire = true;
                startActivity(Intent.createChooser(i, "Partager la liste"));
            } else if (id == 3) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Supprimer le projet ?")
                        .setMessage("« " + p.titre() + " » sera définitivement supprimé.")
                        .setPositiveButton("Supprimer", (d, w) -> {
                            Store.get(this).remove(p);
                            Store.sauver(this);
                            rafraichir();
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
            }
            return true;
        });
        pm.show();
    }
}
