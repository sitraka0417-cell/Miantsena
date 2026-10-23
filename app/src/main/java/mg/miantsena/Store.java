package mg.miantsena;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class Store {
    private static List<Projet> liste;

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("miantsena", Context.MODE_PRIVATE);
    }

    public static synchronized List<Projet> get(Context c) {
        if (liste == null) {
            liste = new ArrayList<>();
            try {
                JSONArray arr = new JSONArray(prefs(c).getString("projets", "[]"));
                for (int i = 0; i < arr.length(); i++) {
                    liste.add(Projet.fromJson(arr.getJSONObject(i)));
                }
            } catch (JSONException ignored) {
            }
        }
        return liste;
    }

    public static synchronized void sauver(Context c) {
        List<Projet> l = get(c);
        JSONArray arr = new JSONArray();
        try {
            for (Projet p : l) arr.put(p.toJson());
        } catch (JSONException e) {
            return;
        }
        prefs(c).edit().putString("projets", arr.toString()).apply();
    }

    public static Projet parId(Context c, String id) {
        if (id == null) return null;
        for (Projet p : get(c)) if (p.id.equals(id)) return p;
        return null;
    }
}
