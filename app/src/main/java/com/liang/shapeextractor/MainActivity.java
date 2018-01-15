package com.liang.shapeextractor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private Button start;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        start = (Button)findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start.setEnabled(true);
                File file = new File("/sdcard/GRD00000.bin");
                try{
                    ShapeExtractor util = new ShapeExtractor(file,169, 86);
//                    util.printMatrix();
                    util.trim();
                }catch (Exception e){
                    e.printStackTrace();
                }
                start.setEnabled(false);

            }
        });
    }
}
