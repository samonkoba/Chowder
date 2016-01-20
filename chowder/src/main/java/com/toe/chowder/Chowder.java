package com.toe.chowder;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.alexgilleran.icesoap.exception.SOAPException;
import com.alexgilleran.icesoap.observer.SOAP11Observer;
import com.alexgilleran.icesoap.request.Request;
import com.alexgilleran.icesoap.request.RequestFactory;
import com.alexgilleran.icesoap.request.SOAP11Request;
import com.alexgilleran.icesoap.request.impl.RequestFactoryImpl;
import com.alexgilleran.icesoap.soapfault.SOAP11Fault;
import com.toe.chowder.envelopes.ProcessCheckoutEnvelope;
import com.toe.chowder.envelopes.TransactionConfirmEnvelope;
import com.toe.chowder.envelopes.TransactionStatusQueryEnvelope;
import com.toe.chowder.responses.ProcessCheckoutResponse;
import com.toe.chowder.responses.TransactionConfirmResponse;
import com.toe.chowder.responses.TransactionStatusQueryResponse;
import com.toe.chowder.utils.Utils;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;


/**
 * Created by Wednesday on 1/16/2016.
 */
public class Chowder {

//    Chowder chowder = new Chowder(YourActivity.this, getString(R.string.merchant_id), getString(R.string.passkey));
//    //Product ID has to have 13 digits
//    chowder.processPayment("1717171717171", amount, phoneNumber);
//    chowder.paymentCompleteDialog = new AlertDialog.Builder(YourActivity.this)
//            .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
//        public void onClick(DialogInterface dialog, int which) {
//           //Do something because the user has completed payment
//            dialog.dismiss();
//        }
//    });

    //      The Merchant captures the payment details and prepares call to the SAG’s endpoint
    //      The Merchant invokes SAG’s processCheckOut interface
    //      The SAG validates the request sent and returns a response
    //      Merchant receives the processCheckoutResponse parameters namely
    //      TRX_ID, ENC_PARAMS, RETURN_CODE, DESCRIPTION and CUST_MSG (Customer message)
    //      The merchant is supposed to display the CUST_MSG to the customer after which the merchant should invoke SAG’s confirmPaymentRequest interface to confirm the transaction
    //      The system will push a USSD menu to the customer and prompt the customer to enter their BONGA PIN and any other validation information.
    //      The transaction is processed on M-PESA and a callback is executed after completion of the transaction

    private String SUCCESS_CODE = "00";
    private RequestFactory requestFactory = new RequestFactoryImpl();

    //Need the url and SOAP action
    private String url = "https://safaricom.co.ke/mpesa_online/lnmo_checkout_server.php?wsdl";
    private String soapAction = "https://safaricom.co.ke/mpesa_online/lnmo_checkout_server.php?wsdl";

    //M-Pesa checkout parameters
    private String encParams = "";
    private String callBackUrl = "http://172.21.20.215:8080/test";
    private String callBackMethod = "POST";

    private String merchantId;
    private String passkey;

    private Activity activity;
    private ProgressDialog progress;
    private String merchantTransactionId;

    public AlertDialog.Builder paymentCompleteDialog;
    private String timestamp;
    private String password;

    public Chowder(Activity activity, String merchantId, String passkey) {
        this.activity = activity;
        this.merchantId = merchantId;
        this.passkey = passkey;
    }

    public Chowder(Activity activity, String merchantId, String passkey, String callbackUrl) {
        this.activity = activity;
        this.merchantId = merchantId;
        this.passkey = passkey;
        this.callBackUrl = callbackUrl;
    }

    public void processPayment(String productId, String amount, String phoneNumber) {
        progress = ProgressDialog.show(activity, "Please wait",
                "Connecting to Safaricom...", true);

        String referenceId = productId;
        timestamp = Utils.generateTimestamp();
        merchantTransactionId = Utils.generateRandomId();
        password = Utils.generatePassword(merchantId + passkey + timestamp);

//      The Merchant captures the payment details and prepares call to the SAG’s endpoint.
//      The Merchant invokes SAG’s processCheckOut interface.
        Log.d("M-PESA REQUEST", new ProcessCheckoutEnvelope(merchantId, password, timestamp, merchantTransactionId, referenceId, amount, phoneNumber, encParams, callBackUrl, callBackMethod).toString());

        trustEveryone();
        SOAP11Request<ProcessCheckoutResponse> processCheckoutRequest = requestFactory.buildRequest(url, new ProcessCheckoutEnvelope(merchantId, password, timestamp, merchantTransactionId, referenceId, amount, phoneNumber, encParams, callBackUrl, callBackMethod), soapAction, ProcessCheckoutResponse.class);
        processCheckoutRequest.execute(processCheckoutObserver);
    }

