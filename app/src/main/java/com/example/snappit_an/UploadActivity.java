package com.example.snappit_an;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.Celebrity;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.RecognizeCelebritiesRequest;
import com.amazonaws.services.rekognition.model.RecognizeCelebritiesResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class UploadActivity extends AppCompatActivity {
    AmazonRekognitionClient amazonRekognitionClient;
    private final int REQUEST_CODE = 100;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String PREFERENCES_FILE = null;
    private static final String PREF_USER_FIRST_TIME = null;



    private static final String IMDB_TOKEN = "k_l1oxwwl6";

    public void getCredential() {
        amazonRekognitionClient = new AmazonRekognitionClient(new AWSUtil());
        amazonRekognitionClient.setRegion(Region.getRegion(Regions.US_EAST_1));
    }

    private final void openGalleryForImage() {
        Intent intent = new Intent("android.intent.action.PICK");
        intent.setType("image/*");
        this.startActivityForResult(intent, this.REQUEST_CODE);

    }

    private final void openCameraForImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        this.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //fix networking operation
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            //create page
            super.onCreate(savedInstanceState);
            setContentView(R.layout.upload_activity);
            Boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .getBoolean("isFirstRun", true);


            getCredential();

                UploadActivity.this.openGalleryForImage();


        }



        ImageButton button = (ImageButton) findViewById(R.id.goBackHome);
        button.setOnClickListener((View.OnClickListener) (new View.OnClickListener() {
            public final void onClick(View it) {
                Intent myintent = new Intent(UploadActivity.this, MainActivity.class);
                startActivity(myintent);
            }
        }));
    }


    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == -1 && requestCode == this.REQUEST_CODE) {

            if (data != null) {
                Uri uri = data.getData();
                ImageView imageView = (ImageView) this.findViewById(R.id.imageBox);
                imageView.setImageURI(uri);
                Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                    findCelebrity(ByteBuffer.wrap(baos.toByteArray()));
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        if (requestCode == this.REQUEST_IMAGE_CAPTURE && resultCode == 1) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            if (data != null) {
                Uri uri = data.getData();
                ImageView imageView = (ImageView) this.findViewById(R.id.imageBox);
                imageView.setImageURI(uri);
                imageBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                    findCelebrity(ByteBuffer.wrap(baos.toByteArray()));
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

//    public void buttonClick() {
//        ImageView imageView = (ImageView) this.findViewById(R.id.imageBox);
//
//        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
//
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//            findCelebrity(ByteBuffer.wrap(baos.toByteArray()));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void findCelebrity(ByteBuffer imageBytes) throws IOException, JSONException {

        if (imageBytes != null && imageBytes.hasArray()) {
            RecognizeCelebritiesRequest request = new RecognizeCelebritiesRequest()
                    .withImage(new Image().withBytes(imageBytes));


            RecognizeCelebritiesResult result = amazonRekognitionClient.recognizeCelebrities(request);

            //Display recognized celebrity information
            List<Celebrity> celebs = result.getCelebrityFaces();

            Log.e(String.valueOf(celebs.size()), " celebrity(s) were recognized.\n");

            for (Celebrity celebrity : celebs) {

                Log.i("Celebrity recognized: ", celebrity.getName());
                //show in app
                TextView textView = (TextView) findViewById(R.id.nameHere);
                textView.append(celebrity.getName());

                Log.i("Celebrity ID: ", celebrity.getId());
                BoundingBox boundingBox = celebrity.getFace().getBoundingBox();
                Log.i("position: ",
                        boundingBox.getLeft().toString() + " " +
                                boundingBox.getTop().toString());
                Log.i("more info", "Further information (if available):");
                for (String url : celebrity.getUrls()) {
                    Log.i(url, "url");

                    if (url.contains("imdb")) {
                        String[] parts = url.split("/");
                        String nameID = parts[parts.length - 1];
                        requestIMDB(nameID);
                    }
                }
            }
        } else {
            Toast.makeText(getApplicationContext(),
                    "Sorry, file path is missing!", Toast.LENGTH_LONG).show();
        }
    }




    public void requestIMDB (String nameID)  throws JSONException  {
        if (nameID == null || nameID.isEmpty()) {
            return;
        }

        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url("https://imdb-api.com/en/API/Name/" + IMDB_TOKEN + "/" + nameID)
                .method("GET", null)
                .build();
        Response response = null;

        try {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            response = client.newCall(request).execute();

            if (response != null && response.body() != null) {

                String jsonData = response.body().string();
                JSONObject Jobject = new JSONObject(jsonData);

                TextView mTextView = (TextView) findViewById(R.id.movie_title);
                mTextView.setText(Jobject.getString("summary"));


                JSONArray Jarray = Jobject.getJSONArray("knownFor");

//                TextView mTextView = findViewById(R.id.movie_title);
//                ImageView img = findViewById(R.id.imageView);

                LinearLayout moviesLayout = (LinearLayout)findViewById(R.id.linearLayout);



                for (int i = 0; i < Jarray.length(); i++) {
                    JSONObject object = Jarray.getJSONObject(i);

                    View inflatedView = inflater.inflate(R.layout.image_item, null);
                    ImageView imgView = (ImageView) inflatedView.findViewById(R.id.imageView);

                    Picasso.get().load(Uri.parse(object.getString("image"))).into(imgView);
                    moviesLayout.addView(imgView);


                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }





    }


}

