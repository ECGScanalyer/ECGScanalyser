package com.ecgscanner.ecgscanner;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class AnalysisActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        ArrayList<Double> data = (ArrayList<Double>) getIntent().getExtras().get("data");

        GraphView graph = (GraphView) findViewById(R.id.graph);
        DataPoint[] dataPoints = new DataPoint[data.size()];
        for(int x = 0; x<data.size(); x++){
            dataPoints[x] = new DataPoint(x/2.0, data.get(x));
        }
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(dataPoints);
//        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[] {
//                dataPoints[0],
//                new DataPoint(1, data.get(1)),
//                new DataPoint(2, data.get(2)),
//                new DataPoint(3, data.get(3)),
//                new DataPoint(4, data.get(4))
//        });
        graph.addSeries(series);
    }
}
