/*
  Demonstrates generating a 2014 payments report with the Square Connect API.
  Replace the value of the `_accessToken` variable below before running this sample.

  You can alter the formatMoney method to display monetary amounts in a different format.

  This sample requires the Unirest Java library. Download instructions are here:
  http://unirest.io/java.html

  This sample requires Java SE 6 or later.
 */

package com.squareup.connectexamples;

import java.text.NumberFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.json.JSONArray;
import org.json.JSONObject;


public class PaymentsReporter {

  // Replace this value with your application's personal access token,
  // available from your application dashboard (https://connect.squareup.com/apps)
  private final String _accessToken = "REPLACE_ME";

  // The base URL for every Connect API request
  private final String _connectHost = "https://connect.squareup.com";

  // Constructor that sets headers common to every Connect API request
  public PaymentsReporter() {
    Unirest.setDefaultHeader("Authorization", "Bearer " + _accessToken);
    Unirest.setDefaultHeader("Content-Type", "application/json");
    Unirest.setDefaultHeader("Accept", "application/json");
  }

  // Helper function to convert cent-based money amounts to dollars and cents
  public String formatMoney(int money) {
    return NumberFormat.getCurrencyInstance().format(money / 100.0);
  }

  // Retrieves all of a merchant's payments from 2014
  public List<JSONObject> get2014Payments() {

    // Restrict the request to the 2014 calendar year, eight hours behind UTC.
    // Unirest URL-encodes query parameters automatically.
    String requestPath = Unirest.get(_connectHost + "/v1/me/payments")
        .queryString("begin_time", "2014-01-01T00:00:00-08:00")
        .queryString("end_time", "2015-01-01T00:00:00-08:00")
        .getUrl();

    List<JSONObject> payments = new ArrayList<JSONObject>();
    HttpResponse<JsonNode> response = null;
    boolean moreResults = true;

    while (moreResults) {
      try {

        // Send a GET request to the List Payments endpoint
        response = Unirest.get(requestPath).asJson();
      } catch (UnirestException e) {

        // If any HTTP request fails during method execution, return null to indicate an error
        return null;
      }

      if (response != null && response.getBody().isArray()) {

        // Read the converted JSON body into the cumulative list of results
        JSONArray paymentArray = response.getBody().getArray();
        for (int i = 0; i < paymentArray.length(); i++) {
          payments.add(paymentArray.getJSONObject(i));
        }

        // Check whether pagination information is included in a response header,
        // indicating more results
        if (response.getHeaders().containsKey("link")){
          String paginationHeader = response.getHeaders().get("link").get(0);
          if (paginationHeader.contains("rel='next'")) {

            // Extract the next batch URL from the header.
            //
            // Pagination headers have the following format:
            // <https://connect.squareup.com/v1/MERCHANT_ID/payments?batch_token=BATCH_TOKEN>;rel='next'
            // This line extracts the URL from the angle brackets surrounding it.
            requestPath = paginationHeader.split("<")[1].split(">")[0];
          } else {
            moreResults = false;
          }
        } else {
          moreResults = false;
        }
      } else {
        return null;
      }
    }

    // Remove potential duplicate values from the list of payments
    Set<String> seenPaymentIds = new HashSet<String>();
    List<JSONObject> uniquePayments = new ArrayList<JSONObject>();
    for (JSONObject payment : payments) {
      if (!seenPaymentIds.contains(payment.get("id").toString())) {
        seenPaymentIds.add(payment.get("id").toString());
        uniquePayments.add(payment);
      }
    }

    return uniquePayments;
  }

  // Prints a sales report based on an array of payments
  public void printSalesReport(List<JSONObject> payments) {

    // Variables for holding cumulative values of various monetary amounts
    int collectedMoney, taxes, tips, discounts, processingFees, returnedProcessingFees, netMoney, refunds;
    collectedMoney = taxes = tips = discounts = processingFees = returnedProcessingFees = netMoney
        = refunds = 0;

    // Add appropriate values to each cumulative variable
    for (JSONObject payment : payments) {
      collectedMoney += payment.getJSONObject("total_collected_money").getInt("amount");
      taxes          += payment.getJSONObject("tax_money")            .getInt("amount");
      tips           += payment.getJSONObject("tip_money")            .getInt("amount");
      discounts      += payment.getJSONObject("discount_money")       .getInt("amount");
      processingFees += payment.getJSONObject("processing_fee_money") .getInt("amount");
      netMoney       += payment.getJSONObject("net_total_money")      .getInt("amount");
      refunds        += payment.getJSONObject("refunded_money")       .getInt("amount");


      // When a refund is applied to a credit card payment, Square returns to the merchant a percentage
      // of the processing fee corresponding to the refunded portion of the payment. This amount
      // is not currently returned by the Connect API, but we can calculate it as shown:

      // If a processing fee was applied to the payment AND some portion of the payment was refunded...
      if (payment.getJSONObject("processing_fee_money").getInt("amount") < 0 &&
          payment.getJSONObject("refunded_money").getInt("amount") < 0) {

        // ...calculate the percentage of the payment that was refunded...
        float percentageRefunded = payment.getJSONObject("refunded_money").getInt("amount") /
                                   (float)payment.getJSONObject("total_collected_money").getInt("amount");

        // ...and multiply that percentage by the original processing fee
        returnedProcessingFees += payment.getJSONObject("processing_fee_money").getInt("amount") *
                                  percentageRefunded;
      }

    }

    int basePurchases = collectedMoney - taxes - tips;

    // Print a sales report similar to the Sales Summary in the merchant dashboard.
    System.out.println("");
    System.out.println("==SALES REPORT FOR 2014==");
    System.out.println("Gross Sales:      " + this.formatMoney(basePurchases - discounts));
    System.out.println("Discounts:        " + this.formatMoney(discounts));
    System.out.println("Net Sales:        " + this.formatMoney(basePurchases));
    System.out.println("Tax collected:    " + this.formatMoney(taxes));
    System.out.println("Tips collected:   " + this.formatMoney(tips));
    System.out.println("Total collected:  " + this.formatMoney(basePurchases + taxes + tips));
    System.out.println("Fees:             " + this.formatMoney(processingFees));
    System.out.println("Refunds:          " + this.formatMoney(refunds));
    System.out.println("Fees returned:    " + this.formatMoney(returnedProcessingFees));
    System.out.println("Net total:        " + this.formatMoney(netMoney + refunds + returnedProcessingFees));
  }

  // Call the methods defined above
  public static void main(String[] args) {
    PaymentsReporter reporter = new PaymentsReporter();
    List<JSONObject> payments = reporter.get2014Payments();
    if (payments != null) {
      reporter.printSalesReport(payments);
    }
  }
}
