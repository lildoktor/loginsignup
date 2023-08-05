package com.example.codeseasy.com.firebaseauth;

import static java.lang.Thread.sleep;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageMetadata;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    Button button, button_image, button_all;
    TextView textView;
    FirebaseUser user;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int YOUR_REQUEST_CODE = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        button = findViewById(R.id.logout);
        textView = findViewById(R.id.user_details);
        user = auth.getCurrentUser();

        button_all = findViewById(R.id.all);
        button_image = findViewById(R.id.upload);

        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        } else {
            textView.setText(user.getEmail());
        }

        button.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });

        button_image.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

        button_all.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/*", "video/*", "audio/*"});
            startActivityForResult(intent, YOUR_REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri uri = data.getData();
                String timestamp = String.valueOf(Instant.now().getEpochSecond());
                String uuid = user.getUid();

                String filePath = uuid + "/" + timestamp + "/" + timestamp;
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] bytes = baos.toByteArray();
                String mimeType = getContentResolver().getType(uri);

                // Create metadata
                StorageMetadata metadata = new StorageMetadata.Builder()
                        .setContentType(mimeType)
                        .build();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference().child(filePath);
                UploadTask uploadTask = storageRef.putBytes(bytes, metadata);
                uploadTask.addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(MainActivity.this, "Upload successful", Toast.LENGTH_LONG).show();
                    // Handle successful upload here
                }).addOnFailureListener(e -> {
                    Log.e("Error1", e.getMessage());
                    Toast.makeText(MainActivity.this, "Upload Failed -> " + e, Toast.LENGTH_LONG).show();
                    e.printStackTrace();

                    // Handle failed upload here
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        else if(requestCode == YOUR_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri uri = data.getData();
                String timestamp = String.valueOf(Instant.now().getEpochSecond());
                String uuid = user.getUid();

                String filePath = uuid + "/" + timestamp + "/" + timestamp;
                String mimeType = getContentResolver().getType(uri);

                // Create metadata
                StorageMetadata metadata = new StorageMetadata.Builder()
                        .setContentType(mimeType).setCustomMetadata("t", timestamp)
                        .build();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference().child(filePath);
                UploadTask uploadTask = storageRef.putFile(uri, metadata);

                uploadTask.addOnSuccessListener(taskSnapshot -> {

                    Toast.makeText(MainActivity.this, "Upload successful", Toast.LENGTH_LONG).show();
                    // Handle successful upload here
                }).addOnFailureListener(e -> {
                    Log.e("Error2", e.getMessage());
                    Toast.makeText(MainActivity.this, "Upload Failed -> " + e, Toast.LENGTH_LONG).show();
                    e.printStackTrace();

                    // Handle failed upload here
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}