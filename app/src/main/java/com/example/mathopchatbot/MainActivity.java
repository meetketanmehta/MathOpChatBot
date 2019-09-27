package com.example.mathopchatbot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;

import co.intentservice.chatui.ChatView;
import co.intentservice.chatui.models.ChatMessage;

import static co.intentservice.chatui.models.ChatMessage.Type.RECEIVED;

import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobileconnectors.lex.interactionkit.InteractionClient;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.continuations.LexServiceContinuation;
import com.amazonaws.mobileconnectors.lex.interactionkit.listeners.InteractionListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lexrts.model.DialogState;
import com.prakritibansal.posttextrequest.TextResponse;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "Main Activity";
    private InteractionClient lexInteractionClient;
    private ChatView chatView ;

    final InteractionListener interactionListener = new InteractionListener() {
        @Override
        public void onReadyForFulfillment(final Response response) {
            Log.d(TAG, "Transaction completed successfully");

            ChatMessage message = new ChatMessage(response.getTextResponse(), getCurrentTimeStamp(), RECEIVED);
            chatView.addMessage(message);
        }

        @Override
        public void promptUserToRespond(final Response response,
                                        final LexServiceContinuation continuation) {

            ChatMessage message = new ChatMessage(response.getTextResponse(), getCurrentTimeStamp(), RECEIVED);
            chatView.addMessage(message);
        }

        @Override
        public void onInteractionError(final Response response, final Exception e) {
            if (response != null) {
                if (DialogState.Failed.toString().equals(response.getDialogState())) {
                    ChatMessage message = new ChatMessage(response.getTextResponse(), getCurrentTimeStamp(), RECEIVED);
                    chatView.addMessage(message);
                } else {
                    chatView.addMessage(new ChatMessage("Please Retry", getCurrentTimeStamp(), RECEIVED));
                }
            } else {
                showToast("Error: " + e.getMessage());
                Log.e(TAG, "Interaction error", e);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatView = (ChatView) findViewById(R.id.chat_view);

        initializeLexSDK();

        chatView.setOnSentMessageListener(new ChatView.OnSentMessageListener() {
            @Override
            public boolean sendMessage(ChatMessage chatMessage) {
                lexInteractionClient.textInForTextOut(chatMessage.getMessage(), null);
                return true;
            }
        });

    }

    private void initializeLexSDK() {
        Log.d(TAG, "Lex Client");

        // Initialize the mobile client
        AWSMobileClient.getInstance().initialize(this, new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails result) {
                Log.d(TAG, "initialize.onResult, userState: " + result.getUserState().toString());

                // Identity ID is not available until we make a call to get credentials, which also
                // caches identity ID.
                AWSMobileClient.getInstance().getCredentials();

                String identityId = AWSMobileClient.getInstance().getIdentityId();
                Log.d(TAG, "identityId: " + identityId);
                String botName = null;
                String botAlias = null;
                String botRegion = null;
                JSONObject lexConfig;
                try {
                    lexConfig = AWSMobileClient.getInstance().getConfiguration().optJsonObject("Lex");
                    lexConfig = lexConfig.getJSONObject(lexConfig.keys().next());

                    botName = lexConfig.getString("Name");
                    botAlias = lexConfig.getString("Alias");
                    botRegion = lexConfig.getString("Region");
                } catch (JSONException e) {
                    Log.e(TAG, "onResult: Failed to read configuration", e);
                }

                InteractionConfig lexInteractionConfig = new InteractionConfig(
                        botName,
                        botAlias,
                        identityId);

                lexInteractionClient = new InteractionClient(getApplicationContext(),
                        AWSMobileClient.getInstance(),
                        Regions.fromName(botRegion),
                        lexInteractionConfig);

                lexInteractionClient.setInteractionListener(interactionListener);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "initialize.onError: ", e);
            }
        });
    }

    private long getCurrentTimeStamp() {
        return System.currentTimeMillis();
    }

    private void showToast(final String message) {
        Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_LONG).show();
        Log.d(TAG, message);
    }

}
