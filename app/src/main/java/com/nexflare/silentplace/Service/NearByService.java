package com.nexflare.silentplace.Service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.nexflare.silentplace.DataBase.SilentPlaceDB;
import com.nexflare.silentplace.Interface.DistanceMatrixApi;
import com.nexflare.silentplace.Model.DistanceMatrixResult;
import com.nexflare.silentplace.Model.PlaceDetail;
import com.nexflare.silentplace.R;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NearByService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public NearByService() {
    }

    GoogleApiClient mGoogleApiClient;
    ArrayList<PlaceDetail> placeDetailArray;
    Retrofit retrofit;
    SilentPlaceDB database;
    Double latitude = null, longitude = null;
    AudioManager audioManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Location", "onCreate: ");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Location", "onStartCommand: ");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        database = new SilentPlaceDB(this);
        placeDetailArray = new ArrayList<>();
        return START_STICKY;
    }

    private void checkIfNearByPlace() {
        placeDetailArray = database.getAllPlaces();
        DistanceMatrixApi api = retrofit.create(DistanceMatrixApi.class);
        if (latitude != null)
            for (int i = 0; i < placeDetailArray.size(); i++) {
                String destination = placeDetailArray.get(i).getLatLng().latitude + "," + placeDetailArray.get(i).getLatLng().longitude;
                Log.d("TAGGER", "checkIfNearByPlace: " + destination);
                Log.d("TAGGER", "checkIfNearByPlace: " + getString(R.string.API_KEY));
                api.getDistance(latitude + "," + longitude, destination, getString(R.string.API_KEY)).enqueue(new Callback<DistanceMatrixResult>() {
                    @Override
                    public void onResponse(Call<DistanceMatrixResult> call, Response<DistanceMatrixResult> response) {
                        long distance = Long.parseLong(response.body().getRows().get(0).getElements().get(0).getDistance().getValue());
                        Log.d("TAGGER", "onResponse: " + distance);
                        if (distance <= 100) {
                            silentPhone();
                        }
                    }

                    @Override
                    public void onFailure(Call<DistanceMatrixResult> call, Throwable t) {
                        Log.d("TAGGER", "onFailure: ");
                    }
                });
            }
    }

    private void silentPhone() {
        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest request = LocationRequest.create()
                .setInterval(15000)
                .setFastestInterval(7500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, new com.google.android.gms.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    latitude=location.getLatitude();
                    longitude=location.getLongitude();
                    checkIfNearByPlace();
                }
            });
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

}
