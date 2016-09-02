
package br.com.brunoscrokbrunoro.cognitivevision;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.microsoft.projectoxford.helper.ImageHelper;
import com.microsoft.projectoxford.helper.ObjectToMap;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.contract.Caption;
import com.microsoft.projectoxford.vision.contract.Category;
import com.microsoft.projectoxford.vision.contract.Face;
import com.microsoft.projectoxford.vision.contract.NameScorePair;
import com.microsoft.projectoxford.vision.contract.Tag;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyzeActivity extends AppCompatActivity {

    private VisionServiceClient client;

    private static final int REQUEST_TAKE_PHOTO = 0;

    private Uri mUriPhotoTaken;
    private Bitmap mBitmap;

    private ProgressDialog progressDialog;

    private TextView txtDescricao;
    private TextView txtCategorias;
    private TextView txtFaces;
    private TextView txtTags;

    private ImageView imageView;

    private LinearLayout lilFaces;
    private LinearLayout lilCategorias;
    private LinearLayout lilDescricao;
    private LinearLayout lilImage;
    private LinearLayout lilTags;

    private FirebaseDatabase database;
    private DatabaseReference myRef;

    public static DecimalFormat df = new DecimalFormat("##.##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze);

        txtDescricao = (TextView) findViewById(R.id.txtDescricao);
        txtCategorias = (TextView) findViewById(R.id.txtCategorias);
        txtFaces = (TextView) findViewById(R.id.txtFaces);
        txtTags = (TextView) findViewById(R.id.txtTags);

        imageView = (ImageView) findViewById(R.id.imageView);

        lilFaces = (LinearLayout) findViewById(R.id.lilFaces);
        lilCategorias = (LinearLayout) findViewById(R.id.lilCategorias);
        lilDescricao = (LinearLayout) findViewById(R.id.lilDescricao);
        lilImage = (LinearLayout) findViewById(R.id.lilImage);
        lilTags = (LinearLayout) findViewById(R.id.lilTags);

        if (client==null){
            client = new VisionServiceRestClient(getString(R.string.subscription_key));
        }
        lilImage.setVisibility(View.GONE);
        lilCategorias.setVisibility(View.GONE);
        lilDescricao.setVisibility(View.GONE);
        lilFaces.setVisibility(View.GONE);
        lilTags.setVisibility(View.GONE);

        database = FirebaseDatabase.getInstance();

        myRef = database.getReference("analyze");

    }

    public void takePhoto(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(getPackageManager()) != null) {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            try {
                File file = File.createTempFile("IMG_", ".jpg", storageDir);
                mUriPhotoTaken = Uri.fromFile(file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            } catch (IOException e) {
                Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode)
        {
            case REQUEST_TAKE_PHOTO: {
                if (resultCode == RESULT_OK) {
                    if(mUriPhotoTaken != null) {
                        mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                                mUriPhotoTaken, getContentResolver());
                        imageView.setImageBitmap(mBitmap);
                        lilImage.setVisibility(View.VISIBLE);
                        doAnalyze();
                    }

                    return;
                }
            }
            default:
                break;
        }
    }

    public void doAnalyze() {
        progressDialog = ProgressDialog.show(this,"Processando","Analizando",true,true);
        progressDialog.show();

        lilCategorias.setVisibility(View.GONE);
        lilDescricao.setVisibility(View.GONE);
        lilFaces.setVisibility(View.GONE);

        try {
            new doRequest().execute();
        } catch (Exception e)
        {
            Toast.makeText(this,"Error encountered. Exception is: " + e.toString(),Toast.LENGTH_SHORT).show();
        }
    }


    private String process() throws VisionServiceException, IOException {
        Gson gson = new Gson();
        String[] features = {"Faces", "Categories","Description"};
        String[] details = {};

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        AnalysisResult v = this.client.analyzeImage(inputStream, features, details);

        String result = gson.toJson(v);
        Log.d("result", result);

        return result;
    }

    private class doRequest extends AsyncTask<String, String, String> {
        // Store error message
        private Exception e = null;

        public doRequest() {
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                return process();
            } catch (Exception e) {
                this.e = e;
            }

            return null;
        }

        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);
            try {
                if (e != null) {
                    e.printStackTrace();
                } else {
                    Gson gson = new Gson();
                    AnalysisResult result = gson.fromJson(data, AnalysisResult.class);

                    HashMap<String, Object> resultFirebase = new HashMap<>();

                    lilDescricao.setVisibility(View.VISIBLE);

                    if (result.description.captions.isEmpty()) {
                        txtDescricao.setText("Não foi possivel encontrar uma descrição para a foto");

                        lilTags.setVisibility(View.GONE);
                    } else {
                        txtDescricao.setText("");
                        HashMap<String, Object> captions = new HashMap<>();
                        int x = 0;
                        for (Caption caption : result.description.captions) {
                            txtDescricao.append(caption.text + "  Confiança: " + df.format(100 * caption.confidence) + "%\n");
                            captions.put(Integer.toString(x), ObjectToMap.caption(caption));
                            x++;
                        }
                        resultFirebase.put("captions",captions);

                       resultFirebase.put("tags", result.description.tags);
                        for (String tag : result.description.tags) {
                            txtTags.append(tag + "\n");
                        }
                    }
                    if (result.categories.isEmpty()) {
                        lilCategorias.setVisibility(View.GONE);
                    } else {
                        lilCategorias.setVisibility(View.VISIBLE);
                        txtCategorias.setText("");
                        HashMap<String, Object> categories = new HashMap<>();
                        int x = 0;
                        for (Category category : result.categories) {
                            txtCategorias.append(category.name + ", Confiança: " + df.format(100 * category.score) + "%\n");
                            categories.put(Integer.toString(x), ObjectToMap.category(category));
                            x++;
                        }
                        resultFirebase.put("categories",categories);
                    }

                    if (result.faces.isEmpty()) {
                        lilFaces.setVisibility(View.GONE);
                    } else {
                        lilFaces.setVisibility(View.VISIBLE);
                        txtFaces.setText("");
                        HashMap<String, Object> faces = new HashMap<>();
                        int x = 0;
                        for (Face face : result.faces) {
                            txtFaces.append("Sexo:" + (face.gender.equals("Male") ? "Masculino" : "Feminino") + ", Idade: " + +face.age + "(Confiança: " + df.format(100 * face.genderScore) + "%)\n");
                            faces.put(Integer.toString(x), ObjectToMap.face(face));
                            x++;
                        }
                        resultFirebase.put("faces",faces);
                    }

                    Map<String, Object> childUpdates = new HashMap<>();
                    childUpdates.put(result.requestId.toString(), resultFirebase);
                    myRef.updateChildren(childUpdates);
                }
            }catch(Exception e){
                e.printStackTrace();
            }

           progressDialog.cancel();
        }
    }
}
