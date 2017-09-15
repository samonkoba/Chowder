package com.toe.chowder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.toe.chowder.constructors.ChowderCredentials;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.toe.chowder.utils.ChowderUtils.encodeBase64;
import static com.toe.chowder.utils.ChowderUtils.showMessage;

public class Chowder {

    //Values
    private String BASE_URL_SANDBOX = "https://sandbox.safaricom.co.ke/mpesa/";
    private String BASE_URL_OAUTH_SANDBOX = "https://sandbox.safaricom.co.ke/";

    private String API_TYPE_OAUTH = "oauth/";
    private String API_TYPE_C2B = "c2b/";
    private String API_TYPE_B2C = "b2c/";
    private String API_TYPE_B2B = "b2b/";
    private String API_TYPE_ACCOUNT_BALANCE = "accountbalance/";
    private String API_TYPE_STK_PUSH = "stkpush/";
    private String API_TYPE_STK_PUSH_QUERY = "stkpushquery/";
    private String API_TYPE_TRANSACTION_STATUS = "transactionstatus/";

    private String API_VERSION = "v1/";

    private String END_POINT_GENERATE_TOKEN = "generate?grant_type=client_credentials";
    private String END_POINT_PAYMENT_REQUEST = "paymentrequest";
    private String END_POINT_REGISTER_URL = "registerurl";
    private String END_POINT_SIMULATE = "simulate";
    private String END_POINT_QUERY = "query";
    private String END_POINT_REVERSAL = "reversal";
    private String END_POINT_LIPA_NA_MPESA = "processrequest";

    //Command IDs
    private String COMMAND_TRANSACTION_REVERSAL = "TransactionReversal";
    private String COMMAND_SALARY_PAYMENT = "SalaryPayment";
    private String COMMAND_BUSINESS_PAYMENT = "BusinessPayment";
    private String COMMAND_PROMOTION_PAYMENT = "PromotionPayment";
    private String COMMAND_ACCOUNT_BALANCE = "AccountBalance";
    private String COMMAND_CUSTOMER_PAY_BILL_ONLINE = "CustomerPayBillOnline";
    private String COMMAND_TRANSACTION_STATUS_QUERY = "TransactionStatusQuery";
    private String COMMAND_CHECK_IDENTITY = "CheckIdentity";
    private String COMMAND_BUSINESS_PAY_BILL = "BusinessPayBill";
    private String COMMAND_BUSINESS_BUY_GOODS = "BusinessBuyGoods";
    private String COMMAND_DISBURSE_FUNDS_TO_BUSINESS = "DisburseFundsToBusiness";
    private String COMMAND_BUSINESS_TO_BUSINESS_TRANSFER = "BusinessToBusinessTransfer";
    private String COMMAND_BUSINESS_TRANSFER_MMF_UTILITY = "BusinessTransferFromMMFToUtility";

    //Others
    private ChowderCredentials chowderCredentials;
    private Activity activity;
    private RequestQueue queue;
    private ProgressDialog progress;

    //Generated Values
    private String accessToken;
    private int expiresIn;

    public Chowder(Activity activity, ChowderCredentials chowderCredentials) {
        this.activity = activity;
        this.chowderCredentials = chowderCredentials;
        queue = Volley.newRequestQueue(activity);
    }

    public void generateAccessToken() {
        String URL = BASE_URL_OAUTH_SANDBOX + API_TYPE_OAUTH + API_VERSION + END_POINT_GENERATE_TOKEN;
        progress = ProgressDialog.show(activity, "Please wait", "Generating access token...", true);

        Log.d("YAYA", URL);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responseJson = new JSONObject(response);
                            accessToken = responseJson.getString("access_token");
                            expiresIn = Integer.parseInt(responseJson.getString("expires_in"));

                            Log.d("YAYA", accessToken);

                            b2cPaymentRequest(COMMAND_BUSINESS_PAYMENT, 1000);
                        } catch (JSONException e) {
                            showMessage(activity, "Something went wrong: " + e.getMessage());
                            e.printStackTrace();
                        }

                        progress.dismiss();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showMessage(activity, "Something went wrong: " + error.getMessage());
                error.printStackTrace();
                progress.dismiss();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("authorization", encodeBase64(chowderCredentials.getMpesaKey(), chowderCredentials.getMpesaSecret()));
                return headers;
            }
        };

        queue.add(stringRequest);
    }

    public void b2cPaymentRequest(String command, int amount) {
        String URL = BASE_URL_SANDBOX + API_TYPE_B2C + API_VERSION + END_POINT_PAYMENT_REQUEST;

        Log.d("YAYA", URL);

        progress = ProgressDialog.show(activity, "Please wait", "Making payment request...", true);
        JSONObject json = new JSONObject();
        try {
            json.put("InitiatorName", chowderCredentials.getInitiatorName());
            json.put("SecurityCredential", chowderCredentials.getSecurityCredential());
            json.put("CommandID", command);
            json.put("Amount", amount);
            json.put("PartyA", chowderCredentials.getShortCode1());
            json.put("PartyB", chowderCredentials.getShortCode2());
            json.put("Remarks", "Transaction: " + command);
            json.put("QueueTimeOutURL", "http://test.com/mpesa");
            json.put("ResultURL", "http://test.com/mpesa");
            json.put("Occassion", "Occasion");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("YAYA", json.toString());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(URL, json, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("YAYA", response.toString());
                progress.dismiss();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showMessage(activity, "Something went wrong: " + error.getMessage());
                error.printStackTrace();
                progress.dismiss();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("authorization", "Bearer " + accessToken);
                return headers;
            }
        };

        queue.add(jsonObjectRequest);
    }
}