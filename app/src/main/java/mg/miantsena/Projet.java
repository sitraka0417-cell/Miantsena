package mg.miantsena;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Projet {
    public String id = UUID.randomUUID().toString();
    public String nom = "";
    public long date = System.currentTimeMillis();
    public List<Article> articles = new ArrayList<>();

    public int nbPris() {
        int n = 0;
        for (Article a : articles) if (a.pris) n++;
        return n;
    }

    public double total() {
        double t = 0;
        for (Article a : articles) t += a.total();
        return t;
    }

    public double totalPris() {
        double t = 0;
        for (Article a : articles) if (a.pris) t += a.total();
        return t;
    }

    public int nbSansPrix() {
        int n = 0;
        for (Article a : articles) if (!a.aPrix()) n++;
        return n;
    }

    public boolean aApprox() {
        for (Article a : articles) if (a.aPrix() && a.approx) return true;
        return false;
    }

    public boolean termine() {
        return !articles.isEmpty() && nbPris() == articles.size();
    }

    public boolean enRetard() {
        return !termine() && date < Utils.debutJour(System.currentTimeMillis());
    }

    public String titre() {
        return nom.trim().isEmpty() ? "Sans titre" : nom.trim();
    }

    public Projet copie() {
        Projet p = new Projet();
        p.nom = titre() + " (copie)";
        p.date = date;
        for (Article a : articles) {
            Article b = new Article();
            b.nom = a.nom;
            b.qte = a.qte;
            b.unite = a.unite;
            b.prix = a.prix;
            b.approx = a.approx;
            b.pris = false;
            p.articles.add(b);
        }
        return p;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("nom", nom);
        o.put("date", date);
        JSONArray arr = new JSONArray();
        for (Article a : articles) arr.put(a.toJson());
        o.put("articles", arr);
        return o;
    }

    public static Projet fromJson(JSONObject o) {
        Projet p = new Projet();
        p.id = o.optString("id", p.id);
        p.nom = o.optString("nom", "");
        p.date = o.optLong("date", System.currentTimeMillis());
        JSONArray arr = o.optJSONArray("articles");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject ao = arr.optJSONObject(i);
                if (ao != null) p.articles.add(Article.fromJson(ao));
            }
        }
        return p;
    }
}
