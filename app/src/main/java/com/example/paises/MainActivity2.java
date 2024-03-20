package com.example.paises;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;



public class MainActivity2 extends AppCompatActivity implements OnMapReadyCallback {
    private TextView txtResultName;
    private TextView txtinformacion;
    private ImageView imgPais;
    private String nombreNacionalidad;
    private GoogleMap mapa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        txtResultName = findViewById(R.id.txtResultado);
        imgPais = findViewById(R.id.ImagenPais);
        nombreNacionalidad = getIntent().getStringExtra("nombreNacionalidad");
        txtResultName.setText(nombreNacionalidad);
        cambiarNombreEnURL(nombreNacionalidad);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }
    private void cambiarNombreEnURL(String nombreNacionalidad) {
        String infoUrl = "http://www.geognos.com/api/en/countries/info/" + nombreNacionalidad + ".json";
        new GetCountryInfoTask().execute(infoUrl);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapa = googleMap;
        mapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mapa.getUiSettings().setZoomControlsEnabled(true);
        CameraUpdate camUpd1 = CameraUpdateFactory.newLatLngZoom(new LatLng(-1.0126, -79.4696), 20);
        mapa.moveCamera(camUpd1);

    }

    private class GetCountryInfoTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            try {
                URL url = new URL(strings[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                result = stringBuilder.toString();
                reader.close();
                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                txtinformacion = findViewById(R.id.txtinfo);
                JSONObject jsonObject = new JSONObject(s);
                JSONObject countryInfo = jsonObject.getJSONObject("Results");
                String countryName = countryInfo.getString("Name");
                txtResultName.setText(countryName);
                JSONArray geoPtArray = countryInfo.getJSONArray("GeoPt");
                double latitude = geoPtArray.getDouble(0);
                double longitude = geoPtArray.getDouble(1);
                LatLng countryLocation = new LatLng(latitude, longitude);
                CameraUpdate countryLocationUpdate = CameraUpdateFactory.newLatLngZoom(countryLocation, 4);
                mapa.moveCamera(countryLocationUpdate);

                JSONObject geoRectangle = countryInfo.getJSONObject("GeoRectangle");
                double west = geoRectangle.getDouble("West");
                double east = geoRectangle.getDouble("East");
                double north = geoRectangle.getDouble("North");
                double south = geoRectangle.getDouble("South");
                PolylineOptions lineas = new PolylineOptions()
                        .add(new LatLng(north, west))
                        .add(new LatLng(north, east))
                        .add(new LatLng(south, east))
                        .add(new LatLng(south, west))
                        .add(new LatLng(north, west));

                lineas.width(8);
                lineas.color(Color.RED);

                mapa.addPolyline(lineas);

                String flagUrl = "http://www.geognos.com/api/en/countries/flag/" + nombreNacionalidad + ".png";
                new GetFlagImageTask().execute(flagUrl);
                JSONObject capitalInfo = countryInfo.getJSONObject("Capital");
                String capitalName = capitalInfo.getString("Name");
                JSONObject countryCodesInfo = countryInfo.getJSONObject("CountryCodes");
                String iso2Code = countryCodesInfo.getString("iso2");
                String iso3Code = countryCodesInfo.getString("iso3");
                String fipsCode = countryCodesInfo.getString("fips");
                int isoNValue = countryCodesInfo.getInt("isoN");

                txtinformacion.setText("Capital: " + capitalName + "\n" + "CODE ISO 2: " + iso2Code + "\n" +"CODE ISO NUM: " + isoNValue +
                        "\n" + "CODE ISO 3: " + iso3Code + "\n" + "CODE FIPS: " + fipsCode + "\n");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    private class GetFlagImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap bitmap = null;
            try {
                URL url = new URL(strings[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream inputStream = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            imgPais.setImageBitmap(bitmap);
        }

    }
}