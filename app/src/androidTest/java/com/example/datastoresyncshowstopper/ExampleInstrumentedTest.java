package com.example.datastoresyncshowstopper;

import android.content.Context;
import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.datastore.AWSDataStorePlugin;
import com.amplifyframework.datastore.generated.model.AmplifyBug;
import com.amplifyframework.logging.AndroidLoggingPlugin;
import com.amplifyframework.logging.LogLevel;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.*;

public class ExampleInstrumentedTest {
    private static final String TAG = "amplify:bug";
    private static final String EMAIL = "test1@test.com";
    private static final String LOGIN = "test001";
    private static final String PASSWORD = "EXAMPLE";

    @Test
    public void test() {
        Log.i(TAG, "starting test");

        try {
            Context context = InstrumentationRegistry.getInstrumentation().getContext();
            Amplify.addPlugin(new AndroidLoggingPlugin(LogLevel.VERBOSE));
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.addPlugin(new AWSDataStorePlugin());
            Amplify.addPlugin(new AWSApiPlugin());
            Amplify.configure(AmplifyConfiguration.builder(context).devMenuEnabled(false).build(), context);
            Log.d(TAG, "Amplify configured.");
        } catch (AmplifyException e) {
            Log.w(TAG, "Amplify Failed to Configure; this is likely caused by a duplicate call to Amplify.configure().");
        }

        checkAuth();

        try {
            CompletableFuture<AmplifyBug> createFuture = new CompletableFuture<>();
            CompletableFuture<AmplifyBug> updateFuture = new CompletableFuture<>();

            AmplifyBug createRecord = AmplifyBug.builder().updated(Boolean.FALSE).build();
            Amplify.DataStore.save(
                    createRecord,
                    success -> createFuture.complete(success.item()),
                    createFuture::completeExceptionally
            );

            AmplifyBug createRecordReturned = createFuture.get();

            AmplifyBug updateRecord = createRecordReturned.copyOfBuilder().updated(Boolean.TRUE).build();
            Amplify.DataStore.save(
                    updateRecord,
                    success -> updateFuture.complete(success.item()),
                    updateFuture::completeExceptionally
            );
            updateFuture.get();
            Thread.sleep(5000); // sleep for 5 seconds to accumulate logs
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "failed to transact record", e);
            fail(e.getMessage());
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