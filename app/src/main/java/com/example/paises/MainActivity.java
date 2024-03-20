package com.example.paises;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.Manifest;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.paises.ml.Banderas;
import com.google.android.gms.tasks.OnFailureListener;


import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;



public class MainActivity extends AppCompatActivity implements  OnFailureListener {

    private static final int REQUEST_GALLERY =  222;
    private static final int REQUEST_CAMERA =  111;
    Bitmap mSelectedImage;
    ImageView mImageView;
    TextView txtResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = findViewById(R.id.image_view);
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA},100);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && null != data) {
            try {
                if (requestCode == REQUEST_CAMERA)
                    mSelectedImage = (Bitmap) data.getExtras().get("data");
                else
                    mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                mImageView.setImageBitmap(mSelectedImage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public void abrirGaleria(View view) {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }

    public void abrirCamara(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void onFailure(@NonNull Exception e) {

    }


    public void PersonalizedModel(View v){
        try {
            String[] etiquetas = {"UY", "SE", "PT", "MX", "GB", "FR", "ES", "CR", "EC", "BR", "CO", "BE", "AR"};
            Banderas model = Banderas.newInstance(getApplicationContext());
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            inputFeature0.loadBuffer(convertirImagenATensorBuffer(mSelectedImage));
            Banderas.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            String resultado = obtenerEtiquetayProbabilidad(etiquetas, outputFeature0.getFloatArray());
            model.close();

            float mayorProbabilidad = obtenerMayorProbabilidad(outputFeature0.getFloatArray());
            if (mayorProbabilidad > 0.8) {
                String nombreNacionalidad = etiquetas[obtenerPosicionMayorProbabilidad(outputFeature0.getFloatArray())];
                Intent intent = new Intent(this, MainActivity2.class);
                intent.putExtra("nombreNacionalidad", nombreNacionalidad);
                startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public ByteBuffer convertirImagenATensorBuffer(Bitmap mSelectedImage){
        Bitmap imagen = Bitmap.createScaledBitmap(mSelectedImage, 224, 224, true);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[224 * 224];
        imagen.getPixels(intValues, 0, imagen.getWidth(), 0, 0, imagen.getWidth(), imagen.getHeight());
        int pixel = 0;
        for(int i = 0; i < imagen.getHeight(); i ++){
            for(int j = 0; j < imagen.getWidth(); j++){
                int val = intValues[pixel++]; // RGB
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
            }
        }
        return byteBuffer;
    }
    public String obtenerEtiquetayProbabilidad(String[] etiquetas, float[] probabilidades){
        float valorMayor = Float.MIN_VALUE;
        int pos = -1;
        for (int i = 0; i < probabilidades.length; i++) {
            if (probabilidades[i] > valorMayor) {
                valorMayor = probabilidades[i];
                pos = i;
            }
        }
        return "Predicción: " + etiquetas[pos] + ", Probabilidad: " +
                (new DecimalFormat("0.00").format(probabilidades[pos] * 100)) + "%";
    }
    private float obtenerMayorProbabilidad(float[] probabilidades) {
        float valorMayor = Float.MIN_VALUE;
        for (float probabilidad : probabilidades) {
            if (probabilidad > valorMayor) {
                valorMayor = probabilidad;
            }
        }
        return valorMayor;
    }
    private int obtenerPosicionMayorProbabilidad(float[] probabilidades) {
        float valorMayor = Float.MIN_VALUE;
        int pos = -1;
        for (int i = 0; i < probabilidades.length; i++) {
            if (probabilidades[i] > valorMayor) {
                valorMayor = probabilidades[i];
                pos = i;
            }
        }
        return pos;
    }

}