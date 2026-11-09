package mg.miantsena;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Synchronisation automatique des projets avec Firestore :
 * utilisateurs/{uid}/projets/{idProjet} -> { json, maj }
 * Tous les appels se font depuis le thread principal.
 */
public class Sync {

    public interface Ecouteur {
        default void donneesChangees() {
        }

        default void accesRefuse() {
        }
    }

    private static final List<Ecouteur> ecouteurs = new CopyOnWriteArrayList<>();
    private static final Map<String, String> empreintes = new HashMap<>();
    private static final Set<String> aPousser = new LinkedHashSet<>();
    private static final Set<String> aSupprimer = new LinkedHashSet<>();
    private static ListenerRegistration inscription;
    private static boolean actif = false;
    private static String uid;
    private static Context app;

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("miantsena_sync", Context.MODE_PRIVATE);
    }

    // ----- Compte -----
    public static String email(Context c) {
        return prefs(c).getString("autorise_email", null);
    }

    public static boolean estConnecte(Context c) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || u.getEmail() == null) return false;
        return u.getEmail().toLowerCase(Locale.ROOT).equals(prefs(c).getString("autorise_email", ""));
    }

    public static void marquerAutorise(Context c, String email) {
        prefs(c).edit().putString("autorise_email", email.toLowerCase(Locale.ROOT)).apply();
    }

    public static void ajouter(Ecouteur e) {
        if (!ecouteurs.contains(e)) ecouteurs.add(e);
    }

    public static void retirer(Ecouteur e) {
        ecouteurs.remove(e);
    }

    /** Connexion refusée : on se déconnecte SANS toucher aux données locales. */
    public static void annulerConnexion(Context c) {
        Context a = c.getApplicationContext();
        FirebaseAuth.getInstance().signOut();
        GoogleSignIn.getClient(a, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
    }

    /** Déconnexion complète : les listes locales sont effacées (elles restent dans le cloud). */
    public static void deconnecter(Context c) {
        Context a = c.getApplicationContext();
        if (inscription != null) {
            inscription.remove();
            inscription = null;
        }
        actif = false;
        empreintes.clear();
        aPousser.clear();
        aSupprimer.clear();
        prefs(a).edit().clear().apply();
        FirebaseAuth.getInstance().signOut();
        GoogleSignIn.getClient(a, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
        Store.vider(a);
    }

    // ----- Démarrage -----
    public static void demarrer(Context c) {
        if (actif || !estConnecte(c)) return;
        app = c.getApplicationContext();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        empreintes.clear();
        for (Projet p : Store.get(app)) {
            try {
                empreintes.put(p.id, p.toJson(false).toString());
            } catch (JSONException ignored) {
            }
        }
        actif = true;
        inscription = collection().addSnapshotListener((snap, err) -> recu(snap, err));
    }

    private static CollectionReference collection() {
        return FirebaseFirestore.getInstance().collection("utilisateurs").document(uid).collection("projets");
    }

    // ----- Local -> cloud -----
    /** Appelé avant chaque sauvegarde locale : repère ce qui a changé. */
    public static void preparer(List<Projet> liste) {
        if (!actif) return;
        Set<String> ids = new HashSet<>();
        for (Projet p : liste) {
            ids.add(p.id);
            String e;
            try {
                e = p.toJson(false).toString();
            } catch (JSONException ex) {
                continue;
            }
            if (!e.equals(empreintes.get(p.id))) {
                p.maj = System.currentTimeMillis();
                empreintes.put(p.id, e);
                aPousser.add(p.id);
            }
        }
        for (String id : new ArrayList<>(empreintes.keySet())) {
            if (!ids.contains(id)) {
                empreintes.remove(id);
                aPousser.remove(id);
                aSupprimer.add(id);
            }
        }
    }

    /** Envoie les changements (Firestore les garde en file si on est hors ligne). */
    public static void envoyer(Context c) {
        if (!actif) return;
        CollectionReference col = collection();
        boolean change = false;
        for (String id : new ArrayList<>(aPousser)) {
            Projet p = Store.parId(c, id);
            if (p == null) continue;
            if (p.maj == 0) {
                p.maj = System.currentTimeMillis();
                change = true;
            }
            Map<String, Object> m = new HashMap<>();
            try {
                m.put("json", p.toJson(false).toString());
            } catch (JSONException e) {
                continue;
            }
            m.put("maj", p.maj);
            col.document(id).set(m);
        }
        aPousser.clear();
        for (String id : aSupprimer) col.document(id).delete();
        aSupprimer.clear();
        if (change) Store.sauverLocal(c);
    }

    // ----- Cloud -> local -----
    private static void recu(QuerySnapshot snap, FirebaseFirestoreException err) {
        if (err != null) {
            if (err.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                deconnecter(app);
                for (Ecouteur e : ecouteurs) e.accesRefuse();
            }
            return;
        }
        if (snap == null || !actif) return;

        List<Projet> locaux = Store.get(app);
        Set<String> synced = new HashSet<>(prefs(app).getStringSet("synced", new HashSet<>()));
        Set<String> cloud = new HashSet<>();
        boolean modif = false;

        for (DocumentSnapshot d : snap.getDocuments()) {
            String id = d.getId();
            cloud.add(id);
            String json = d.getString("json");
            if (json == null) continue;
            Long majD = d.getLong("maj");
            long maj = majD == null ? 0 : majD;
            Projet loc = null;
            for (Projet p : locaux) if (p.id.equals(id)) loc = p;
            if (loc != null && loc.maj >= maj) {
                if (loc.maj > maj) aPousser.add(id); // local plus récent
                continue;
            }
            Projet dist;
            try {
                dist = Projet.fromJson(new JSONObject(json));
                dist.id = id;
                dist.maj = maj;
                empreintes.put(id, dist.toJson(false).toString());
            } catch (JSONException e) {
                continue;
            }
            if (loc == null) locaux.add(dist);
            else loc.copierDepuis(dist);
            modif = true;
        }

        if (!snap.getMetadata().isFromCache()) {
            // suppressions faites sur un autre appareil
            for (Iterator<Projet> it = locaux.iterator(); it.hasNext(); ) {
                Projet p = it.next();
                if (cloud.contains(p.id)) continue;
                if (synced.contains(p.id)) {
                    it.remove();
                    empreintes.remove(p.id);
                    aPousser.remove(p.id);
                    modif = true;
                } else {
                    aPousser.add(p.id); // jamais envoyé : on l'envoie
                }
            }
            synced = new HashSet<>(cloud);
        } else {
            synced.addAll(cloud);
        }
        prefs(app).edit().putStringSet("synced", synced).apply();

        if (modif) {
            Store.sauverLocal(app);
            for (Ecouteur e : ecouteurs) e.donneesChangees();
        }
        envoyer(app);
    }
}
