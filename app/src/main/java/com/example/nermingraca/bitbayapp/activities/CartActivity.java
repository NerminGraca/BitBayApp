package com.example.nermingraca.bitbayapp.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.nermingraca.bitbayapp.R;
import com.example.nermingraca.bitbayapp.models.Product;
import com.example.nermingraca.bitbayapp.models.User;
import com.example.nermingraca.bitbayapp.service.ServiceRequest;
import com.example.nermingraca.bitbayapp.singletons.UserData;
import com.example.nermingraca.bitbayapp.util.CustomListAdapterWithQuantity;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String json = intent.getStringExtra("jsonProducts");

        if (json.contains("<!DOCTYPE html>")) {
            setContentView(R.layout.activity_cart_empty);
        } else {
            setContentView(R.layout.activity_cart);
            Log.d("DEBUG in Cart Activity", json);
            List<Product> products = productsFromJson(json);

            ListView mProductList = (ListView) findViewById(R.id.cart_list);
            CustomListAdapterWithQuantity productsAdapter = new CustomListAdapterWithQuantity
                    (this, products);
            mProductList.setAdapter(productsAdapter);

            mProductList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final Product clicked = (Product) parent.getItemAtPosition(position);

                    AlertDialog.Builder adb = new AlertDialog.Builder(CartActivity.this);
                    adb.setView(view);
                    adb.setTitle(getString(R.string.remove_from_cart_label));

                    adb.setIcon(android.R.drawable.ic_dialog_alert);

                    adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(), "You clicked on YES", Toast.LENGTH_SHORT).show();
                            String url = getString(R.string.service_remove_from_cart);
                            Log.d("CART DEBUG", url);
                            JSONObject json = new JSONObject();
                            try {
                                json.put("productId", clicked.getmId());
                                json.put("userId", clicked.getmSellerId());
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e("ERROR", e.getMessage());
                            }
                            String jsonString = json.toString();
                            Callback callback = response();
                            ServiceRequest.post(url, jsonString, callback);

                            finish();
                        }
                    });

                    adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(), "You clicked on Cancel", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                    parent.removeViewInLayout(view);
                    adb.show();
                }
            });

            Button mViewCheckoutButton = (Button) findViewById(R.id.view_to_checkout_button);
            mViewCheckoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CartActivity.this, CheckoutWebViewActivity.class);
                    startActivity(intent);
                }
            });
        }

    }

    public List<Product> productsFromJson(String json) {
        List<Product> tempList = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for(int i = 0; i < array.length(); i++){
                JSONObject productObj = array.getJSONObject(i);
                Log.d("RESPONSE", productObj.toString());
                int id = productObj.getInt("id");
                String name = productObj.getString("name");
                double price = productObj.getDouble("price");
                String description = productObj.getString("description");
                String owner = productObj.getString("owner");
                String imagePath = productObj.getString("productImagePath1");
                int userId = productObj.getInt("ownerId");
                int quantity = productObj.getInt("quantity");
                double ownerRating = productObj.getDouble("ownerRating");
                String ownerAddress = productObj.getString("ownerAddress");
                Product temp = new Product
                        (id, name, price, description, owner, imagePath, userId, quantity,
                                ownerRating, ownerAddress);
                tempList.add(temp);
                int orderedQuantity = productObj.getInt("orderedQuantity");
                temp.setmOrderedQuantity(orderedQuantity);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("ERROR", e.getMessage());
        }
        return tempList;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(MainActivity.getmSharedPreferences().getString(
                getString(R.string.key_user_email),
                null
        ) != null){
            getMenuInflater().inflate(R.menu.menu_show_product, menu);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //noinspection SimplifiableIfStatement
        /*
        if (id == R.id.action_settings) {
            return true;
        }
        */

        if (id == R.id.logout_action) {
            MainActivity.logout();
            moveTaskToBack(true);
            Intent toLogin = new Intent( CartActivity.this, MainActivity.class);
            startActivity(toLogin);
            return true;
        }

        if (id == R.id.profile_action) {
            User user = ProfileActivity.getCurrentUser();
            moveTaskToBack(true);
            Intent intent = new Intent(CartActivity.this, ProfileActivity.class);
            intent.putExtra("username", user.getmUsername());
            intent.putExtra("email", user.getmEmail());
            startActivity(intent);
            return true;
        }

        if (id == R.id.cart_action) {
            toCart();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toCart() {
        int buyerId = UserData.getInstance().getId();
        String url = getString(R.string.service_get_cart);
        JSONObject json = new JSONObject();

        try {
            json.put("userId", buyerId);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("ERROR", e.getMessage());
        }
        String jsonString = json.toString();
        Log.d("DEBUG", jsonString);
        Callback callback = response();
        ServiceRequest.post(url, jsonString, callback);
    }

    public Callback response() {
        return new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.e("ERROR", e.getMessage());
            }

            @Override
            public void onResponse(Response response) throws IOException {

                String responseJson = response.body().string();
                Log.d("DEBUG", responseJson);
                Intent toCart = new Intent(CartActivity.this, CartActivity.class);
                toCart.putExtra("jsonProducts", responseJson);
                startActivity(toCart);
            }
        };

    }
}
