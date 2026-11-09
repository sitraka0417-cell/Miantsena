package mg.miantsena;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Code PIN par utilisateur (compte Google), stocké haché (PBKDF2) sur l'appareil. */
public class Verrou {

    /** Vrai au démarrage et dès que l'appli passe en arrière-plan. */
    public static boolean verrouille = true;
    /** À mettre à vrai avant d'ouvrir volontairement une autre appli (partage...) pour ne pas reverrouiller. */
    public static boolean sortieVolontaire = false;
    public static boolean pinAffiche = false;

    public static final int LONGUEUR = 4;
    private static final int ESSAIS_MAX = 5;

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("miantsena_pin", Context.MODE_PRIVATE);
    }

    public static boolean actif(Context c) {
        String e = Sync.email(c);
        return e != null && p(c).contains("pin_" + e);
    }

    public static boolean bioActive(Context c) {
        String e = Sync.email(c);
        return e != null && p(c).contains("pin_" + e) && p(c).getBoolean("bio_" + e, false);
    }

    public static void definirBio(Context c, boolean oui) {
        String e = Sync.email(c);
        if (e == null) return;
        p(c).edit().putBoolean("bio_" + e, oui).apply();
    }

    public static boolean requis(Context c) {
        return verrouille && Sync.estConnecte(c) && actif(c);
    }

    public static void deverrouiller() {
        verrouille = false;
    }

    public static void definir(Context c, String pin) {
        String e = Sync.email(c);
        if (e == null) return;
        try {
            byte[] sel = new byte[16];
            new SecureRandom().nextBytes(sel);
            String v = Base64.encodeToString(sel, Base64.NO_WRAP) + ":" + Base64.encodeToString(hash(pin, sel), Base64.NO_WRAP);
            p(c).edit().putString("pin_" + e, v).remove("echecs_" + e).remove("bloque_" + e).apply();
        } catch (Exception ignored) {
        }
    }

    public static boolean verifier(Context c, String pin) {
        String e = Sync.email(c);
        if (e == null) return false;
        String v = p(c).getString("pin_" + e, null);
        if (v == null) return false;
        try {
            String[] t = v.split(":");
            byte[] sel = Base64.decode(t[0], Base64.NO_WRAP);
            byte[] attendu = Base64.decode(t[1], Base64.NO_WRAP);
            return MessageDigest.isEqual(attendu, hash(pin, sel));
        } catch (Exception ex) {
            return false;
        }
    }

    public static void supprimer(Context c) {
        supprimerPour(c, Sync.email(c));
    }

    public static void supprimerPour(Context c, String email) {
        if (email == null) return;
        p(c).edit().remove("pin_" + email).remove("bio_" + email).remove("echecs_" + email).remove("bloque_" + email).apply();
    }

    // ----- Protection contre les essais répétés -----
    public static long tempsBloque(Context c) {
        String e = Sync.email(c);
        if (e == null) return 0;
        return Math.max(0, p(c).getLong("bloque_" + e, 0) - System.currentTimeMillis());
    }

    /** Retourne le nombre d'essais restants avant blocage (0 = bloqué maintenant). */
    public static int enregistrerEchec(Context c) {
        String e = Sync.email(c);
        if (e == null) return 0;
        int n = p(c).getInt("echecs_" + e, 0) + 1;
        SharedPreferences.Editor ed = p(c).edit().putInt("echecs_" + e, n);
        int restants = ESSAIS_MAX - (n % ESSAIS_MAX);
        if (n % ESSAIS_MAX == 0) {
            long duree = Math.min(30000L << Math.min(n / ESSAIS_MAX - 1, 4), 300000L); // 30 s, 1 min, 2 min, 4 min, 5 min
            ed.putLong("bloque_" + e, System.currentTimeMillis() + duree);
            restants = 0;
        }
        ed.apply();
        return restants;
    }

    public static void reinitialiserEchecs(Context c) {
        String e = Sync.email(c);
        if (e == null) return;
        p(c).edit().remove("echecs_" + e).remove("bloque_" + e).apply();
    }

    private static byte[] hash(String pin, byte[] sel) throws Exception {
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return f.generateSecret(new PBEKeySpec(pin.toCharArray(), sel, 20000, 160)).getEncoded();
    }
}