    private void trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }
    }

    private SOAP11Observer<ProcessCheckoutResponse> processCheckoutObserver = new SOAP11Observer<ProcessCheckoutResponse>() {

        @Override
        public void onCompletion(Request<ProcessCheckoutResponse, SOAP11Fault> request) {
            if (request.getResult() != null) {
                String returnCode = request.getResult().getReturnCode();
                String processDescription = request.getResult().getDescription();
                String transactionId = request.getResult().getTransactionId();
                String encParams = request.getResult().getEncParams();
                String customMessage = request.getResult().getCustomMessage();

                Log.d("M-PESA REQUEST", "Return code: " + returnCode);

//                The SAG validates the request sent and returns a response.
//                Merchant receives the processCheckoutResponse parameters namely
//                TRX_ID, ENC_PARAMS, RETURN_CODE, DESCRIPTION and
//                CUST_MSG (Customer message).

                if (returnCode.equals(SUCCESS_CODE)) {
                    progress.dismiss();
//                   The merchant is supposed to display the CUST_MSG to the customer after which
//                   the merchant should invoke SAG’s confirmPaymentRequest
//                   interface to confirm the transaction

                    showCustomMessage(activity, customMessage, transactionId);
                } else {
                    progress.dismiss();
                    Log.d("M-PESA REQUEST", "Process checkout failed: " + returnCode);
                    Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT).show();
                    showErrorMessage(activity, "Checkout process failed. Return code: " + returnCode);
                }
            } else {
                progress.dismiss();
                Log.d("M-PESA REQUEST", "Result is null");
                Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT).show();
                showErrorMessage(activity, "Checkout process failed. Please check your internet connection and try again.");
            }
        }

        @Override
        public void onException(Request<ProcessCheckoutResponse, SOAP11Fault> request, SOAPException e) {
            progress.dismiss();
            Log.e("M-PESA REQUEST", "Error: " + e.toString());
            Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT).show();
            showErrorMessage(activity, "Checkout process failed. Error: " + e.toString());
        }
    };

    private void showCustomMessage(final Context context, String customMessage, final String transactionId) {
        new AlertDialog.Builder(context)
                .setTitle("Payment Ready")
                .setMessage(customMessage)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setPositiveButton("Pay", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("M-PESA REQUEST", new TransactionConfirmEnvelope(merchantId, password, timestamp, transactionId, merchantTransactionId).toString());

                        progress = ProgressDialog.show(context, "Please wait",
                                "Processing your payment...", true);
                        SOAP11Request<TransactionConfirmResponse> transactionConfirmRequest = requestFactory.buildRequest(url, new TransactionConfirmEnvelope(merchantId, password, timestamp, transactionId, merchantTransactionId), soapAction, TransactionConfirmResponse.class);
                        transactionConfirmRequest.execute(transactionConfirmObserver);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private SOAP11Observer<TransactionConfirmResponse> transactionConfirmObserver = new SOAP11Observer<TransactionConfirmResponse>() {

        @Override
        public void onCompletion(Request<TransactionConfirmResponse, SOAP11Fault> request) {
//            The system will push a USSD menu to the customer and prompt the customer to enter
//            their BONGA PIN and any other validation information.
//            The transaction is processed on M-PESA and a callback is executed after completion of the transaction.

            if (request.getResult() != null) {

                String returnCode = request.getResult().getReturnCode();
                String processDescription = request.getResult().getDescription();
                String merchantTransactionId = request.getResult().getMerchantTransactionId();
                final String transactionId = request.getResult().getTransactionId();

                Log.d("M-PESA REQUEST", "Return code: " + returnCode);

                if (returnCode.equals(SUCCESS_CODE)) {
                    progress.dismiss();
                    new AlertDialog.Builder(activity)
                            .setTitle("Transaction in Progress")
                            .setMessage("Please enter your Bonga PIN in the Safaricom dialog that will pop up. After you have made your payment, confirm it here.")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setCancelable(false)
                            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    checkTransactionStatus(merchantId, transactionId);
                                    dialog.dismiss();
                                }
                            })
                            .show();
                } else {
                    progress.dismiss();
                    Log.d("M-PESA REQUEST", "Transaction confirm failed: " + returnCode);
                    Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT).show();
                    showErrorMessage(activity, "Transaction confirmation failed. Return code: " + returnCode);
                }
            } else {
                progress.dismiss();
                Log.d("M-PESA REQUEST", "Result is null");
                Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT).show();
                showErrorMessage(activity, "Transaction confirmation failed. Please check your internet connection and try again.");
            }
        }

        @Override
        public void onException(Request<TransactionConfirmResponse, SOAP11Fault> request, SOAPException e) {
            progress.dismiss();
            Log.e("M-PESA REQUEST", "Error: " + e.toString());
            Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT).show();
            showErrorMessage(activity, "Transaction confirmation failed. Error: " + e.toString());
        }
    };

    public void checkTransactionStatus(String merchantId, String transactionId) {
        progress = ProgressDialog.show(activity, "Please wait",
                "Checking the transaction status...", true);
        Log.d("M-PESA REQUEST", new TransactionStatusQueryEnvelope(merchantId, password, timestamp, transactionId, merchantTransactionId).toString());

        SOAP11Request<TransactionStatusQueryResponse> transactionStatusQueryRequest = requestFactory.buildRequest(url, new TransactionStatusQueryEnvelope(merchantId, password, timestamp, transactionId, merchantTransactionId), soapAction, TransactionStatusQueryResponse.class);
        transactionStatusQueryRequest.execute(transactionStatusQueryObserver);
    }

    private SOAP11Observer<TransactionStatusQueryResponse> transactionStatusQueryObserver = new SOAP11Observer<TransactionStatusQueryResponse>() {

        @Override
        public void onCompletion(Request<TransactionStatusQueryResponse, SOAP11Fault> request) {
            if (request.getResult() != null) {
                String msisdn = request.getResult().getMsisdn();
                String amount = request.getResult().getAmount();
                String mpesaTransactionDate = request.getResult().getMpesaTransactionDate();
                String mpesaTransactionId = request.getResult().getMpesaTransactionId();
                String transactionStatus = request.getResult().getTransactionStatus();
                String returnCode = request.getResult().getReturnCode();
                String processDescription = request.getResult().getDescription();
                String merchantTransactionId = request.getResult().getMerchantTransactionId();
                String encParams = request.getResult().getEncParams();
                String transactionId = request.getResult().getTransactionId();

                Log.d("M-PESA REQUEST", "Return code: " + returnCode);

                if (returnCode.equals(SUCCESS_CODE)) {
                    progress.dismiss();
                    Log.d("M-PESA REQUEST", "Transaction id: " + transactionId);
                    paymentCompleteDialog.setTitle("Payment Complete")
                            .setMessage("Your amount of Ksh." + amount + " has been successfully paid to merchant Id " + merchantId + " with the M-Pesa transaction code " + mpesaTransactionId + " on " + mpesaTransactionDate + ".\n\nThank you for your business. ")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setCancelable(false)
                            .show();
                } else {
                    progress.dismiss();
                    Log.d("M-PESA REQUEST", "Transaction confirm failed: " + returnCode);
                    Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT).show();
                    showErrorMessage(activity, "Transaction status query failed. Return code: " + returnCode);
                }
            } else {
                progress.dismiss();
                Log.d("M-PESA REQUEST", "Result is null");
                Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT).show();
                showErrorMessage(activity, "Transaction status query failed. Please check your internet connection and try again.");
            }
        }

        @Override
        public void onException(Request<TransactionStatusQueryResponse, SOAP11Fault> request, SOAPException e) {
            progress.dismiss();
            Log.e("M-PESA REQUEST", "Error: " + e.toString());
            Toast.makeText(activity, "Something went wrong", Toast.LENGTH_SHORT).show();
            showErrorMessage(activity, "Transaction status query failed. Error: " + e.toString());
        }
    };

    private void showErrorMessage(final Context context, String customMessage) {
        new AlertDialog.Builder(context)
                .setTitle("Payment Ready")
                .setMessage(customMessage)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }
}