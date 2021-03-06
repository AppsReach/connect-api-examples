<?php

# Demonstrates creating, updating, and deleting an item with the Square Connect API.
# Replace the value of the `$accessToken` variable below before running this sample.
#
# This sample requires the Unirest PHP library. Download it here:
# http://unirest.io/php.html
#
# Results are logged with error_log.


# Replace this value with the path to the Unirest PHP library
require_once 'path/to/Unirest.php';


# Replace this value with your application's personal access token,
# available from your application dashboard (https://connect.squareup.com/apps)
$accessToken = 'REPLACE_ME';

# The base URL for every Connect API request
$connectHost = 'https://connect.squareup.com';

# Standard HTTP headers for every Connect API request
$requestHeaders = array (
  'Authorization' => 'Bearer ' . $accessToken,
  'Accept' => 'application/json',
  'Content-Type' => 'application/json'
);

# Creates a "Milkshake" item.
function createItem() {
  global $accessToken, $connectHost, $requestHeaders;

  $request_body = array(
    "name"=>"Milkshake",
    "variations"=>array(
      array(
        "name"=>"Small",
        "pricing_type"=>"FIXED_PRICING",
        "price_money"=>array(
          "currency_code"=>"USD",
          "amount"=>400
        )
      )
    )
  );

  $response = Unirest\Request::post($connectHost . '/v1/me/items', $requestHeaders, json_encode($request_body));

  if ($response->code == 200) {
    error_log('Successfully created item:');
    error_log(json_encode($response->body, JSON_PRETTY_PRINT));
    return $response->body;
  } else {
    error_log('Item creation failed');
    return NULL;
  }
}

# Updates the Milkshake item to rename it to "Malted Milkshake"
function updateItem($itemId) {
  global $accessToken, $connectHost, $requestHeaders;

  $request_body = array(
    "name"=>"Malted Milkshake"
  );

  $response = Unirest\Request::put($connectHost . '/v1/me/items/' . $itemId, $requestHeaders, json_encode($request_body));

  if ($response->code == 200) {
    error_log('Successfully updated item:');
    error_log(json_encode($response->body, JSON_PRETTY_PRINT));
    return $response->body;
  } else {
    error_log('Item update failed');
    return NULL;
  }
}

# Deletes the Malted Milkshake item.
function deleteItem($itemId) {
  global $accessToken, $connectHost, $requestHeaders;

  $response = Unirest\Request::delete($connectHost . '/v1/me/items/' . $itemId, $requestHeaders);

  if ($response->code == 200) {
    error_log('Successfully deleted item');
    return $response->body;
  } else {
    error_log('Item deletion failed');
    return NULL;
  }
}

$myItem = createItem();

# Update and delete the item only if it was successfully created
if ($myItem) {
  updateItem($myItem->id);
  deleteItem($myItem->id);
} else {
  error_log("Aborting");
}

?>
