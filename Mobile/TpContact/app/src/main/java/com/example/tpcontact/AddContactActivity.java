package com.example.tpcontact;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.tpcontact.api.ContactApi;
import com.example.tpcontact.bean.Contact;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddContactActivity extends AppCompatActivity {

    private TextInputEditText editTextName;
    private TextInputEditText editTextPhone;
    private MaterialButton btnSave;
    private ContactApi contactApi;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Initialize views
        editTextName = findViewById(R.id.editTextName);
        editTextPhone = findViewById(R.id.editTextPhone);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Setup back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(view -> onBackPressed());

        // Initialize Retrofit
        contactApi = RetrofitClient.getClient().create(ContactApi.class);

        // Setup save button
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveContact();
            }
        });
    }

    private void saveContact() {
        String name = editTextName.getText().toString().trim();
        String phoneStr = editTextPhone.getText().toString().trim();

        if (name.isEmpty() || phoneStr.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        int phone;
        try {
            phone = Integer.parseInt(phoneStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Numéro de téléphone invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        // Afficher un indicateur de chargement
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        btnSave.setEnabled(false); // Désactiver le bouton pendant l'opération

        Contact contact = new Contact();
        contact.setName(name);
        contact.setNumber(phone);

        Log.d("AddContactActivity", "Envoi du contact au serveur: " + contact.toString());

        contactApi.saveContact(contact).enqueue(new Callback<Contact>() {
            @Override
            public void onResponse(Call<Contact> call, Response<Contact> response) {
                // Masquer l'indicateur de chargement
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                btnSave.setEnabled(true);

                Log.d("AddContactActivity", "Response code: " + response.code());

                if (response.isSuccessful()) {
                    Toast.makeText(AddContactActivity.this, "Contact ajouté avec succès", Toast.LENGTH_SHORT).show();
                    finish(); // Retour à l'activité précédente
                } else {
                    try {
                        // Tenter d'afficher le message d'erreur du serveur
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() :
                                "Erreur inconnue";
                        Log.e("AddContactActivity", "Error Body: " + errorBody);
                        Toast.makeText(AddContactActivity.this,
                                "Erreur: " + response.code() + " - " + errorBody,
                                Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(AddContactActivity.this,
                                "Erreur lors de l'ajout du contact: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                        Log.e("AddContactActivity", "Erreur lors de la lecture de l'erreur", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<Contact> call, Throwable t) {
                // Masquer l'indicateur de chargement
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                btnSave.setEnabled(true);

                Toast.makeText(AddContactActivity.this,
                        "Erreur réseau: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();

                // Log l'erreur pour le débogage
                Log.e("AddContactActivity", "Erreur réseau", t);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}