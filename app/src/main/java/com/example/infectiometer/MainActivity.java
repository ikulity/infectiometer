package com.example.infectiometer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RequestQueue rQueue;

    private EditText etWeek;
    private TextView tvLocationSID;
    private TextView tvWeekSID;
    private TextView tvInfectionsCount;

    private RadioGroup radioGroup;
    private Spinner areaSpinner;
    private Spinner citySpinner;

    private JSONObject areaKeys;
    private JSONObject weekKeys;
    private JSONObject cityKeys;

    private int areaSID;

    private List<String> cityNameList = new ArrayList<String>();

    private Toast toast;

    //Callback interface for JSONRequest so we can get all the data needed before initializing anything else
    public interface VolleyCallBack {
        void onSuccess();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context = getApplicationContext();


        //gets a list of areas for the area spinner from a resource
        areaSpinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.spinner_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        areaSpinner.setAdapter(adapter);

        rQueue = Volley.newRequestQueue(this);

        //Requests the JSONObject that contains the key-value pairs of areas and weeks
        getJSONfromURL(new VolleyCallBack() {
            //After we successfully get the first JSONObject we can set a listener for the area spinner
            @Override
            public void onSuccess() {
                areaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String chosenArea = areaSpinner.getSelectedItem().toString();
                        //goes through the area keys and looks for the selected area SID
                        Iterator<String> iter = areaKeys.keys();
                        while (iter.hasNext()) {
                            String key = iter.next();
                            try {
                                Object value = areaKeys.get(key);
                                if (value.toString().equals(chosenArea))
                                {
                                    //area SID found!
                                    areaSID = Integer.parseInt(key);

                                    //gets the city names within the chosen area with a new JSONRequest using the former area SID. also adds these city names to the second spinner
                                    String locationURL = "https://sampo.thl.fi/pivot/prod/fi/epirapo/covid19case/fact_epirapo_covid19case.json?row=hcdmunicipality2020-" + areaSID;
                                    JsonObjectRequest jsonObjectRequest1 = new JsonObjectRequest(Request.Method.GET, locationURL, null, new Response.Listener<JSONObject>() {
                                        @Override
                                        public void onResponse(JSONObject response) {
                                            try {
                                                JSONObject dataset = response.getJSONObject("dataset");
                                                JSONObject dimension = dataset.getJSONObject("dimension");
                                                JSONObject hcd = dimension.getJSONObject("hcdmunicipality2020");
                                                JSONObject category = hcd.getJSONObject("category");
                                                cityKeys = category.getJSONObject("label");

                                                //before adding the city names to a spinner list lets clear the list and add an item ("all cities")
                                                cityNameList.clear();
                                                cityNameList.add("Koko alue");
                                                Iterator<String> iter = cityKeys.keys();
                                                while (iter.hasNext())
                                                {
                                                    String key = iter.next();
                                                    try {
                                                        Object value = cityKeys.get(key);
                                                        cityNameList.add(value.toString());
                                                    } catch (JSONException e)
                                                    {
                                                        System.out.println(e.getMessage());
                                                    }
                                                }
                                            } catch (JSONException e) {
                                                //Toast toast = Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG);
                                                //toast.show();
                                                e.printStackTrace();
                                            }
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error)
                                        {
                                            Toast toast = Toast.makeText(context, "error.getMessage()", Toast.LENGTH_LONG);
                                            toast.show();
                                        }
                                    }) ;

                                    rQueue.add(jsonObjectRequest1);

                                    break;
                                }
                            } catch (JSONException e) {
                                toast = Toast.makeText(context, "Error in finding area keys", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                        citySpinner.setSelection(0);
                        tvInfectionsCount.setText("");
                        //citySpinner.getRootView().invalidate();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }
        });

        //Setup for other views..

        //City spinner
        citySpinner = findViewById(R.id.spinner2);
        cityNameList.add("Koko alue");
        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, cityNameList);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        citySpinner.setAdapter(adapter1);

        //Text-related objects
        etWeek = findViewById(R.id.editTextWeekNumber);
        tvLocationSID = findViewById(R.id.textViewLocationSID);
        tvWeekSID = findViewById(R.id.textViewWeekSID);
        tvInfectionsCount = findViewById(R.id.textViewInfections);

        //RadioGroup
        radioGroup  = findViewById(R.id.radioGroup);
    }





    public void getJSONfromURL(final VolleyCallBack callBack)
    {
        Context context = getApplicationContext();
        String url = "https://sampo.thl.fi/pivot/prod/fi/epirapo/covid19case/fact_epirapo_covid19case.json";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject dataset = response.getJSONObject("dataset");
                    JSONObject dimension = dataset.getJSONObject("dimension");

                    //area SIDs
                    JSONObject hcd = dimension.getJSONObject("hcdmunicipality2020");
                    JSONObject category = hcd.getJSONObject("category");
                    areaKeys = category.getJSONObject("label");


                    //week SIDs
                    JSONObject dateweek = dimension.getJSONObject("dateweek20200101");
                    JSONObject category1 = dateweek.getJSONObject("category");
                    weekKeys = category1.getJSONObject("index");

                    callBack.onSuccess();

                } catch (JSONException e) {
                    Toast toast = Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT);
                    toast.show();
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                Toast toast = Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT);
                toast.show();
            }
        }) ;

        rQueue.add(jsonObjectRequest);
    }





    public void getInfoBTN_onClick(View view)
    {
        Context context = getApplicationContext();
        Toast toast;

        //user input strings
        String city = citySpinner.getSelectedItem().toString();
        String week = etWeek.getText().toString();

        int radioButtonID = radioGroup.getCheckedRadioButtonId();
        View radioButton = radioGroup.findViewById(radioButtonID);
        int radioButtonIndex = radioGroup.indexOfChild(radioButton);

        //at this point we already know the area SID because it was used to get the city names for the city spinner
        //if the user selected "all cities" aka "whole area" we use the area SID as the location SID
        String locationSID = "";

        if (city.equals("Koko alue"))
            locationSID = Integer.toString(areaSID);
        else
        {
            Iterator<String> iter = cityKeys.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                try {
                    Object value = cityKeys.get(key);
                    if (value.toString().equals(city))
                        locationSID = key;
                } catch (JSONException e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        //if inputted "week number" is blank we set it to 105 which means "all time" stats
        if (week.equals(""))
            week = "105";
        else{
            //max week is 52
            if (Integer.parseInt(week) > 52){
                week = "52";
                etWeek.setText(week);
            }

            //calculate the week number using the radiobutton index (0-104 weeks for 2 years of covid)
            week = Integer.toString(Integer.parseInt(week) + (53 * radioButtonIndex) - 1);
        }


        //looks for the week SID with the week number we just got (0-104)
        int viikkoSID = 0;
        Iterator<String> iter = weekKeys.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            try {
                Object value = weekKeys.get(key);
                if (value.toString().equals(week))
                    viikkoSID = Integer.parseInt(key);
            } catch (JSONException e) {
                toast = Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG);
                toast.show();
            }
        }

        //SID printing for debugging
        //tvLocationSID.setText(locationSID);
        //tvWeekSID.setText(Integer.toString(viikkoSID));


        //new url with a more strict search criteria (area/city and week number)
        String url1 = "https://sampo.thl.fi/pivot/prod/fi/epirapo/covid19case/fact_epirapo_covid19case.json?row=hcdmunicipality2020-" + locationSID + ".&column=dateweek20200101-" + viikkoSID + ".&filter=measure-444833";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url1, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject dataset = response.getJSONObject("dataset");
                    JSONObject values = dataset.getJSONObject("value");
                    String value = values.get("0").toString();
                    if (value.equals(".."))
                        tvInfectionsCount.setText(R.string.eitietoja);
                    else
                        tvInfectionsCount.setText(values.get("0").toString() + " tartuntaa");

                } catch (JSONException e) {
                    tvInfectionsCount.setText(R.string.eitietoja);
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                Context context = getApplicationContext();
                Toast toast = Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG);
                toast.show();
            }
        }) ;

        rQueue.add(jsonObjectRequest);

    }





    public void infoImage_onClick(View view)
    {
        Context context = getApplicationContext();
        toast = Toast.makeText(context, R.string.info, Toast.LENGTH_SHORT);
        toast.show();
    }

}