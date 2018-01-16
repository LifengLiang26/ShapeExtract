package com.liang.shapeextractor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "ShapeExtractor";
    private Button start;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start = (Button)findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start.setEnabled(false);
                File file = new File("/sdcard/GRD00000.bin");
                try{
                    ShapeExtractor extractor = new ShapeExtractor(file,122, 56, 1);
//                    util.printMatrix();
                    Log.d(TAG, "Done");
                }catch (Exception e){
                    e.printStackTrace();
                }
                start.setEnabled(true);

            }
        });
    }
}
