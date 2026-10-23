package mg.miantsena;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProjetAdapter extends RecyclerView.Adapter<ProjetAdapter.VH> {

    public interface Ecouteur {
        void ouvrir(Projet p);
        void modifier(Projet p);
        void menu(Projet p, View ancre);
    }

    private final List<Projet> items = new ArrayList<>();
    private final Ecouteur ec;

    public ProjetAdapter(Ecouteur ec) {
        this.ec = ec;
    }

    public void setItems(List<Projet> l) {
        items.clear();
        items.addAll(l);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_projet, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final Projet p = items.get(position);
        int n = p.articles.size();
        h.nom.setText(p.titre());
        h.date.setText(Utils.date(p.date));
        h.retard.setVisibility(p.enRetard() ? View.VISIBLE : View.GONE);
        h.fini.setVisibility(p.termine() ? View.VISIBLE : View.GONE);
        h.progression.setMax(Math.max(n, 1));
        h.progression.setProgress(p.nbPris());
        h.compte.setText(p.nbPris() + "/" + n + " article" + (n > 1 ? "s" : "") + " pris");
        h.total.setText((p.aApprox() ? "≈ " : "") + Utils.argent(p.total()));
        h.itemView.setOnClickListener(v -> ec.ouvrir(p));
        h.btnOuvrir.setOnClickListener(v -> ec.ouvrir(p));
        h.btnModifier.setOnClickListener(v -> ec.modifier(p));
        h.menu.setOnClickListener(v -> ec.menu(p, v));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView nom, date, retard, fini, compte, total, btnModifier, btnOuvrir;
        ProgressBar progression;
        View menu;

        VH(View v) {
            super(v);
            nom = v.findViewById(R.id.nom);
            date = v.findViewById(R.id.date);
            retard = v.findViewById(R.id.badge_retard);
            fini = v.findViewById(R.id.badge_fini);
            compte = v.findViewById(R.id.compte);
            total = v.findViewById(R.id.total);
            btnModifier = v.findViewById(R.id.btn_modifier);
            btnOuvrir = v.findViewById(R.id.btn_ouvrir);
            progression = v.findViewById(R.id.progression);
            menu = v.findViewById(R.id.menu);
        }
    }
}
