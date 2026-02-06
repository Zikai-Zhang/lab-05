package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;


public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private Button deleteCityButton;
    private ListView cityListView;

    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;
    private City selectedCity;
    private int selectedCityPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        deleteCityButton = findViewById(R.id.buttonDeleteCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);
        cityListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        citiesRef.addSnapshotListener((QuerySnapshot value, FirebaseFirestoreException error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }

            cityArrayList.clear();
            if (value != null) {
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");
                    cityArrayList.add(new City(name, province));
                }
            }
            if (selectedCity != null) {
                boolean stillExists = false;
                for (int i = 0; i < cityArrayList.size(); i++) {
                    City city = cityArrayList.get(i);
                    if (city.getName().equals(selectedCity.getName())) {
                        stillExists = true;
                        selectedCity = city;
                        selectedCityPosition = i;
                        break;
                    }
                }
                if (!stillExists) {
                    selectedCity = null;
                    selectedCityPosition = -1;
                    cityListView.clearChoices();
                } else {
                    cityListView.setItemChecked(selectedCityPosition, true);
                }
            }
            cityArrayAdapter.notifyDataSetChanged();
        });

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });
        deleteCityButton.setOnClickListener(view -> confirmDeleteSelectedCity());

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            selectedCity = city;
            selectedCityPosition = i;
            cityListView.setItemChecked(i, true);
        });

        cityListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            if (city == null) {
                return true;
            }
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
            return true;
        });
    }

    @Override
    public void updateCity(City city, String title, String year) {
        String oldName = city.getName();
        city.setName(title);
        city.setProvince(year);
        cityArrayAdapter.notifyDataSetChanged();
        selectedCity = city;

        // Updating the database using delete + addition
        if (!oldName.equals(title)) {
            citiesRef.document(oldName).delete();
        }
        citiesRef.document(city.getName()).set(city);
    }

    @Override
    public void addCity(City city){
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();

        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(city);
    }

    private void confirmDeleteSelectedCity() {
        if (selectedCity == null) {
            Toast.makeText(this, "Please select a city first.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete City")
                .setMessage("Delete " + selectedCity.getName() + "?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    citiesRef.document(selectedCity.getName()).delete();
                    selectedCity = null;
                    selectedCityPosition = -1;
                    cityListView.clearChoices();
                    cityArrayAdapter.notifyDataSetChanged();
                })
                .show();
    }


}
