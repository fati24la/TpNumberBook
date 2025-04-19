package com.example.tpcontact;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tpcontact.Adapter.ContactAdapter;
import com.example.tpcontact.api.ContactApi;
import com.example.tpcontact.bean.Contact;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ContactAdapter contactAdapter;
    private List<Contact> contactList;
    private FloatingActionButton fabAddContact;
    private SearchView searchView;
    private LinearLayout emptyState;
    private TextView contactsCount;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        fabAddContact = findViewById(R.id.fabAddContact);
        searchView = findViewById(R.id.searchView);
        emptyState = findViewById(R.id.emptyState);
        contactsCount = findViewById(R.id.contactsCount);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactList = new ArrayList<>();
        contactAdapter = new ContactAdapter(contactList);
        recyclerView.setAdapter(contactAdapter);

        // Initialiser l'état vide
        updateEmptyStateVisibility();

        // Setup FAB
        fabAddContact.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddContactActivity.class);
            startActivity(intent);
        });

        // Setup SearchView
        setupSearchView();

        // Demander la permission de lire les contacts si nécessaire
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 1);
        }
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (contactAdapter != null) {
                    Log.d("MainActivity", "Filtering with: " + newText);
                    contactAdapter.getFilter().filter(newText);
                    // Mise à jour du compteur après filtrage
                    updateContactsCount(contactAdapter.getFilteredItemCount());
                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Récupérer les contacts de l'API
        getApiContacts();
    }

    private void updateEmptyStateVisibility() {
        if (contactList == null || contactList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateContactsCount(int count) {
        contactsCount.setText("Tous les contacts (" + count + ")");
    }

    private void getApiContacts() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        ContactApi contactApi = RetrofitClient.getClient().create(ContactApi.class);
        Log.d("MainActivity", "Fetching contacts from API...");

        contactApi.getAllContacts().enqueue(new Callback<List<Contact>>() {
            @Override
            public void onResponse(Call<List<Contact>> call, Response<List<Contact>> response) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                Log.d("MainActivity", "API Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    contactList.clear();
                    List<Contact> receivedContacts = response.body();
                    contactList.addAll(receivedContacts);

                    Log.d("MainActivity", "Received " + contactList.size() + " contacts");

                    // Mettre à jour l'adaptateur
                    contactAdapter.updateContacts(contactList);

                    // Mettre à jour le compteur de contacts
                    updateContactsCount(contactList.size());

                    // Mettre à jour la visibilité de l'état vide
                    updateEmptyStateVisibility();

                    // Réappliquer le filtre si une recherche est en cours
                    if (!searchView.getQuery().toString().isEmpty()) {
                        contactAdapter.getFilter().filter(searchView.getQuery());
                    }
                } else {
                    try {
                        // Tenter d'afficher le message d'erreur du serveur
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() :
                                "Erreur inconnue";
                        Log.e("MainActivity", "Error Body: " + errorBody);
                        Toast.makeText(MainActivity.this,
                                "Erreur: " + response.code() + " - " + errorBody,
                                Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this,
                                "Erreur lors de la récupération des contacts: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                        Log.e("MainActivity", "Erreur lors de la lecture de l'erreur", e);
                    }

                    // Même en cas d'erreur, mettre à jour l'état vide
                    updateEmptyStateVisibility();
                }
            }

            @Override
            public void onFailure(Call<List<Contact>> call, Throwable t) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                Toast.makeText(MainActivity.this,
                        "Erreur réseau: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                Log.e("MainActivity", "Erreur réseau", t);

                // Mettre à jour l'état vide même en cas d'erreur
                updateEmptyStateVisibility();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission accordée, vous pouvez accéder aux contacts du téléphone si nécessaire
            // getPhoneContacts();
        } else {
            Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show();
        }
    }
}