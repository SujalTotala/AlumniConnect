package com.alumniconnect.app.network;

import android.content.Context;
import androidx.annotation.NonNull;
import com.alumniconnect.app.utils.SessionManager;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final SessionManager sessionManager;

    public AuthInterceptor(Context context) {
        this.sessionManager = new SessionManager(context);
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

        // Handle 401 Unauthorized globally
        if (response.code() == 401) {
            sessionManager.clearSession();
        }

        return response;
    }
}
