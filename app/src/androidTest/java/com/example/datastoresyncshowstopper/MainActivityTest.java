package com.example.datastoresyncshowstopper;

import android.util.Log;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.generated.model.AmplifyBug;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.hub.SubscriptionToken;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {
    private static final String ID = UUID.randomUUID().toString();
    private static final String TAG = "amplify:bug";
    private static final String EMAIL = ID + "@test.com";
    private static final String LOGIN = ID;
    private static final String PASSWORD = "EXAMPLE12354";

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void test() {
        Log.i(TAG, "starting test");

        checkAuth();
        CompletableFuture<HubEvent<?>> readyFuture = new CompletableFuture<>();
        Amplify.Hub.subscribe(HubChannel.DATASTORE,
                event -> DataStoreChannelEventName.SYNC_QUERIES_READY.toString().equals(event.getName()),
                readyFuture::complete);

        Amplify.DataStore.start(
                () -> Log.d("main", "Amplify DataStore sync explicitly initiated."),
                error -> Log.e("main", "Amplify DataStore sync did not initiate.", error)
        );

        try {
            readyFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            fail(e.getMessage());
        }

        CompletableFuture<AmplifyBug> createFuture = new CompletableFuture<>();
        CompletableFuture<AmplifyBug> updateFuture = new CompletableFuture<>();

        AmplifyBug createRecord = AmplifyBug.builder().updated(Boolean.FALSE).build();
        Amplify.DataStore.save(
                createRecord,
                success -> createFuture.complete(success.item()),
                createFuture::completeExceptionally
        );
        try {
            AmplifyBug createRecordReturned = createFuture.get();

            AmplifyBug updateRecord = createRecordReturned.copyOfBuilder().updated(Boolean.TRUE).build();
            Amplify.DataStore.save(
                    updateRecord,
                    success -> updateFuture.complete(success.item()),
                    updateFuture::completeExceptionally
            );
            updateFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "failed to transact record", e);
            fail(e.getMessage());
        }

        try {
            Thread.sleep(5000); // sleep for 5 seconds to accumulate logs
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "finishing test");
    }

    private boolean checkAuth() {
        if (null == LOGIN || null == PASSWORD || null == EMAIL) {
            Log.i(TAG, "no credentials to Auth");
            return false;
        }
        try {
            AuthSession session = getSession();
            if(session.isSignedIn()) {
                Log.i(TAG, "isSignedIn");
                return true;
            }
        }
        catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "authSession exception", e);
            return false;
        }

        try {
            AuthSignInResult signInResult = signIn(LOGIN, PASSWORD);
            if(signInResult.isSignInComplete()) {
                Log.i(TAG, "SignInComplete");
            } else {
                Log.w(TAG, "Unexpected SignIn nextStep: " + signInResult.getNextStep());
            }
            return true;
        } catch (ExecutionException | InterruptedException e) {
            Throwable t = e.getCause();
            // if the excetion isn't use not found then we are done otherwise, we can continue to try to signUp
            if (null != t && !"User not found in the system.".equals(t.getMessage())) {
                Log.e(TAG, "signIn exception", e);
                return false;
            }
        }
        try {
            AuthSignUpResult signUpResult = signUp(LOGIN, PASSWORD, EMAIL);
            if (signUpResult.isSignUpComplete()) {
                // call to get a session now
                return this.checkAuth();
            } else {
                Log.w(TAG, "Unexpected signUp nextStep: " + signUpResult.getNextStep());
                return false;
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Failed signUp", e);
            return false;
        }
    }


    private static AuthSession getSession() throws ExecutionException, InterruptedException
    {
        CompletableFuture<AuthSession> qsession = new CompletableFuture<>();
        Amplify.Auth.fetchAuthSession(qsession::complete, qsession::completeExceptionally);
        return qsession.get();
    }

    private static AuthSignInResult signIn(String login, String password) throws ExecutionException, InterruptedException
    {
        CompletableFuture<AuthSignInResult> qresult = new CompletableFuture<>();
        Amplify.Auth.signIn(login, password, qresult::complete, qresult::completeExceptionally);
        return qresult.get();
    }

    private static AuthSignUpResult signUp(String login, String password, String email) throws ExecutionException, InterruptedException
    {
        AuthSignUpOptions options = AuthSignUpOptions.builder().userAttribute(AuthUserAttributeKey.email(), email).build();
        CompletableFuture<AuthSignUpResult> qresult = new CompletableFuture<>();
        Amplify.Auth.signUp(login, password, options, qresult::complete, qresult::completeExceptionally);
        return qresult.get();
    }
}