package com.ecgscanner.ecgscanner;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    ImageView imageview;
    Button btnAnalysis;
    Button btnUpload;
    Button btnCamera;

    ArrayList<Double> graphData;
    Bitmap scan;

    int GET_FROM_GALLERY = 2;
    int GET_FROM_CAMERA = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OpenCVLoader.initDebug();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnAnalysis = (Button) findViewById(R.id.btnAnalysis);
        btnUpload = (Button) findViewById(R.id.btnUpload);
        imageview = (ImageView)findViewById(R.id.imageview);

        btnAnalysis.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] { Manifest.permission.CAMERA }, 1);
        }

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, GET_FROM_CAMERA);
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(
                    Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI), GET_FROM_GALLERY);
            }
        });

        btnAnalysis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //send request
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        performPostCall("http://192.168.43.217:3333/areyougonnadie",
                                new HashMap<String, String>(){{put("ecg_signals","[1,2,3]");}});
                    }
                });
                t.run();

                //new activity
                Intent intent = new Intent(getApplicationContext(), AnalysisActivity.class);
                intent.putExtra("data", graphData);
                startActivity(intent);
            }

        });

        btnAnalysis.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                detectEdges(scan);
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==GET_FROM_GALLERY && resultCode == Activity.RESULT_OK) {
            Uri selectedImage = data.getData();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                imageview.setImageBitmap(bitmap);
                scan = bitmap;
                btnAnalysis.setVisibility(View.VISIBLE);
                graphData = getGraphData(bitmap);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if(requestCode==GET_FROM_CAMERA && resultCode == Activity.RESULT_OK) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            imageview.setImageBitmap((bitmap));//can greyscale here if needed
            scan = bitmap;
            btnAnalysis.setVisibility(View.VISIBLE);
            graphData = getGraphData(bitmap);
        }
    }

    public ArrayList<Double> getGraphData(Bitmap bitmap){
        int  w = bitmap.getWidth();
        int  h = bitmap.getHeight();
        ArrayList<Double> xList = new ArrayList<Double>();
        int[][] arr = new int[w][h];
        for (int x =0; x<w;x++){
            ArrayList<Integer> yList = new ArrayList<Integer>();
            for (int y =0; y<h;y++){
                int _color =  bitmap.getPixel(x, y);
                arr[x][y] = _color;
                if (_color == -1){ // if white
                    yList.add(y);
                }
            }
            //check white value ratio
            double ratio = yList.size()/(double)h;
            if (ratio > 0.5 || ratio == 0){
                continue;
            }
            //filter yList for noise
//            ArrayList<Integer> clusterList = new ArrayList<>();
//            for (int i = 0; i<yList.size(); i++){
//
//            }

            //get y value
            if (yList.size() != 0 ){
                double newElement = yList.get(0);
//                if (xList.size() < 10){
//                    int index = 0;
//                    while (h*0.1 >= newElement && newElement >= h*0.9){
//                        newElement = yList.get(index);
//                        index++;
//                    }
//                }else{
//                    if()
//                }


                xList.add(-1.0 * newElement);//calculateAverage
            }else{
//                if (xList.size() != 0) {
//                    xList.add(xList.get(-1));
//                }else{
//                    continue;
//                }
                continue;
            }
        }

        //normalising results
        if (xList.size() == 0){
            return xList;
        }
        Double min = xList.get(0);
        for (double i : xList){
            min = min < i ? min : i;
        }
        for(int j = 0; j<xList.size(); j++){
            xList.set(j, xList.get(j) - min);
        }
        return xList;
    }

    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    private double calculateAverage(List <Integer> marks) {
        Integer sum = 0;
        if(!marks.isEmpty()) {
            for (Integer mark : marks) {
                sum += mark;
            }
            return sum.doubleValue() / marks.size();
        }
        return sum;
    }

    private void detectEdges(Bitmap bitmap) {
        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);

        Mat edges = new Mat(rgba.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4);
        Imgproc.Canny(edges, edges, 100, 200);

        // Don't do that at home or work it's for visualization purpose.
//        BitmapHelper.showBitmap(this, bitmap, imageView);
        Bitmap resultBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edges, resultBitmap);
        imageview.setImageBitmap(resultBitmap);
        scan = resultBitmap;
        graphData = getGraphData(resultBitmap);
//        BitmapHelper.showBitmap(this, resultBitmap, detectEdgesImageView)
    }

    public String  performPostCall(String requestURL,
                                   HashMap<String, String> postDataParams) {
        URL url;
        String response = "";
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            // param=value&param2=value
            // "[[float1, float2, float3,...]]"
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode=conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }
            else {
                response="";
            }
        } catch (Exception e) {

            e.printStackTrace();
        }
        return response;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }
}
