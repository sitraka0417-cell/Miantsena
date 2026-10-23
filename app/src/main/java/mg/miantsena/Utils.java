package mg.miantsena;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utils {

    public static String argent(double v) {
        String s;
        if (Math.abs(v - Math.rint(v)) < 0.005) {
            s = String.format(Locale.FRANCE, "%,.0f", v);
        } else {
            s = String.format(Locale.FRANCE, "%,.2f", v);
        }
        return s.replace('\u202f', ' ').replace('\u00a0', ' ') + " Ar";
    }

    public static String date(long ms) {
        String s = new SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE).format(new Date(ms));
        return s.substring(0, 1).toUpperCase(Locale.FRANCE) + s.substring(1);
    }

    public static long debutJour(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static String texteProjet(Projet p) {
        StringBuilder sb = new StringBuilder();
        sb.append("🛒 ").append(p.titre()).append("\n📅 ").append(date(p.date)).append("\n\n");
        for (Article a : p.articles) {
            sb.append(a.pris ? "☑ " : "☐ ").append(a.nom);
            if (a.qte > 1) sb.append(" ×").append(a.qte);
            if (a.aPrix()) sb.append(" — ").append(a.approx ? "≈ " : "").append(argent(a.total()));
            sb.append("\n");
        }
        sb.append("\nTotal : ").append(p.aApprox() ? "≈ " : "").append(argent(p.total()));
        return sb.toString();
    }
}
