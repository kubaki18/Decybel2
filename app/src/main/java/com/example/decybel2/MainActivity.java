package com.example.decybel2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.sources.VectorSource;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    /*
     * Deklaracja i definicja zmiennych globalnych
     */

    Timer timer;

    // MapView
    private MapView mapView;

    // Monitorowanie dźwięku
    private String intensity = "";
    MediaRecorder recorder;

    // Dostęp do narzędzi
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private static final int REQUEST_INTERNET_PERMISSION = 2;
    private static final int REQUEST_COARSE_LOCATION = 3;
    private static final int REQUEST_FINE_LOCATION = 4;
    private boolean permissionToRecord = false;
    private boolean permissionInternet = false;
    private boolean permissionCoarseLocation = false;
    private boolean permissionFineLocation = false;
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

    // Lokalizacja
    private String latitude = "", longitude = "";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Tagi
    private static final String LOG_TAG_MEDIARECORDER = "MEDIARECORDER", LOG_TAG_LOCATION = "LOCATION", LOG_VOLLEY = "VOLLEY";

    /*
     * Deklaracja i definicja funkcji
     */

    // Funkcja wywoływana za każdym razem, kiedy użytkownik przyzna lub nie przyzna aplikacji dostępu do poszczególnych narzędzi
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Gdy dostęp zostanie przyznany
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            case REQUEST_INTERNET_PERMISSION:
                permissionInternet = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                break;
            case REQUEST_COARSE_LOCATION:
                permissionCoarseLocation = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                break;
            case REQUEST_FINE_LOCATION:
                permissionFineLocation = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                break;
        }

        // Gdy dane dostęp nie zostanie przyznany
        if (!permissionToRecord) {
            Toast.makeText(getApplicationContext(), "Aby aplikacja mogła w pełni funkcjonować, wymagany jest dostęp do mikrofonu", Toast.LENGTH_LONG).show();
        }
        if (!permissionInternet) {
            Toast.makeText(getApplicationContext(), "Aby aplikacja mogła w pełni funkcjonować, wymagany jest dostęp do internetu", Toast.LENGTH_LONG).show();
        }
        if (!permissionCoarseLocation) {
            Toast.makeText(getApplicationContext(), "Aby aplikacja mogła w pełni funkcjonować, wymagany jest dostęp do lokalizacji", Toast.LENGTH_LONG).show();
        }
        if (!permissionFineLocation) {
            Toast.makeText(getApplicationContext(), "Aby aplikacja mogła w pełni funkcjonować, wymagany jest dostęp do lokalizacji", Toast.LENGTH_LONG).show();
        }
    }


    // Lokalizacja
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(0);
        locationRequest.setFastestInterval(0);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) && shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(getApplicationContext(), "Aby aplikacja mogła w pełni funkcjonować, wymagany jest dostęp do lokalizacji", Toast.LENGTH_LONG).show();
                }
            }
            ActivityCompat.requestPermissions(this, permissions, REQUEST_COARSE_LOCATION);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_FINE_LOCATION);
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    // Nagrywanie dźwięku
    private void startRecording() {
        // Przygotowanie do monitorowania natężenia dźwięku
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);  // Ustawienie mikrofonu jako źródła dźwięku
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile("/dev/null");
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        try {
            recorder.prepare();
        } catch (IOException e) {
            Timber.e("prepare() failed: Błąd metody prepare()");
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    // Obiekt odpowiedzialny za monitorowanie natężenia dźwięku i lokalizacji użytkownika
    private class Monitor extends TimerTask {
        private final MediaRecorder recorder;

        public Monitor(MediaRecorder recorder) {
            this.recorder = recorder;
        }

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    // Lokalizacja
                    locationCallback = new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            if (locationResult == null) {
                                Timber.e("LocationResult is null");
                                Toast.makeText(getApplicationContext(), "Aby aplikacja mogła w pełni funkcjonować, zezwól aplikacji na pobieranie Twojej lokalizacji", Toast.LENGTH_LONG).show();
                                return;
                            }
                            for (Location location : locationResult.getLocations()) {
                                latitude = Double.toString(location.getLatitude());
                                longitude = Double.toString(location.getLongitude());
                            }
                        }
                    };

                    startLocationUpdates();  // pobranie lokalizacji użytkownika

                    intensity = Double.toString(20 * Math.log10((double) Math.abs(recorder.getMaxAmplitude())));  // Pobranie natężenia dżwięku

                    // Wysłanie danych do serwera przy pomocy biblioteki Volley
                    String url = "http://kubaki18.pythonanywhere.com/" + intensity + "/" + longitude + "/" + latitude;
                    RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                    StringRequest output = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Timber.i(response);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError e) {
                            Timber.e("Błąd biblioteki Volley");
                        }
                    }) {
                        @Override
                        protected Map<String, String> getParams() {
                            Map<String, String> parameters = new HashMap<String, String>();
                            try {
                                parameters.put("intensity", intensity);
                                parameters.put("latitude", latitude);
                                parameters.put("longitude", longitude);
                            } catch (IllegalArgumentException e) {
                                Timber.e("IllegalArgumentException: Błąd biblioteki Volley");
                            }
                            return parameters;
                        }

                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            Map<String, String> parameters = new HashMap<String, String>();
                            parameters.put("Content-Type", "application/x-www-form-urlencoded");
                            return parameters;
                        }
                    };
                    queue.add(output);

                }
            });
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.access_token));

        setContentView(R.layout.activity_main);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Lokalizacja
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Przygotowanie do monitorowania natężenia dźwięku
            startRecording();

            // Rozpoczęcie monitorowania natężenia dźwięku i lokalizacji użytkownika co określony okres czasu
            timer = new Timer();
            timer.scheduleAtFixedRate(new Monitor(recorder), 0, 5000);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    Toast.makeText(getApplicationContext(), "Aby aplikacja mogła w pełni funkcjonować, wymagany jest dostęp mikrofonu", Toast.LENGTH_LONG).show();
                }
            }
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        mapboxMap.setStyle(Style.DARK, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                VectorSource vectorSource = new VectorSource(
                        "loudness-source",
                        "http://api.mapbox.com/v4/kubaki18.loudness.json?access_token=" + getString(R.string.access_token)
                );
                style.addSource(vectorSource);
                CircleLayer circleLayer = new CircleLayer("loudness", "loudness-source");
                circleLayer.setSourceLayer("loudness");
                circleLayer.withProperties(
                        circleOpacity(0.6f),
                        circleColor(Color.parseColor("#ffffff")),
                        circleRadius(interpolate(exponential(1.0f), get("loudness"),
                                stop(0, 0f),
                                stop(1, 1f),
                                stop(110, 11f)
                        ))
                );
                style.addLayer(circleLayer);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        if(recorder != null) {
            stopRecording();
        }
        if(timer != null) {
            timer.cancel();
            timer.purge();
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if(recorder != null) {
            stopRecording();
        }
        if(timer != null) {
            timer.cancel();
            timer.purge();
        }
    }
}