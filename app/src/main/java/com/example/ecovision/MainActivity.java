package com.example.ecovision;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private ImageView imageView;
    private TextView resultTextView;
    private Interpreter tflite;
    private final int MODEL_INPUT_SIZE = 224;
    private DataType inputDataType;
    private TextToSpeech textToSpeech;
    private TextView infoTextView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        resultTextView = findViewById(R.id.resultTextView);
        infoTextView = findViewById(R.id.infoTextView);
        infoTextView.setVisibility(View.GONE); // Oculta el infoTextView al iniciar
        Button captureButton = findViewById(R.id.captureButton);

        // Inicializar TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("es", "ES"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Idioma no soportado");
                    Toast.makeText(MainActivity.this, "Idioma no soportado", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("TTS", "Inicialización fallida");
                Toast.makeText(MainActivity.this, "Error en síntesis de voz", Toast.LENGTH_SHORT).show();
            }
        });



        // Verificar permisos de cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }

        // Cargar modelo TFLite
        try {
            tflite = new Interpreter(Utils.loadModelFile(this, "model.tflite"));
            // Obtener especificaciones del modelo
            Tensor inputTensor = tflite.getInputTensor(0);
            inputDataType = inputTensor.dataType();
            Log.d("ModelSpecs", "Formato de entrada: " + inputDataType.toString() +
                    " | Dimensión: " + Arrays.toString(inputTensor.shape()));
        } catch (Exception e) {
            Toast.makeText(this, "Error cargando modelo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("ModelLoading", "Error carga modelo", e);
        }

        captureButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            }
        });
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "No se encontró aplicación de cámara", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (data != null && data.getExtras() != null) {
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                if (imageBitmap != null) {
                    imageView.setImageBitmap(imageBitmap);
                    classifyImage(imageBitmap);
                }
            }
        }
    }
    private void classifyImage(Bitmap bitmap) {
        try {
            if (tflite == null) {
                Toast.makeText(this, "Modelo no inicializado", Toast.LENGTH_SHORT).show();
                return;
            }

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, false);
            ByteBuffer inputBuffer = createInputBuffer(scaledBitmap);

            // Obtener especificaciones del tensor de salida
            Tensor outputTensor = tflite.getOutputTensor(0);
            DataType outputDataType = outputTensor.dataType();
            Log.d("ModelSpecs", "Formato de salida: " + outputDataType.toString());

            // Buffer de salida según el tipo de dato
            if (outputDataType == DataType.UINT8) {
                byte[][] output = new byte[1][6]; // Ajusta según tu modelo
                tflite.run(inputBuffer, output);
                processResults(output[0]);
            } else {
                float[][] output = new float[1][6];
                tflite.run(inputBuffer, output);
                processResults(output[0]);
            }

        } catch (Exception e) {
            Log.e("Classification", "Error: ", e);
            runOnUiThread(() -> Toast.makeText(this,
                    "Error en clasificación: " + e.getMessage(),
                    Toast.LENGTH_LONG).show());
        }
    }

    private ByteBuffer createInputBuffer(Bitmap bitmap) {
        // Determinar bytes por canal según el tipo de dato de entrada
        int bytesPerChannel = inputDataType == DataType.FLOAT32 ? 4 : 1;
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(
                MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3 * bytesPerChannel
        );
        inputBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[MODEL_INPUT_SIZE * MODEL_INPUT_SIZE];
        bitmap.getPixels(pixels, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE);

        // Llenar el buffer según el tipo de dato
        for (int pixel : pixels) {
            if (inputDataType == DataType.FLOAT32) {
                // Normalizar valores a float [0,1]
                inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f); // R
                inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);  // G
                inputBuffer.putFloat((pixel & 0xFF) / 255.0f);         // B
            } else {
                // Usar valores byte [0,255]
                inputBuffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                inputBuffer.put((byte) ((pixel >> 8) & 0xFF));  // G
                inputBuffer.put((byte) (pixel & 0xFF));         // B
            }
        }

        return inputBuffer;
    }

    private void processResults(byte[] results) {
        // Convertir bytes a valores numéricos (0-255)
        float[] floatResults = new float[results.length];
        for (int i = 0; i < results.length; i++) {
            floatResults[i] = (results[i] & 0xFF) / 255.0f;
        }
        processResults(floatResults);
    }

    private void processResults(float[] results) {
        String[] labels = {"Plástico", "Papel", "Vidrio", "Metal", "Carton", "Basura"};
        int maxIndex = 0;
        for (int i = 0; i < results.length; i++) {
            if (results[i] > results[maxIndex]) maxIndex = i;
        }

        String message;
        String info;
        switch (labels[maxIndex]) {
            case "Plástico":
                message = "Depositar en el contenedor amarillo";
                info = "El plástico es un material reciclable usado en envases y botellas. Se recomienda limpiarlo antes de desecharlo.";
                break;
            case "Papel":
                message = "Depositar en el Contenedor azul";
                info = "El papel se recicla para fabricar nuevos productos como libretas y cartón. Evita mezclarlo con materiales húmedos.";
                break;
            case "Vidrio":
                message = "Depositar en el Contenedor verde";
                info = "El vidrio puede reciclarse muchas veces sin perder calidad. Se recomienda enjuagar antes de desechar.";
                break;
            case "Metal":
                message = "Depositar en el Contenedor gris";
                info = "El metal, como las latas de aluminio, se recicla para fabricar nuevos envases y productos metálicos.";
                break;
            case "Carton":
                message = "Depositar en el Contenedor azul";
                info = "El cartón reciclado se usa para crear empaques y cajas. Asegúrate de que no esté contaminado con grasa o líquidos.";
                break;
            case "Basura":
                message = "Depositar en el Contenedor gris 🗑";
                info = "Residuos no reciclables como servilletas sucias o productos mixtos. Considera reducir su uso.";
                break;
            default:
                message = "No reciclable ❌";
                info = "Este objeto no se puede reciclar. Intenta reutilizarlo o reducir su uso en el futuro.";
        }

        resultTextView.setText(message);
        infoTextView.setVisibility(View.VISIBLE);
        infoTextView.setText(info);
        speakMessage(message + ". " + info);
    }

    private void speakMessage(String message) {
        if (textToSpeech != null) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
            textToSpeech.setSpeechRate(0.9f);
            textToSpeech.setPitch(1.1f);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }
}