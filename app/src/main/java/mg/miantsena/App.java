package mg.miantsena;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

/** Verrouille l'appli (code PIN) à chaque ouverture, dès qu'elle est passée en arrière-plan. */
public class App extends Application implements Application.ActivityLifecycleCallbacks {

    private int demarrees = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityStarted(Activity a) {
        demarrees++;
        if (demarrees == 1) Verrou.sortieVolontaire = false;
        if (a instanceof PinActivity || a instanceof LoginActivity) return;
        if (Verrou.requis(this) && !Verrou.pinAffiche) {
            Verrou.pinAffiche = true;
            Intent i = new Intent(a, PinActivity.class);
            i.putExtra("mode", PinActivity.VERIFIER);
            a.startActivity(i);
            a.overridePendingTransition(0, 0);
        }
    }

    @Override
    public void onActivityStopped(Activity a) {
        demarrees--;
        if (demarrees <= 0 && !a.isChangingConfigurations() && !Verrou.sortieVolontaire) {
            Verrou.verrouille = true;
        }
    }

    @Override
    public void onActivityCreated(Activity a, Bundle b) {
    }

    @Override
    public void onActivityResumed(Activity a) {
    }

    @Override
    public void onActivityPaused(Activity a) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity a, Bundle b) {
    }

    @Override
    public void onActivityDestroyed(Activity a) {
    }
}
