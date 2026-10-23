package mg.miantsena;

import org.json.JSONException;
import org.json.JSONObject;

public class Article {
    public String nom = "";
    public double qte = 1;        // peut être décimal (0,5)
    public String unite = "pce";  // pce, kg, L, m, paquet...
    public double prix = -1;      // -1 = prix non renseigné
    public boolean approx = false; // prix approximatif
    public boolean pris = false;   // déjà acheté

    public boolean aPrix() { return prix >= 0; }

    public double total() { return aPrix() ? prix * qte : 0; }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("nom", nom);
        o.put("qte", qte);
        o.put("unite", unite);
        o.put("prix", prix);
        o.put("approx", approx);
        o.put("pris", pris);
        return o;
    }

    public static Article fromJson(JSONObject o) {
        Article a = new Article();
        a.nom = o.optString("nom", "");
        a.qte = o.optDouble("qte", 1);
        if (a.qte <= 0) a.qte = 1;
        a.unite = o.optString("unite", "pce");
        a.prix = o.optDouble("prix", -1);
        a.approx = o.optBoolean("approx", false);
        a.pris = o.optBoolean("pris", false);
        return a;
    }
}
