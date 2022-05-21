package com.example.android.weardatacollector;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvManager {
    protected Context context;
    String timestamp;
    String Activity;
    File file;
    String ppgfilePath;
    String accfilePath;
    String gyroFilePath;
    String storageDir = "/wearDataCollector/";

    //Constructor gets caller Context and the path

    public CsvManager(Context context, String timestamp, String Activity) {

        this.context = context;
        this.timestamp = timestamp;
        this.Activity = Activity;
        file = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() + storageDir + timestamp);

        //Delete the Folder if it already exists
        try {
            if (file.exists()) {
                if (deleteDirectory(file)) {
                    Log.d("CsvManager", "Folder Succesfully deleted");
                } else {
                    Log.e("CsvManager", "Folder could not be deleted");
                }
            }
        } catch (Exception e) {
            Log.e("App", "Exception while deleting file " + e.getMessage());
        }

        //create the folder
        file.mkdirs();

        //create ppgfile path
        ppgfilePath = (Environment.getExternalStorageDirectory().getAbsolutePath() + storageDir + timestamp + "/ppg_" + Activity + "_" + timestamp + ".csv");


        //To delete the file if it already exists
        try {
            File filed = new File(ppgfilePath);
            if (filed.exists()) {
                filed.delete();
                Log.d("CsvManager", "File Succesfully deleted");
            }
        } catch (Exception e) {
            Log.e("App", "Exception while deleting file " + e.getMessage());
        }

        try {
            CSVWriter csvWriter = new CSVWriter(new FileWriter(ppgfilePath, true));
            String row[] = new String[]{"t", "x"};
            csvWriter.writeNext(row);
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //create accfile path
        accfilePath = (Environment.getExternalStorageDirectory().getAbsolutePath() + storageDir + timestamp + "/acc_" + Activity + "_" + timestamp + ".csv");

        //To delete the file if it already exists
        try {
            File filed = new File(accfilePath);
            if (filed.exists()) {
                filed.delete();
                Log.d("CsvManager", "File Succesfully deleted");
            }
        } catch (Exception e) {
            Log.e("App", "Exception while deleting file " + e.getMessage());
        }

        try {
            CSVWriter csvWriter = new CSVWriter(new FileWriter(accfilePath, true));
            String row[] = new String[]{"t", "x", "y", "z", "bx", "by", "bz"};
            csvWriter.writeNext(row);
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //create accfile path
        gyroFilePath = (Environment.getExternalStorageDirectory().getAbsolutePath() + storageDir + timestamp + "/gyro_" + Activity + "_" + timestamp + ".csv");

        //To delete the file if it already exists
        try {
            File filed = new File(gyroFilePath);
            if (filed.exists()) {
                filed.delete();
                Log.d("CsvManager", "File Succesfully deleted");
            }
        } catch (Exception e) {
            Log.e("App", "Exception while deleting file " + e.getMessage());
        }

        try {
            CSVWriter csvWriter = new CSVWriter(new FileWriter(gyroFilePath, true));
            String row[] = new String[]{"t", "axisX", "axisY", "axisZ", "bx", "by", "bz"};
            csvWriter.writeNext(row);
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void savePPG(List<int[]> data) {
        try {
            CSVWriter csvWriter = new CSVWriter(new FileWriter(ppgfilePath, true));

            for (int i = 0; i < data.size(); i++) {
                String row[] = new String[2];
                row[0] = String.valueOf(data.get(i)[0]);
                row[1] = String.valueOf(data.get(i)[1]);
                csvWriter.writeNext(row);
            }
            csvWriter.close();
            Log.d("CsvManager", "ppgFile Succesfully created");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAccelerometer(List<float[]> data) {
        try {
            CSVWriter csvWriter = new CSVWriter(new FileWriter(accfilePath, true));
            for (int i = 0; i < data.size(); i++) {
                String row[] = new String[7];
                row[0] = String.valueOf(data.get(i)[0]);//t
                row[1] = String.valueOf(data.get(i)[1]);//x
                row[2] = String.valueOf(data.get(i)[2]);//y
                row[3] = String.valueOf(data.get(i)[3]);//z
                row[4] = String.valueOf(data.get(i)[4]);//z
                row[5] = String.valueOf(data.get(i)[5]);//z
                row[6] = String.valueOf(data.get(i)[6]);//z
                csvWriter.writeNext(row);
            }
            csvWriter.close();
            Log.d("CsvManager", "Accelerometer File Successfully created");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveGyroScope(List<float[]> data) {
        try {
            CSVWriter csvWriter = new CSVWriter(new FileWriter(gyroFilePath, true));
            for (int i = 0; i < data.size(); i++) {
                String row[] = new String[7];
                row[0] = String.valueOf(data.get(i)[0]);//t
                row[1] = String.valueOf(data.get(i)[1]);//x
                row[2] = String.valueOf(data.get(i)[2]);//y
                row[3] = String.valueOf(data.get(i)[3]);//z
                row[4] = String.valueOf(data.get(i)[4]);//z
                row[5] = String.valueOf(data.get(i)[5]);//z
                row[6] = String.valueOf(data.get(i)[6]);//z
                csvWriter.writeNext(row);
            }
            csvWriter.close();
            Log.d("CsvManager", "Gyroscope File Successfully created");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean deleteDirectory(File Path) {
        if (Path.exists()) {
            File[] files = Path.listFiles();
            if (files == null) {
                return true;
            }
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (Path.delete());
    }
}
