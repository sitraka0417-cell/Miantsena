package mg.miantsena;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Chargement très simple de la photo du compte Google (sans bibliothèque externe). */
public class Photo {
    private static String urlCache;
    private static Bitmap bitmapCache;

    public static void charger(final String url, final ImageView vue, final View lettre) {
        if (url == null) return;
        if (url.equals(urlCache) && bitmapCache != null) {
            afficher(bitmapCache, vue, lettre);
            return;
        }
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                try (InputStream in = c.getInputStream()) {
                    final Bitmap b = BitmapFactory.decodeStream(in);
                    if (b == null) return;
                    urlCache = url;
                    bitmapCache = b;
                    vue.post(() -> afficher(b, vue, lettre));
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private static void afficher(Bitmap b, ImageView vue, View lettre) {
        vue.setImageBitmap(b);
        vue.setVisibility(View.VISIBLE);
        lettre.setVisibility(View.GONE);
    }
}
