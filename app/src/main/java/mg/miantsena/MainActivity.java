package mg.miantsena;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ProjetAdapter adapter;
    private TextView vide, resume;
    private EditText recherche;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        rafraichir();
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
