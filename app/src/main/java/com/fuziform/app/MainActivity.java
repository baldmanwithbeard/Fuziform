package com.fuziform.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.vuzix.hud.actionmenu.ActionMenuActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

//import com.vuzix.hud.actionmenu.DefaultActionMenuItemView;
public class MainActivity extends ActionMenuActivity {
    public String uniqueId = "";
    private Boolean firstRun = false;
    public String resultMessage = "";
    private static final int REQUEST_TAKE_PHOTO = 0;
    private final int PICK_IMAGE = 1;
    // The URI of photo taken with camera
    private Uri mUriPhotoTaken;

    private final String apiEndpoint = "https://facialrecognitionresource.cognitiveservices.azure.com/face/v1.0";
    private final String subscriptionKey = "e67038c399f54a49a1e7854811a398e6";
    private final FaceServiceClient faceServiceClient =
            new FaceServiceRestClient(apiEndpoint, subscriptionKey);
    private ProgressDialog detectionProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        firstRun = VerifyUser.isFirstRun(this);
        if (firstRun){addPersonGroup();}
        uniqueId = VerifyUser.id(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
//        listView = findViewById(R.id.detailsView);
        detectionProgressDialog = new ProgressDialog(this);
        
    }

    @Override
    protected boolean onCreateActionMenu(Menu menu) {
        super.onCreateActionMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected boolean alwaysShowActionMenu() {
        return false;
    }

    public void browseForImage(MenuItem item) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(
                intent, "Select Picture"), PICK_IMAGE);
    }
// findFace doesn't get used yet... (needs to be solved)
    public void findFace(MenuItem item) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(getPackageManager()) != null) {
            // Save the photo taken to a temporary file.
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            try {
                File file = File.createTempFile("IMG_", ".jpg", storageDir);
                mUriPhotoTaken = Uri.fromFile(file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            } catch (IOException e) {
                setInfo(e.getMessage());
            }
        }
    }

    public void manageFaces(MenuItem item){
        startActivity(new Intent(this, ManageFaces.class));
    }
    public void addPersonGroup() {
        String personGroupId = UUID.randomUUID().toString();

        Intent intent = new Intent(this, PersonGroupActivity.class);
        intent.putExtra("AddNewPersonGroup", true);
        intent.putExtra("PersonGroupName", "");
        intent.putExtra("PersonGroupId", personGroupId);
        startActivity(intent);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK &&
                data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), uri);
                ImageView imageView = findViewById(R.id.imageView);
                imageView.setImageBitmap(bitmap);

                // Comment out for tutorial

                detectAndFrame(bitmap);
                setInfo(resultMessage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // Detect faces by uploading a face image.
// Frame faces after detection.
    private void detectAndFrame(final Bitmap imageBitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());

        @SuppressLint("StaticFieldLeak") AsyncTask<InputStream, String, Face[]> detectTask = new AsyncTask<InputStream, String, Face[]>() {
            String exceptionMessage = "";

            @SuppressLint("DefaultLocale")
            @Override
            protected Face[] doInBackground(InputStream... params) {
                try {
                    publishProgress("Detecting...");
                    Face[] result = faceServiceClient.detect(
                            params[0],
                            true,         // returnFaceId
                            false,        // returnFaceLandmarks
                            null          // returnFaceAttributes:
                        /* new FaceServiceClient.FaceAttributeType[] {
                            FaceServiceClient.FaceAttributeType.Age,
                            FaceServiceClient.FaceAttributeType.Gender }
                        */
                    );
                    if (result == null) {
                        publishProgress(
                                "Detection Finished. Nothing detected");
                        return null;
                    }
                    resultMessage = String.format(
                            "Detection Finished. %d face(s) detected",
                            result.length);
                    publishProgress(resultMessage);
                    return result;
                } catch (Exception e) {
                    exceptionMessage = String.format(
                            "Detection failed: %s", e.getMessage());
                    return null;
                }
            }

            @Override
            protected void onPreExecute() {
                //TODO: show progress dialog
                detectionProgressDialog.show();
            }

            @Override
            protected void onProgressUpdate(String... progress) {
                //TODO: update progress
                detectionProgressDialog.setMessage(progress[0]);
            }

            @Override
            protected void onPostExecute(Face[] result) {
                //TODO: update face frames
                detectionProgressDialog.dismiss();

                if (!exceptionMessage.equals("")) {
                    showError(exceptionMessage);
                }
                if (result == null) return;

                ImageView imageView = findViewById(R.id.imageView);
                imageView.setImageBitmap(
                        drawFaceRectanglesOnBitmap(imageBitmap, result));
                imageBitmap.recycle();
                setInfo(resultMessage);
            }
        };

        detectTask.execute(inputStream);
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }})
                .create().show();
    }

    private static Bitmap drawFaceRectanglesOnBitmap(
            Bitmap originalBitmap, Face[] faces) {
        Bitmap bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        if (faces != null) {
            for (Face face : faces) {
                FaceRectangle faceRectangle = face.faceRectangle;
                canvas.drawRect(
                        faceRectangle.left,
                        faceRectangle.top,
                        faceRectangle.left + faceRectangle.width,
                        faceRectangle.top + faceRectangle.height,
                        paint);
            }
        }
        return bitmap;
    }
    private void setInfo(String info) {
        TextView textView = findViewById(R.id.textView);
        textView.setText(info);
    }
}
