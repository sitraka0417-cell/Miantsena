package mg.miantsena;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.VH> {

    public interface Ecouteur {
        void change();
        void editer(Article a);
        void supprimer(Article a);
    }

    private final List<Article> items;
    private final Ecouteur ec;
    private boolean edition;

    public ArticleAdapter(List<Article> items, Ecouteur ec) {
        this.items = items;
        this.ec = ec;
    }

    public void setEdition(boolean e) {
        edition = e;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final Article a = items.get(position);
        h.coche.setOnCheckedChangeListener(null);
        h.coche.setChecked(a.pris);
        h.nom.setText(a.nom);
        if (a.pris) h.nom.setPaintFlags(h.nom.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        else h.nom.setPaintFlags(h.nom.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        float alpha = a.pris ? 0.5f : 1f;
        h.nom.setAlpha(alpha);
        h.detail.setAlpha(alpha);
        h.sousTotal.setAlpha(alpha);

        if (a.aPrix()) {
            h.detail.setText(Utils.qteTexte(a) + " × " + (a.approx ? "≈ " : "") + Utils.argent(a.prix));
            h.sousTotal.setText((a.approx ? "≈ " : "") + Utils.argent(a.total()));
        } else {
            h.detail.setText(Utils.qteTexte(a) + " • prix non renseigné");
            h.sousTotal.setText("—");
        }

        h.btnEdit.setVisibility(edition ? View.VISIBLE : View.GONE);
        h.btnSuppr.setVisibility(edition ? View.VISIBLE : View.GONE);

        h.coche.setOnCheckedChangeListener((b, checked) -> {
            a.pris = checked;
            h.itemView.post(ec::change);
        });
        h.itemView.setOnClickListener(v -> {
            if (edition) ec.editer(a);
            else h.coche.performClick();
        });
        h.btnEdit.setOnClickListener(v -> ec.editer(a));
        h.btnSuppr.setOnClickListener(v -> ec.supprimer(a));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox coche;
        TextView nom, detail, sousTotal;
        View btnEdit, btnSuppr;

        VH(View v) {
            super(v);
            coche = v.findViewById(R.id.coche);
            nom = v.findViewById(R.id.nom);
            detail = v.findViewById(R.id.detail);
            sousTotal = v.findViewById(R.id.sous_total);
            btnEdit = v.findViewById(R.id.btn_edit);
            btnSuppr = v.findViewById(R.id.btn_suppr);
        }
    }
}
