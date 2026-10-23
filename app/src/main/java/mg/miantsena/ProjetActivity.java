package mg.miantsena;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProjetActivity extends AppCompatActivity implements ArticleAdapter.Ecouteur {

    private Projet projet;
    private boolean edition, nouveau;
    private ArticleAdapter adapter;
    private EditText champNom;
    private TextView btnDate, titre, sousTitre, totalTv, detailTv, infoTv, vide;
    private View carteInfos;
    private ProgressBar progression;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projet);

        projet = Store.parId(this, getIntent().getStringExtra("id"));
        if (projet == null) {
            finish();
            return;
        }
        edition = getIntent().getBooleanExtra("edition", false);
        nouveau = getIntent().getBooleanExtra("nouveau", false);

        champNom = findViewById(R.id.champ_nom);
        btnDate = findViewById(R.id.btn_date);
        titre = findViewById(R.id.titre);
        sousTitre = findViewById(R.id.sous_titre);
        totalTv = findViewById(R.id.total);
        detailTv = findViewById(R.id.detail);
        infoTv = findViewById(R.id.info);
        vide = findViewById(R.id.vide);
        carteInfos = findViewById(R.id.carte_infos);
        progression = findViewById(R.id.progression);

        champNom.setText(projet.nom);
        champNom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                projet.nom = s.toString();
            }
        });

        RecyclerView rv = findViewById(R.id.liste);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ArticleAdapter(projet.articles, this);
        rv.setAdapter(adapter);

        findViewById(R.id.btn_retour).setOnClickListener(v -> finish());
        findViewById(R.id.btn_menu).setOnClickListener(this::menu);
        btnDate.setOnClickListener(v -> choisirDate());
        findViewById(R.id.btn_ajouter).setOnClickListener(v -> dialogArticle(null));

        appliquerMode();
    }

    private void appliquerMode() {
        carteInfos.setVisibility(edition ? View.VISIBLE : View.GONE);
        btnDate.setText(Utils.date(projet.date));
        if (edition) {
            titre.setText("Modifier le projet");
            sousTitre.setVisibility(View.GONE);
        } else {
            titre.setText(projet.titre());
            sousTitre.setText(Utils.date(projet.date));
            sousTitre.setVisibility(View.VISIBLE);
        }
        adapter.setEdition(edition);
        maj();
    }

    private void maj() {
        double total = projet.total();
        double pris = projet.totalPris();
        String approx = projet.aApprox() ? "≈ " : "";
        totalTv.setText(approx + Utils.argent(total));
        detailTv.setText("Déjà pris : " + Utils.argent(pris) + "   •   Reste : " + Utils.argent(Math.max(0, total - pris)));
        int n = projet.articles.size();
        int sans = projet.nbSansPrix();
        String info = projet.nbPris() + "/" + n + " article" + (n > 1 ? "s" : "") + " pris";
        if (sans > 0) info += " • " + sans + " sans prix (non compté" + (sans > 1 ? "s" : "") + ")";
        if (projet.aApprox()) info += " • total approximatif";
        infoTv.setText(info);
        progression.setMax(Math.max(n, 1));
        progression.setProgress(projet.nbPris());
        vide.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
    }

    // ----- Ecouteur ArticleAdapter -----
    @Override
    public void change() {
        Collections.sort(projet.articles, (a, b) -> Boolean.compare(a.pris, b.pris));
        adapter.notifyDataSetChanged();
        maj();
        Store.sauver(this);
    }

    @Override
    public void editer(Article a) {
        dialogArticle(a);
    }

    @Override
    public void supprimer(final Article a) {
        final int index = projet.articles.indexOf(a);
        if (index < 0) return;
        projet.articles.remove(index);
        adapter.notifyDataSetChanged();
        maj();
        Store.sauver(this);
        Snackbar.make(findViewById(R.id.liste), "Article supprimé", Snackbar.LENGTH_LONG)
                .setAction("Annuler", v -> {
                    projet.articles.add(Math.min(index, projet.articles.size()), a);
                    adapter.notifyDataSetChanged();
                    maj();
                    Store.sauver(this);
                }).show();
    }

    // ----- Date -----
    private void choisirDate() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(projet.date);
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar n = Calendar.getInstance();
            n.set(y, m, d, 12, 0, 0);
            projet.date = n.getTimeInMillis();
            btnDate.setText(Utils.date(projet.date));
            Store.sauver(this);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ----- Menu -----
    private void menu(View ancre) {
        PopupMenu pm = new PopupMenu(this, ancre);
        pm.getMenu().add(0, 1, 0, edition ? "Passer en mode courses" : "Modifier le projet");
        pm.getMenu().add(0, 2, 1, "Tout cocher");
        pm.getMenu().add(0, 3, 2, "Tout décocher");
        pm.getMenu().add(0, 4, 3, "Partager la liste");
        pm.getMenu().add(0, 5, 4, "Supprimer le projet");
        pm.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                edition = !edition;
                appliquerMode();
            } else if (id == 2 || id == 3) {
                for (Article a : projet.articles) a.pris = (id == 2);
                change();
            } else if (id == 4) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT, Utils.texteProjet(projet));
                startActivity(Intent.createChooser(i, "Partager la liste"));
            } else if (id == 5) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Supprimer le projet ?")
                        .setMessage("« " + projet.titre() + " » sera définitivement supprimé.")
                        .setPositiveButton("Supprimer", (d, w) -> {
                            Store.get(this).remove(projet);
                            Store.sauver(this);
                            finish();
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
            }
            return true;
        });
        pm.show();
    }

    // ----- Dialogue article -----
    private void dialogArticle(final Article existant) {
        View v = getLayoutInflater().inflate(R.layout.dialog_article, null);
        final AutoCompleteTextView nom = v.findViewById(R.id.d_nom);
        final EditText qte = v.findViewById(R.id.d_qte);
        final EditText prix = v.findViewById(R.id.d_prix);
        final CheckBox approx = v.findViewById(R.id.d_approx);

        // Suggestions : articles déjà utilisés + dernier prix connu
        final Map<String, Article> connus = new LinkedHashMap<>();
        for (Projet p : Store.get(this)) {
            for (Article a : p.articles) {
                if (!a.nom.trim().isEmpty()) connus.put(a.nom.trim().toLowerCase(), a);
            }
        }
        List<String> noms = new ArrayList<>();
        for (Article a : connus.values()) noms.add(a.nom);
        String[] defauts = {"Riz", "Huile", "Sucre", "Sel", "Pain", "Lait", "Œufs", "Savon", "Tomates", "Oignons", "Pommes de terre", "Café", "Eau"};
        for (String d : defauts) if (!connus.containsKey(d.toLowerCase())) noms.add(d);
        nom.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, noms));
        nom.setOnItemClickListener((parent, view, pos, id) -> {
            Article k = connus.get(String.valueOf(parent.getItemAtPosition(pos)).trim().toLowerCase());
            if (k != null && prix.getText().toString().trim().isEmpty() && k.aPrix()) {
                prix.setText(formaterPrix(k.prix));
                approx.setChecked(k.approx);
            }
        });

        if (existant != null) {
            nom.setText(existant.nom);
            qte.setText(String.valueOf(existant.qte));
            if (existant.aPrix()) prix.setText(formaterPrix(existant.prix));
            approx.setChecked(existant.approx);
        } else {
            qte.setText("1");
        }

        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(this)
                .setTitle(existant == null ? "Ajouter un article" : "Modifier l’article")
                .setView(v)
                .setPositiveButton(existant == null ? "Ajouter" : "Enregistrer", null)
                .setNegativeButton("Annuler", null);
        if (existant == null) b.setNeutralButton("Ajouter et suivant", null);
        final AlertDialog d = b.create();

        d.setOnShowListener(x -> {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(bt -> {
                if (enregistrerArticle(existant, nom, qte, prix, approx)) d.dismiss();
            });
            if (existant == null) {
                d.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(bt -> {
                    if (enregistrerArticle(null, nom, qte, prix, approx)) {
                        nom.setText("");
                        qte.setText("1");
                        prix.setText("");
                        approx.setChecked(false);
                        nom.requestFocus();
                    }
                });
            }
        });
        d.show();
        nom.requestFocus();
    }

    private boolean enregistrerArticle(Article existant, EditText nom, EditText qte, EditText prix, CheckBox approx) {
        String n = nom.getText().toString().trim();
        if (n.isEmpty()) {
            nom.setError("Entrez le nom de l’article");
            return false;
        }
        int q = 1;
        try {
            q = Math.max(1, Integer.parseInt(qte.getText().toString().trim()));
        } catch (NumberFormatException ignored) {
        }
        double p = -1;
        String ps = prix.getText().toString().trim().replace(',', '.').replace(" ", "");
        if (!ps.isEmpty()) {
            try {
                p = Double.parseDouble(ps);
                if (p < 0) p = -1;
            } catch (NumberFormatException e) {
                prix.setError("Prix invalide");
                return false;
            }
        }
        Article a = existant != null ? existant : new Article();
        a.nom = n;
        a.qte = q;
        a.prix = p;
        a.approx = p >= 0 && approx.isChecked();
        if (existant == null) projet.articles.add(0, a);
        change();
        return true;
    }

    private String formaterPrix(double p) {
        if (Math.abs(p - Math.rint(p)) < 0.005) return String.valueOf((long) Math.rint(p));
        return String.valueOf(p);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (projet == null) return;
        if (isFinishing() && nouveau && projet.articles.isEmpty() && projet.nom.trim().isEmpty()) {
            Store.get(this).remove(projet);
        }
        Store.sauver(this);
    }
}
