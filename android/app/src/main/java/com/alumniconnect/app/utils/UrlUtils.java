package com.alumniconnect.app.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class UrlUtils {

    /**
     * Safely opens an external URL using an Intent.ACTION_VIEW.
     * Handles missing protocols (prepends https://), trims whitespace,
     * validates the URI, and safely handles devices with no browser installed.
     * Never crashes the application.
     */
    public static void openUrlSafely(Context context, String rawUrl) {
        if (context == null) return;

        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            Toast.makeText(context, "No web link provided.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = rawUrl.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            Uri uri = Uri.parse(url);
            if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
                Toast.makeText(context, "Invalid link format.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No browser found to open link.", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(context, "Permission denied opening link.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open link.", Toast.LENGTH_SHORT).show();
        }
    }
}
