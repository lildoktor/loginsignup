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
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.ListResult;
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
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    Button button, btn_upload, btn_delete;
    TextView textView;
    FirebaseUser user;
    private static final int MM_CODE = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        button = findViewById(R.id.logout);
        textView = findViewById(R.id.user_details);
        user = auth.getCurrentUser();

        btn_delete = findViewById(R.id.del);
        btn_upload = findViewById(R.id.upload);

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

        btn_upload.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/*", "video/*", "audio/*"});
            startActivityForResult(intent, MM_CODE);
        });

        btn_delete.setOnClickListener(view -> {
            String uuid = user.getUid();
            FirebaseStorage storage = FirebaseStorage.getInstance();
            storage.getReference(uuid+"/1691233273").listAll().addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    for(StorageReference item : task.getResult().getItems()) {
                        item.delete();
                    }
                }
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == MM_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                String timestamp = String.valueOf(Instant.now().getEpochSecond());
                String uuid = user.getUid();

                String filePath = uuid + "/" + timestamp + "/" + timestamp;
                String mimeType = getContentResolver().getType(uri);

                StorageMetadata metadata = new StorageMetadata.Builder()
                        .setContentType(mimeType).setCustomMetadata("t", timestamp)
                        .build();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference().child(filePath);
                UploadTask uploadTask = storageRef.putFile(uri, metadata);

                uploadTask.addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(MainActivity.this, "Upload successful", Toast.LENGTH_LONG).show();

                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        FirebaseDatabase database = FirebaseDatabase.getInstance("https://booqr-3cb0a-default-rtdb.europe-west1.firebasedatabase.app");
                        DatabaseReference myRef = database.getReference("files");

                        Map<String, Object> fileData = new HashMap<>();
                        fileData.put("timestamp", timestamp);
                        fileData.put("mimeType", mimeType);

                        myRef.child(uuid).child(timestamp).setValue(fileData).addOnCompleteListener(task -> {
                            String t = String.valueOf(Instant.now().getEpochSecond());
                            myRef.child(uuid).child(timestamp).child(t).setValue(fileData).addOnCompleteListener(task1 -> {
                                if(task1.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "Upload successful2!!!", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Upload failed", Toast.LENGTH_LONG).show();
                                }
                            });

                            if(task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "Upload successful1!!!", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Upload failed", Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                }).addOnFailureListener(e -> {
                    Log.e("Error2", e.getMessage());
                    Toast.makeText(MainActivity.this, "Upload Failed -> " + e, Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
        }
    }
}