package com.example.farmily;

import static android.Manifest.permission.READ_MEDIA_IMAGES;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.File;
import java.util.ArrayList;

import model.Address;
import model.GitHubUtilities;
import model.Listing;  // Make sure you import your Listing model

public class ListingActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener{

    private EditText editTextDescription;
    private EditText editTextProductId;
    private TextView textViewPhoto;
    private ImageView imageViewProduct;
    private EditText editTextPrice;
    private EditText editTextStock;
    private EditText editTextTitle;
    private EditText editTextDeliveryArea;

    private Button buttonSubmit;
    private Spinner spPhoto;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> photoList;
    private ArrayList<String> photoPath;
    private ImageView imPhoto;
    DatabaseReference listings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_listing);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_listing), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initialize();
    }

    private void initialize() {
        imPhoto = findViewById(R.id.imageViewProduct);
        spPhoto = findViewById(R.id.spPhoto);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextProductId = findViewById(R.id.editTextProductId);
        textViewPhoto = findViewById(R.id.textViewPhoto);
        imageViewProduct = findViewById(R.id.imageViewProduct);
        editTextPrice = findViewById(R.id.editTextPrice);
        editTextStock = findViewById(R.id.editTextStock);
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextDeliveryArea = findViewById(R.id.editTextDeliveryArea);

        spPhoto.setOnItemSelectedListener(this);
        photoList = new ArrayList<>();
        photoPath = new ArrayList<>();

        listings = FirebaseDatabase.getInstance().getReference("Listings");

        ActivityCompat.requestPermissions(this, new String[]{READ_MEDIA_IMAGES}, PackageManager.PERMISSION_GRANTED);
        getFilesInfoFromDownloads();

        buttonSubmit = findViewById(R.id.buttonSubmit);
        buttonSubmit.setOnClickListener(this);
        this.textViewPhoto.setOnClickListener(v -> getFilesInfoFromDownloads());
    }

    @Override
    public void onClick(View v) {

        String description = editTextDescription.getText().toString().trim();
        String productId = editTextProductId.getText().toString().trim();
        String priceStr = editTextPrice.getText().toString().trim();
        String stockStr = editTextStock.getText().toString().trim();
        String title = editTextTitle.getText().toString().trim();
        String addressInput = editTextDeliveryArea.getText().toString().trim();

        if (description.isEmpty() || productId.isEmpty() || priceStr.isEmpty() ||
                stockStr.isEmpty() || title.isEmpty() ||  addressInput.isEmpty()) {
            Toast.makeText(this, "Please fill in all the fields", Toast.LENGTH_SHORT).show();
            return;
        }


        float price;
        int stock;
        try {
            price = Float.parseFloat(priceStr);
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Price and Stock must be valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] addrParts = addressInput.split(",");
        if (addrParts.length != 6) {
            Toast.makeText(this, "Please enter address as: streetNumber, streetName, postalCode, city, province, country", Toast.LENGTH_LONG).show();
            return;
        }

        int streetNumber;
        try {
            streetNumber = Integer.parseInt(addrParts[0].trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid street number", Toast.LENGTH_SHORT).show();
            return;
        }
        String streetName = addrParts[1].trim();
        String postalCode = addrParts[2].trim();
        String city = addrParts[3].trim();
        String province = addrParts[4].trim();
        String country = addrParts[5].trim();
        Address address = new Address(streetNumber, streetName, postalCode, city, province, country);


        Listing listing = new Listing();
        listing.setId(productId);
        listing.setDescription(description);
        listing.setTitle(title);
        listing.setPrice(price);
        listing.setStock(stock);
        listing.setDeliveryArea(address);


        if (!photoPath.isEmpty() && spPhoto.getSelectedItemPosition() < photoPath.size()) {
            listing.setImagePath(photoPath.get(spPhoto.getSelectedItemPosition()));
        } else {
            listing.setImagePath("");
        }

        String key = listings.push().getKey();
        listings.child(key).setValue(listing);
        Toast.makeText(this, "Listing Created Successfully", Toast.LENGTH_SHORT).show();


        clearFields();
    }

    private void clearFields(){
        editTextDescription.setText("");
        editTextProductId.setText("");
        editTextPrice.setText("");
        editTextStock.setText("");
        editTextTitle.setText("");
        editTextDeliveryArea.setText("");
    }

    public void getFilesInfoFromDownloads() {
        File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDirectory.exists() && downloadDirectory.isDirectory()) {
            File[] files = downloadDirectory.listFiles();
            if (files != null && files.length > 0) {
                populateSpinner(files);
            } else {
                Log.d("FILES", "No files found in the Download Folder");
            }
        } else {
            Log.d("FILES", "Download directory does not exist");
        }
    }

    private void populateSpinner(File[] files) {
        photoList.clear();
        photoPath.clear();
        for (File file : files) {
            photoList.add(file.getName());
            photoPath.add(file.getAbsoluteFile().getAbsolutePath());
        }
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, photoList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPhoto.setAdapter(adapter);
    }

    private void uploadPhoto(View view) {
        String photo = spPhoto.getSelectedItem().toString();
        GitHubUtilities.uploadFileToGithub(this, photo);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String photo = spPhoto.getSelectedItem().toString();
        String photoFile = photoPath.get(position);
        Bitmap bitmapPhoto = BitmapFactory.decodeFile(photoFile);
        imPhoto.setImageBitmap(bitmapPhoto);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // No action needed
    }
}
