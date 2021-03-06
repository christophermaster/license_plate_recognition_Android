package com.sandro.openalprsample;

import android.Manifest;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.BoringLayout;
import android.util.Log;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.ImageView;

import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sandro.openalprsample.apiRest.models.AccessHistoryApi;
import com.sandro.openalprsample.conexion.BBDD_Helper;
import com.sandro.openalprsample.crudDao.HistoricalAccessDao;
import com.sandro.openalprsample.crudDao.OwnerDao;
import com.sandro.openalprsample.crudDao.VehicleDao;
import com.squareup.picasso.Picasso;

import org.openalpr.OpenALPR;
import org.openalpr.model.Results;
import org.openalpr.model.ResultsError;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;

import java.util.Locale;

public class ReconocimientoPlaca extends AppCompatActivity {

    private static final int REQUEST_IMAGE = 100;
    static final String ANDROID_DATA_DIR = "/data/data/com.sandro.openalprsample";

    private File destination;

    private TextView resultTextView;
    private TextView resultPorcenytaje;
    private TextView noExiste;
    private TextView pase;

    private ImageView imageView;
    private ImageView camara;
    private ImageView carpeta;

    private static final int PICK_IMAGE = 100;
    private Uri imageUri;
    private String name ;
    private Boolean type = true;
    final int STORAGE=1;

