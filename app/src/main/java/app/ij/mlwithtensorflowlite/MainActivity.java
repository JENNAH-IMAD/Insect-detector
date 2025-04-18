package app.ij.mlwithtensorflowlite;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import app.ij.mlwithtensorflowlite.ml.Model;


public class MainActivity extends AppCompatActivity {

    Button camera, gallery;
    ImageView imageView;
    TextView result;
    int imageSize = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = findViewById(R.id.button);
        gallery = findViewById(R.id.button2);

        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 3);
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(cameraIntent, 1);
            }
        });
    }

    public void classifyImage(Bitmap image){
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
            for(int i = 0; i < imageSize; i ++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            String[] classes = {
                    "rice leaf roller", "rice leaf caterpillar", "paddy stem maggot", "asiatic rice borer",
                    "yellow rice borer", "rice gall midge", "Rice Stemfly", "brown plant hopper",
                    "white backed plant hopper", "small brown plant hopper", "rice water weevil",
                    "rice leafhopper", "grain spreader thrips", "rice shell pest", "grub",
                    "mole cricket", "wireworm", "white margined moth", "black cutworm",
                    "large cutworm", "yellow cutworm", "red spider", "corn borer",
                    "army worm", "aphids", "Potosiabre vitarsis", "peach borer",
                    "english grain aphid", "green bug", "bird cherry-oataphid", "wheat blossom midge",
                    "penthaleus major", "longlegged spider mite", "wheat phloeothrips", "wheat sawfly",
                    "cerodonta denticornis", "beet fly", "flea beetle", "cabbage army worm",
                    "beet army worm", "Beet spot flies", "meadow moth", "beet weevil",
                    "sericaorient alismots chulsky", "alfalfa weevil", "flax budworm", "alfalfa plant bug",
                    "tarnished plant bug", "Locustoidea", "lytta polita", "legume blister beetle",
                    "blister beetle", "therioaphis maculata Buckton", "odontothrips loti", "Thrips",
                    "alfalfa seed chalcid", "Pieris canidia", "Apolygus lucorum", "Limacodidae",
                    "Viteus vitifoliae", "Colomerus vitis", "Brevipoalpus lewisi McGregor", "oides decempunctata",
                    "Polyphagotars onemus latus", "Pseudococcus comstocki Kuwana", "parathrene regalis", "Ampelophaga",
                    "Lycorma delicatula", "Xylotrechus", "Cicadella viridis", "Miridae",
                    "Trialeurodes vaporariorum", "Erythroneura apicalis", "Papilio xuthus", "Panonchus citri McGregor",
                    "Phyllocoptes oleiverus ashmead", "Icerya purchasi Maskell", "Unaspis yanonensis", "Ceroplastes rubens",
                    "Chrysomphalus aonidum", "Parlatoria zizyphus Lucus", "Nipaecoccus vastalor", "Aleurocanthus spiniferus",
                    "Tetradacus c Bactrocera minax", "Dacus dorsalis(Hendel)", "Bactrocera tsuneonis", "Prodenia litura",
                    "Adristyrannus", "Phyllocnistis citrella Stainton", "Toxoptera citricidus", "Toxoptera aurantii",
                    "Aphis citricola Vander Goot", "Scirtothrips dorsalis Hood", "Dasineura sp", "Lawana imitata Melichar",
                    "Salurnis marginella Guerr", "Deporaus marginatus Pascoe", "Chlumetia transversa", "Mango flat beak leafhopper",
                    "Rhytidodera bowrinii white", "Sternochetus frigidus", "Cicadellidae"
            };
            result.setText(classes[maxPos]);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == 3){
                Bitmap image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }else{
                Uri dat = data.getData();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}