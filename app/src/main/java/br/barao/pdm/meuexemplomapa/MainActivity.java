package br.barao.pdm.meuexemplomapa;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private static final int REQUEST_PERMISSOES = 1;
    private GoogleMap map;
    private List<Marker> listaMarcadores = new ArrayList<>();
    private TextView tvLatitude;
    private TextView tvLongitude;

    /*
     * Variávei utilizadas para geolocalização
     */
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Location ultimaLocalizacao;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inicializaComponentes();
        verificaPermissoes();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        paraGeolocalizacao();
    }

    private void verificaPermissoes()
    {
        boolean ACCESS_COARSE_LOCATION = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean ACCESS_FINE_LOCATION = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        //
        if (ACCESS_COARSE_LOCATION && ACCESS_FINE_LOCATION)
        {
            inicializaMapa();
            inicializaGeolocalizacao();
        }
        else
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSOES);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_PERMISSOES:
                for (int grant : grantResults)
                {
                    if (grant != PackageManager.PERMISSION_GRANTED)
                    {
                        Toast.makeText(this, R.string.PermissoesNaoConcedidas, Toast.LENGTH_LONG).show();
                        this.finish();
                        return;
                    }
                }
                inicializaMapa();
                inicializaGeolocalizacao();
                break;
        }
    }

    private void inicializaMapa()
    {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback()
        {
            @Override
            public void onMapReady(GoogleMap googleMap)
            {
                map = googleMap;
                configuraMapa();
            }
        });
    }

    private void configuraMapa()
    {
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            map.setMyLocationEnabled(true);
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener()
        {
            @Override
            public void onMapClick(LatLng latLng)
            {
                adicionaMarcador(latLng.latitude, latLng.longitude);
            }
        });
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {
            @Override
            public boolean onMarkerClick(Marker marker)
            {
                if (listaMarcadores.remove(marker))
                {
                    marker.remove();
                    return true;
                }
                else
                    return false;
            }
        });
    }

    private void adicionaMarcador(Double latitude, Double longitude)
    {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(latitude, longitude));
        markerOptions.draggable(false);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        markerOptions.title("Título");
        Marker marker = map.addMarker(markerOptions);
        marker.setTag(new Date());
        listaMarcadores.add(marker);
    }

    private void inicializaComponentes()
    {
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
    }

    /*
     * Método responsável por inicializar a geolocalização (captura de coordenada)
     */
    private void inicializaGeolocalizacao()
    {
        //
        //Obtem acesso a uma instancia de um utilizado para ter acesso a geolocalização
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        //
        //Configura como o GPS irá trabalhar para o seu APP
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(0);
        locationRequest.setFastestInterval(0);
        //
        //Cria objeto que irá receber as atualizações de localização
        locationCallback = new LocationCallback()
        {
            @Override
            public void onLocationResult(LocationResult locationResult)
            {
                super.onLocationResult(locationResult);
                if (locationResult != null)
                {
                    ultimaLocalizacao = locationResult.getLastLocation();
                    atualizaInformacoesLocalizacao();
                }
            }
        };
        //
        //Inicia a captura de localização
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /*
     * Interrompe a atualização de coordenadas
     */
    private void paraGeolocalizacao()
    {
        try
        {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        catch (Exception ignored)
        {
        }
    }

    private void atualizaInformacoesLocalizacao()
    {
        tvLatitude.setText(String.valueOf(ultimaLocalizacao.getLatitude()));
        tvLongitude.setText(String.valueOf(ultimaLocalizacao.getLongitude()));
    }
}