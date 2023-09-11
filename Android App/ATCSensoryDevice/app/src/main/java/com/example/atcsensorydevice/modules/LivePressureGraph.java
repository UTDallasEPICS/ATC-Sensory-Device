package com.example.atcsensorydevice.modules;

import android.util.Log;

import com.example.atcsensorydevice.R;
import com.example.atcsensorydevice.objects.XYPoint;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class LivePressureGraph implements LivePressureCallback{
    LineGraphSeries<DataPoint> dataSeries;
    GraphView livePressurePlot;
    private ArrayList<XYPoint> pointArray;
    private int time = 0;
    private final DataPoint[] newData = new DataPoint[11];

    public LivePressureGraph(GraphView livePressurePlot){
        this.livePressurePlot = livePressurePlot;
        pointArray = new ArrayList<>();
        dataSeries = new LineGraphSeries<>();
        setupLiveGraph();
    }

    private void setupLiveGraph(){
        dataSeries.setThickness(5);
        dataSeries.setColor(R.color.accent);

        //Plot Style
        livePressurePlot.setTitle("Live Pressure Reading (PSI)");
        livePressurePlot.getGridLabelRenderer().setTextSize(25);
        livePressurePlot.getGridLabelRenderer().setVerticalAxisTitleTextSize(20);
        livePressurePlot.getGridLabelRenderer().setPadding(20);
        livePressurePlot.getGridLabelRenderer().setHorizontalAxisTitleTextSize(25);
        livePressurePlot.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        livePressurePlot.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");

        //set Scrollable and Scalable
        livePressurePlot.getViewport().setScalableY(false);
        livePressurePlot.getViewport().setScrollable(false);

        //set manual x bounds
        livePressurePlot.getViewport().setYAxisBoundsManual(true);
        livePressurePlot.getViewport().setMaxY(15.3);
        livePressurePlot.getViewport().setMinY(14.0);

        //set manual y bounds
        livePressurePlot.getViewport().setXAxisBoundsManual(true);
        livePressurePlot.getViewport().setMaxX(10);
        livePressurePlot.getViewport().setMinX(0);
    }

    private void createLiveGraph(){
        pointArray = sortArray(pointArray);
        Log.d("[SIZE]", String.valueOf(pointArray.size()));
        for(int i = 0; i < pointArray.size(); i++){
            try{
                double x = pointArray.get(i).getX();
                double y = pointArray.get(i).getY();
                if(time <= 10 ){
                    dataSeries.appendData(new DataPoint(x,y), true, 15);
                }
                newData[i] = new DataPoint(x,y);
            }catch (IllegalArgumentException e){
                Log.e("[PLOT]", "createLiveGraph: IllegalArgumentException" + e.getMessage());
            }
        }
    }

    private void drawLiveGraph(){
        livePressurePlot.addSeries(dataSeries);
    }

    private ArrayList<XYPoint> sortArray(ArrayList<XYPoint> array){
        int factor = Integer.parseInt(String.valueOf(Math.round(Math.pow(array.size(),2))));
        int m = array.size() - 1;
        int count = 0;

        while (true) {
            m--;
            if (m <= 0) {
                m = array.size() - 1;
            }
            try {
                double tempY = array.get(m - 1).getY();
                double tempX = array.get(m - 1).getX();
                if (tempX > array.get(m).getX()) {
                    array.get(m - 1).setY(array.get(m).getY());
                    array.get(m).setY(tempY);
                    array.get(m - 1).setX(array.get(m).getX());
                    array.get(m).setX(tempX);
                } else if (tempX == array.get(m).getX()) {
                    count++;
                } else if (array.get(m).getX() > array.get(m - 1).getX()) {
                    count++;
                }
                if (count == factor) {
                    break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                break;
            }
        }
        return array;
    }

    @Override
    public void onPressureReceive(float value){
        Log.d("OnPressureReceive", String.valueOf(value));

        for(int i = 0; i < pointArray.size(); i++){
            System.out.printf("%.1f\t", pointArray.get(i).getY());
        }
        System.out.println();
        for(int i = 0; i < pointArray.size(); i++){
            System.out.printf("0%.1f\t", pointArray.get(i).getX());
        }
        System.out.println();


        double x = time;
        double y = (double) value;
        //Log.d("[TIME]", String.valueOf(time));
        if(time > 10){
            //Shift Points one unit to the left
            //Replace last point with new data
            pointArray = shiftPointsLeft(pointArray);
            pointArray.set(pointArray.size()-1, new XYPoint(x-1,y));

            livePressurePlot.removeAllSeries();
            createLiveGraph();
            dataSeries = new LineGraphSeries<>(newData);
            setupLiveGraph();
            drawLiveGraph();
        }else{
            pointArray.add(new XYPoint(x,y));

            createLiveGraph();
            dataSeries = new LineGraphSeries<>();
            setupLiveGraph();
            drawLiveGraph();
            time++;
        }
    }

    private ArrayList<XYPoint> shiftPointsLeft(ArrayList<XYPoint> array){
        for(int i = 0; i < array.size()-1; i++){
            array.set(i, new XYPoint(i, array.get(i+1).getY()));
        }
        return array;
    }
}
