package com.alumniconnect.app.network;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.alumniconnect.app.activities.LoginActivity;
import com.alumniconnect.app.utils.SessionManager;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private static final AtomicBoolean isRedirecting = new AtomicBoolean(false);

    private final Context appContext;
    private final SessionManager sessionManager;

    public AuthInterceptor(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder builder = originalRequest.newBuilder();

        String token = sessionManager.getToken();
        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }

        builder.addHeader("Accept", "application/json");
        builder.addHeader("Content-Type", "application/json");

        Response response = chain.proceed(builder.build());

        // Centralized 401 Unauthorized handling
        if (response.code() == 401) {
            String path = originalRequest.url().encodedPath();
            boolean isAuthEndpoint = path.contains("auth/login") || path.contains("auth/register");

            // Only redirect if this is not a direct login/register attempt
            if (!isAuthEndpoint && isRedirecting.compareAndSet(false, true)) {
                sessionManager.clearSession();

                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        Intent intent = new Intent(appContext, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("session_expired", true);
                        appContext.startActivity(intent);
                    } catch (Exception ignored) {
                    } finally {
                        // Reset guard after short delay to permit future sessions
                        new Handler(Looper.getMainLooper()).postDelayed(() -> isRedirecting.set(false), 2000);
                    }
                });
            } else if (isAuthEndpoint) {
                // If login attempt returned 401, clear session without redirect loop
                sessionManager.clearSession();
            }
        }

        return response;
    }
}
