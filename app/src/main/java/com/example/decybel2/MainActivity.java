package com.example.decybel2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
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
import static com.mapbox.mapboxsdk.style.expressions.Expression.linear;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgb;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    /*
     * Deklaracja i definicja zmiennych globalnych
     */

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
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

    // Lokalizacja
    private String latitude = "", longitude = "";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    /*
     * Deklaracja i definicja funkcji
     */

    // Funkcja wywoływana za każdym razem, kiedy użytkownik przyzna lub nie przyzna aplikacji dostępu do poszczególnych narzędzi
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Gdy dostęp zostanie przyznany
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // do sth
                } else {
                    Toast.makeText(getApplicationContext(), "Aby aplikacja mogła w pełni funkcjonować, wymagany jest dostęp do mikrofonu", Toast.LENGTH_LONG).show();
//                    ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
                }
            }
            case REQUEST_INTERNET_PERMISSION: {
                if (grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // do sth
                } else {
                    Toast.makeText(getApplicationContext(), "Aby aplikacja mogła w pełni funkcjonować, wymagany jest dostęp do internetu", Toast.LENGTH_LONG).show();
                }
            }
            case REQUEST_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    // do sth
                } else {
                    Toast.makeText(getApplicationContext(), "Aby aplikacja mogła w pełni funkcjonować, wymagany jest dostęp do lokalizacji", Toast.LENGTH_LONG).show();
                }
            }
            case REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                    // do sth
                } else {
                    Toast.makeText(getApplicationContext(), "Aby aplikacja mogła w pełni funkcjonować, wymagany jest dostęp do lokalizacji", Toast.LENGTH_LONG).show();
                }
            }
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
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    // Obiekt odpowiedzialny za monitorowanie natężenia dźwięku i lokalizacji użytkownika
    private class Monitor extends TimerTask {
        private MediaRecorder recorder;

        public Monitor(MediaRecorder recorder) {
            this.recorder = recorder;
        }

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

                    try {
                        intensity = Double.toString(20 * Math.log10((double) Math.abs(recorder.getMaxAmplitude())));  // Pobranie natężenia dżwięku
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

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

//        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        // Lokalizacja
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Przygotowanie do monitorowania natężenia dźwięku

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);  // Ustawienie mikrofonu jako źródła dźwięku
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // MPEG_2_TS
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioSamplingRate(44100);
            recorder.setAudioEncodingBitRate(96000);
            recorder.setOutputFile("/dev/null");
            try {
                recorder.prepare();
                recorder.start();
            } catch (IOException e) {
                Timber.e("prepare() failed: Błąd metody prepare()");
            } catch (Throwable t) {
                t.printStackTrace();
            }

            // Rozpoczęcie monitorowania natężenia dźwięku i lokalizacji użytkownika co określony okres czasu
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new Monitor(recorder), 0, 15000);
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
                        circleColor(interpolate(linear(), get("loudness"),
                                stop(0, rgb(0, 208, 79)),
                                stop(90, rgb(243, 148, 47)),
                                stop(150, rgb(255, 41, 28))
                        )),
                        circleRadius(interpolate(exponential(1.0f), get("loudness"),
                                stop(0, 0f),
                                stop(1, 1f),
                                stop(110, 3f)
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
    }
}