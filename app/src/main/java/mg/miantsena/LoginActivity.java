package mg.miantsena;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private static final String TEL = "0348017559";
    private static final String MAIL = "sitraka0417@gmail.com";
    private static final String FB_PROFIL = "https://www.facebook.com/sitraka.nambinintsoa.739981";
    private static final String FB_MESSENGER = "https://m.me/sitraka.nambinintsoa.739981";

    private GoogleSignInClient client;
    private ActivityResultLauncher<Intent> lanceur;
    private View btnGoogle;
    private ProgressBar progression;
    private TextView message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnGoogle = findViewById(R.id.btn_google);
        progression = findViewById(R.id.progression);
        message = findViewById(R.id.message);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        client = GoogleSignIn.getClient(this, gso);

        lanceur = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            try {
                GoogleSignInAccount compte = GoogleSignIn.getSignedInAccountFromIntent(r.getData())
                        .getResult(ApiException.class);
                connecterFirebase(compte.getIdToken());
            } catch (ApiException e) {
                occupe(false);
                if (e.getStatusCode() != 12501) { // 12501 = annulé par l'utilisateur
                    erreur(getString(R.string.login_echec) + " (code " + e.getStatusCode() + ")");
                }
            }
        });

        btnGoogle.setOnClickListener(v -> {
            occupe(true);
            message.setText(R.string.login_info);
            message.setTextColor(0xCCFFFFFF);
            client.signOut().addOnCompleteListener(t -> lanceur.launch(client.getSignInIntent()));
        });

        findViewById(R.id.ct_tel).setOnClickListener(v -> appeler());
        findViewById(R.id.ct_mail).setOnClickListener(v -> ecrireMail());
        findViewById(R.id.ct_facebook).setOnClickListener(v -> ouvrirFacebook());
    }

    // ----- Connexion -----
    private void connecterFirebase(String idToken) {
        AuthCredential cred = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseAuth.getInstance().signInWithCredential(cred)
                .addOnSuccessListener(res -> verifier(res.getUser()))
                .addOnFailureListener(e -> {
                    occupe(false);
                    erreur(getString(R.string.login_echec));
                });
    }

    /** Seuls les comptes présents dans la collection Firestore « autorises » peuvent entrer. */
    private void verifier(FirebaseUser u) {
        final String email = u == null || u.getEmail() == null ? "" : u.getEmail().toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            refuser(getString(R.string.login_refuse));
            return;
        }
        FirebaseFirestore.getInstance().collection("autorises").document(email).get()
                .addOnSuccessListener(d -> {
                    if (d.exists()) {
                        Sync.marquerAutorise(this, email);
                        Verrou.deverrouiller();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        refuser(getString(R.string.login_refuse) + "\n" + email);
                    }
                })
                .addOnFailureListener(e -> refuser(getString(R.string.login_reseau)));
    }

    private void refuser(String texte) {
        Sync.annulerConnexion(this);
        occupe(false);
        erreur(texte);
    }

    private void erreur(String texte) {
        message.setText(texte);
        message.setTextColor(0xFFFFB4B4);
    }

    private void occupe(boolean oui) {
        btnGoogle.setEnabled(!oui);
        btnGoogle.setAlpha(oui ? 0.6f : 1f);
        progression.setVisibility(oui ? View.VISIBLE : View.GONE);
    }

    // ----- Contacts cliquables -----
    private void appeler() {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + TEL)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, TEL, Toast.LENGTH_LONG).show();
        }
    }

    private void ecrireMail() {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + MAIL));
        i.putExtra(Intent.EXTRA_SUBJECT, "Miantsena - fangatahana alalana hiditra");
        i.setPackage("com.google.android.gm");
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            i.setPackage(null);
            try {
                startActivity(i);
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(this, MAIL, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void ouvrirFacebook() {
        for (String pkg : new String[]{"com.facebook.orca", "com.facebook.mlite"}) { // Messenger, puis Messenger Lite
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(FB_MESSENGER));
                i.setPackage(pkg);
                startActivity(i);
                return;
            } catch (ActivityNotFoundException ignored) {
            }
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(FB_PROFIL))); // navigateur
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, FB_PROFIL, Toast.LENGTH_LONG).show();
        }
    }
}
