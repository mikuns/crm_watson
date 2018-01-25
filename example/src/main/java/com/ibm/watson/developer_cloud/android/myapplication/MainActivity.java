/*
 * Copyright 2017 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.ibm.watson.developer_cloud.android.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.HashMap;
//import java.util.List;
import java.util.Map;

//import com.google.gson.Gson;
import com.ibm.watson.developer_cloud.conversation.v1.ConversationService;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;
import com.ibm.watson.developer_cloud.http.ServiceCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
  private final String TAG = "MainActivity";

  private EditText input;
  private ImageView conv;
  private TextView clear;

  private static ConversationService conversationService;

  private Handler handler = new Handler();
  public ListView msgView;
  public ArrayAdapter<String> msgList;
  Map context = new HashMap();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    conversationService = initConversationService();
    // set the workspace id for WCS
    final String inputWorkspaceId = getString(R.string.conversation_workspaceId);

    msgView = (ListView) findViewById(R.id.listView);
    msgList = new ArrayAdapter<String>(this, R.layout.msg_list_view);
    msgView.setAdapter(msgList);

    input = (EditText) findViewById(R.id.input);
    conv = (ImageView) findViewById(R.id.conv_button);
    clear = (TextView) findViewById(R.id.clear);

    MessageResponse response = null;
    conversationAPI(String.valueOf(input.getText()), context, inputWorkspaceId);

    conv.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //pressing the [Send] button passes the text to the WCS conversation service
        MessageResponse response = null;
        conversationAPI(String.valueOf(input.getText()), context, inputWorkspaceId);
      }
    });

    clear.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //pressing the [Clear] button should clear the context
        //in essence starting the conversation again from scratch
        context = new HashMap();
        //should also clear the msgList
        msgList.clear();;
        msgView.setAdapter(msgList);
        msgView.smoothScrollToPosition(msgList.getCount() - 1);
        //invoke the initial message from WCS
        MessageResponse response = null;
        conversationAPI(String.valueOf(input.getText()), context, inputWorkspaceId);
      }
    });


  }

  private void showError(final Exception e) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        e.printStackTrace();
      }
    });
  }

  private ConversationService initConversationService() {
    ConversationService service = new ConversationService(ConversationService.VERSION_DATE_2016_07_11);
    String username = getString(R.string.conversation_username);
    String password = getString(R.string.conversation_password);
    service.setUsernameAndPassword(username, password);
    service.setEndPoint(getString(R.string.conversation_url));
    return service;
  }

  /**
   * On activity result.
   *
   * @param requestCode the request code
   * @param resultCode the result code
   * @param data the data
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    //do nothing special
  }


  public void conversationAPI(String input, Map context, String workspaceId) {

    //conversationService
    MessageRequest newMessage = new MessageRequest.Builder()
            .inputText(input).context(context).build();

    //cannot use the following as it will attempt to run on the UI thread and crash
//    MessageResponse response = conversationService.message(workspaceId, newMessage).execute();

    //use the following so it runs on own async thread
    //then when get a response it calls displayMsg that will update the UI
    conversationService.message(workspaceId, newMessage).enqueue(new ServiceCallback<MessageResponse>() {
      @Override
      public void onResponse(MessageResponse response) {
        //output to system log output, just for verification/checking
        System.out.println(response);
        displayMsg(response);
      }
      @Override
      public void onFailure(Exception e) {
        showError(e);
      }
    });
  };

  public void displayMsg(MessageResponse msg)
  {
    final MessageResponse mssg=msg;
    handler.post(new Runnable() {

      @Override
      public void run() {

        //from the WCS API response
        //https://www.ibm.com/watson/developercloud/conversation/api/v1/?java#send_message
        //extract the text from output to display to the ic_user2
        String text = mssg.getText().get(0);

        //now output the text to the UI to show the chat history
        msgList.add(text);
        msgView.setAdapter(msgList);
        msgView.smoothScrollToPosition(msgList.getCount() - 1);

        //set the context, so that the next time we call WCS we pass the accumulated context
        context = mssg.getContext();

        //rather than converting response to a JSONObject and parsing through it
        //we can use the APIs for the MessageResponse .getXXXXX() to get the values as shown above
        //keeping the following just in case need this at a later date
        //
        //          https://developer.android.com/reference/org/json/JSONObject.html
/*
          JSONObject jObject = new JSONObject(mssg);
          JSONObject jsonOutput = jObject.getJSONObject("output");
          JSONArray jArray1 = jsonOutput.getJSONArray("text");
          for (int i=0; i < jArray1.length(); i++)
          {
            try {
              String textContent = String.valueOf(jArray1.getString(i));
              System.out.println(textContent);
              msgList.add(textContent);
              msgView.setAdapter(msgList);
              msgView.smoothScrollToPosition(msgList.getCount() - 1);
            } catch (JSONException e) {
              // Oops
              System.out.println(e);
            }
          }
          JSONArray jArray2 = jObject.getJSONArray("intents");
          for (int i=0; i < jArray2.length(); i++)
          {
            try {
              JSONObject oneObject = jArray2.getJSONObject(i);
              // Pulling items from the array
              String oneObjectsItem = oneObject.getString("confidence");
              String oneObjectsItem2 = oneObject.getString("intent");
              String jOutput = oneObjectsItem+" : "+oneObjectsItem2;
              msgList.add(jOutput);
              msgView.setAdapter(msgList);
              msgView.smoothScrollToPosition(msgList.getCount() - 1);
            } catch (JSONException e) {
              // Oops
            }
          }
*/
      }
    });

  };

}