    private BBDD_Helper helper ;
    private Boolean verifivation ;
    private String placa;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reconocimiento_placa);

        resultTextView = (TextView) findViewById(R.id.textView);
        resultPorcenytaje = (TextView) findViewById(R.id.porcentaje);
        noExiste = (TextView) findViewById(R.id.noexiste);
        pase = (TextView) findViewById(R.id.pase);
        imageView = (ImageView) findViewById(R.id.imageView);
        camara = (ImageView) findViewById(R.id.camara);
        carpeta = (ImageView) findViewById(R.id.carpeta);
        placa = null;
     /*  click.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ActivityResult();
            }
        });*/
        helper = new BBDD_Helper(this);
        carpeta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type = true;
                openGallery();

            }
        });

        camara.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                type = false;
                name = dateToString(new Date(), "yyyy-MM-dd-hh-mm-ss");
                destination = new File(Environment.getExternalStorageDirectory(), name + ".jpg");
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(destination));
                startActivityForResult(intent, REQUEST_IMAGE);
            }
        });
    }


    private void openGallery(){
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        gallery.setType("image/");
        startActivityForResult(gallery.createChooser(gallery,"Seleccione la Aplicación"), PICK_IMAGE);

    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    //protected void ActivityResult() {
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        imageView.setImageDrawable(getResources().getDrawable(R.drawable.no_image));
        noExiste.setText("");
        pase.setText("");
        resultTextView.setText("");
        resultPorcenytaje.setText("");
        verifivation = false;

        if(resultCode == RESULT_OK && requestCode == PICK_IMAGE && type){

            imageUri = data.getData();
            destination = new File(getPath(imageUri));
            imageView.setImageURI(imageUri);


        }


        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK) {

            try {
                final ProgressDialog progress = ProgressDialog.show(this, "Cargando", "Procesando Imagen...", true);

                final String openAlprConfFile = ANDROID_DATA_DIR + File.separatorChar + "runtime_data" + File.separatorChar + "openalpr.conf";
                checkPermission();

                FileInputStream in = new FileInputStream(destination);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 10;

                Picasso.with(ReconocimientoPlaca.this).load(destination).fit().centerCrop().into(imageView);

                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        String result = OpenALPR.Factory.create(ReconocimientoPlaca.this, ANDROID_DATA_DIR).recognizeWithCountryRegionNConfig("us", "", destination.getAbsolutePath(), openAlprConfFile, 10);

                        Log.d("OPEN ALPR", result);

                        try {
                            final Results results = new Gson().fromJson(result, Results.class);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {


                                    StringBuilder listPlaca = new StringBuilder();
                                    StringBuilder listPorcentaje = new StringBuilder();

                                    if (results != null && results.getResults() !=null) {
                                        listPlaca.append("Placa");
                                        listPlaca.append("\n");
                                        listPorcentaje.append("Porcentaje %");
                                        listPorcentaje.append("\n");
                                        for (int i = 0; i <= results.getResults().get(0).getCandidates().size() / 3; i++) {
                                            listPlaca.append(results.getResults().get(0).getCandidates().get(i).getPlate());
                                            listPlaca.append("\n");
                                            listPorcentaje.append(results.getResults().get(0).getCandidates().get(i).getConfidence() + " %");
                                            listPorcentaje.append("\n");

                                            if(!verifivation){

                                                SQLiteDatabase db = helper.getWritableDatabase();
                                                verifivation = VehicleDao.exists(db, results.getResults().get(0).getCandidates().get(i).getPlate());
                                                placa =  results.getResults().get(0).getCandidates().get(i).getPlate();

                                            }

                                            System.out.println("Plate: " + results.getResults().get(0).getCandidates().get(i).getPlate());
                                        }
                                        resultTextView.setText(listPlaca);
                                        resultPorcenytaje.setText(listPorcentaje);
                                        try {
                                            create(placa,verifivation);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                    } else {
                                        noExiste.setText("Error , no se encontró placa");
                                    }


                                }
                            });
                        } catch (JsonSyntaxException exception) {
                            final ResultsError resultsError = new Gson().fromJson(result, ResultsError.class);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    resultTextView.setText(resultsError.getMsg());
                                }
                            });
                        }

                        progress.dismiss();
                    }
                });

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    private void checkPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(this, "We require access to storage to manage the picture.", Toast.LENGTH_LONG).show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE);
                // WRITE_EXTERNAL_STORAGE is an app-defined int constant. The callback method gets the result of the request.
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case STORAGE:{
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this,"Storage permision is needed to analye the picture.", Toast.LENGTH_LONG).show();
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public String dateToString(Date date, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format, Locale.getDefault());

        return df.format(date);
    }


    public void create(String placa, Boolean exists) throws IOException {

        SQLiteDatabase db = helper.getWritableDatabase();
        SimpleDateFormat formatoFecha,formatohora;
        byte[] photo;

        photo = readBytes(imageUri);
        Date fechaActual = new Date();

        formatoFecha = new SimpleDateFormat("yyyy-MM-dd");
        formatohora = new SimpleDateFormat("HH:mm:ss");


        AccessHistoryApi model = new AccessHistoryApi();
        Integer id = VehicleDao.idOwner(db,placa);
        if( id != 0)
            model.setIdOwner(id);

        model.setTypesecurity("A");
        model.setPhotho(photo);
        model.setDate(formatoFecha.format(fechaActual));
        model.setHour(formatohora.format(fechaActual));
        model.setTypeaccess("E");
        model.setCom_id(1);



        if(exists && id != 0 ){

            System.out.println("id Propietario : " + model.getDate());
            System.out.println("id Propietario : " + model.getHour());
            System.out.println("id Propietario : " + model.getCom_id());
            System.out.println("id Propietario : " + model.getCode());
            System.out.println("id Propietario : " + model.getOwn_id());


            Long newRowId = HistoricalAccessDao.createAccess(model,db );

            if(newRowId == -1){

                Toast.makeText(getApplicationContext(),"No se guardó el registro ", Toast.LENGTH_LONG).show();

            }
            pase.setText("Placa Permitida.");

        }else {
            pase.setText("Placa No Permitada.");
        }

        Toast.makeText(getApplicationContext(),"Se guardó el registro ", Toast.LENGTH_LONG).show();



    }

    public byte[] readBytes(Uri uri) throws IOException {


        // this dynamically extends to take the bytes you read

        InputStream inputStream = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();


        // this is storage overwritten on each iteration with bytes

        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        // we need to know how may bytes were read to write them to the byteBuffer

        int len = 0;

        while ((len = inputStream.read(buffer)) != -1) {

            byteBuffer.write(buffer, 0, len);

        }
       // and then we can return your byte array.
        return byteBuffer.toByteArray();

    }




}
