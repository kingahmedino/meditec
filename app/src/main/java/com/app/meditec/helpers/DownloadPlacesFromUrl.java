package com.app.meditec.helpers;

import android.util.Log;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadPlacesFromUrl {
    public PlaceInfoResponse readUrl(String myUrl) throws IOException {
        String data = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;

        try {
            URL url = new URL(myUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();

            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            data = stringBuilder.toString();
            bufferedReader.close();
        } catch (IOException e) {
            Log.i("Download Url Tag", "read Url: " + e.getMessage());
        } finally {
            if (inputStream != null){
                inputStream.close();
                httpURLConnection.disconnect();
            }
        }
        return parseData(data);
    }

    private PlaceInfoResponse parseData(String data) {
        Gson gson = new Gson();
        Log.d("DownloadPlacesFromUrl", "parseData: " + data);
        return gson.fromJson(data, PlaceInfoResponse.class);
    }
}
