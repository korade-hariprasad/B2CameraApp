package com.example.cameraapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button btnCapture;
    Button btnGallery;
    private ImageView capturedImageView;
    private String currentPhotoPath;
    private Uri photoURI;

    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result) {
                        capturedImageView.setImageURI(photoURI);
                        capturedImageView.setDrawingCacheEnabled(true);
                        capturedImageView.buildDrawingCache();
                        Bitmap captureBitmap=capturedImageView.getDrawingCache();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        captureBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                        byte[] data = baos.toByteArray();
                        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                        StorageReference imagesRef = storageRef.child("images");
                        StorageReference spaceRef = storageRef.child("images/space.jpg");
                        spaceRef.putFile(photoURI).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                                if(task.isSuccessful())
                                {
                                    Log.d("mytag",task.getResult().getStorage().getPath());
                                    Toast.makeText(MainActivity.this, "Image Uploaded", Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(MainActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                                Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        // false
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnCapture = findViewById(R.id.btnCapture);
        btnGallery = findViewById(R.id.btnGallery);
        capturedImageView = findViewById(R.id.imageView);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checkCameraPermission();
            }
        });
        capturedImageView.setDrawingCacheEnabled(true);
        capturedImageView.buildDrawingCache();
        Bitmap bitmap=capturedImageView.getDrawingCache();
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                galleryMulti.launch(new PickVisualMediaRequest());
            }
        });


    }

    ActivityResultLauncher<PickVisualMediaRequest> gallery=registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri uri) {

            Log.d("mytag",uri.toString());
        }
    });
    ActivityResultLauncher<PickVisualMediaRequest> galleryMulti=registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), new ActivityResultCallback<List<Uri>>() {
        @Override
        public void onActivityResult(List<Uri> o) {
            Log.d("mytag",""+o.size());
        }
    });


    private void checkCameraPermission() {

        if(ContextCompat.checkSelfPermission(MainActivity.this,android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){

            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA))
            {
                showPermissionDialog();
            }else{

                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},100);
            }

        }else{
            Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
            dispatchTakePictureIntent();
        }
    }

    private void showPermissionDialog() {
        AlertDialog.Builder builder=new AlertDialog.Builder(this)
                .setTitle("Camera Permission Needed")
                .setMessage("This app needs the Camera permission to function properly. Please grant the permission.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 100);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        AlertDialog alertDialog=builder.create();
        alertDialog.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==100)
        {
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }else{

                if(!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA))
                {

                    showSettingsDialog();

                }else{
                    Toast.makeText(MainActivity.this, "Permission Denied Try Again", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showSettingsDialog() {

        AlertDialog.Builder builder=new AlertDialog.Builder(this)
                .setTitle("Camera Permission Needed")
                .setMessage("This app needs the Camera permission to function properly. Please grant the permission in settings.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        AlertDialog alertDialog=builder.create();
        alertDialog.show();
    }


    private void dispatchTakePictureIntent() {
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (photoFile != null) {
            photoURI = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
            takePictureLauncher.launch(photoURI);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(getApplicationContext().getExternalFilesDir(null), "images");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        Log.d("mytag",currentPhotoPath);
        return image;
    }
}