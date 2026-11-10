package com.tungsten.fcllibrary.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.LocaleList;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class LocaleUtils {

    /**
     * 0: System
     * 1: English
     * 2: Simplified Chinese
     * 3: Russian
     * 4: Brazilian Portuguese
     * 5: Persian
     * 6: Ukrainian
     * 7: German
     * 8: Traditional Chinese (Hong Kong / Cantonese)
     * 11: Traditional Chinese (Taiwan)
     * 12: Vietnamese
     */
    public static final int[] SUPPORTED_LANG_IDS = {0, 1, 2, 11, 8, 12, 3, 6, 7, 4, 5};

    public static Locale RUSSIAN = new Locale("ru");
    public static Locale BRAZILIAN_PORTUGUESE = new Locale("pt", "BR");
    public static Locale PERSIAN = new Locale("fa");
    public static Locale UKRAINIAN = new Locale("uk");
    public static Locale GERMAN = new Locale("de");
    public static Locale HK = new Locale("zh", "HK");
    public static Locale TW = new Locale("zh", "TW");
    public static Locale VIETNAMESE = new Locale("vi");

    private static DateTimeFormatter dateTimeFormatter;

    public static boolean isChinese(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("launcher", Context.MODE_PRIVATE);
        int lang = sharedPreferences.getInt("lang", 0);
        return lang == 2 || lang == 8 || lang == 11 || (lang == 0 && getSystemLocale().getLanguage().startsWith("zh"));
    }

    public static int getLanguage(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("launcher", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("lang", 0);
    }

    public static int getLanguageSpinnerIndex(Context context) {
        int lang = getLanguage(context);
        for (int i = 0; i < SUPPORTED_LANG_IDS.length; i++) {
            if (SUPPORTED_LANG_IDS[i] == lang) return i;
        }
        return 0;
    }

    public static Context setLanguage(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("launcher", Context.MODE_PRIVATE);
        return updateResources(context, sharedPreferences.getInt("lang", 0));
    }

    public static void changeLanguage(Context context, int lang) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("launcher", Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("lang", lang);
        editor.apply();
    }

    private static Context updateResources(Context context, int lang) {
        Locale locale = getLocale(lang);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLocales(new LocaleList(locale));
        return context.createConfigurationContext(configuration);
    }

    public static Locale getLocale(int lang) {
        switch (lang) {
            case 1:
                return Locale.ENGLISH;
            case 2:
                return Locale.CHINA;
            case 3:
                return RUSSIAN;
            case 4:
                return BRAZILIAN_PORTUGUESE;
            case 5:
                return PERSIAN;
            case 6:
                return UKRAINIAN;
            case 7:
                return GERMAN;
            case 8:
                return HK;
            case 11:
                return TW;
            case 12:
                return VIETNAMESE;
            default:
                return getSystemLocale();
        }
    }

    public static Locale getSystemLocale() {
        return LocaleList.getDefault().get(0);
    }

    public static String formatDateTime(Context context, Instant instant) {
        return getDateTimeFormatter(context).format(instant);
    }

    public static DateTimeFormatter getDateTimeFormatter(Context context) {
        if (dateTimeFormatter == null) {
            @SuppressLint("DiscouragedApi") int resId = context.getResources().getIdentifier("world_time", "string", context.getPackageName());
            String time = "EEE, MMM d, yyyy HH:mm:ss";
            if (resId != 0) {
                time = context.getString(resId);
            }
            dateTimeFormatter = DateTimeFormatter.ofPattern(time).withZone(ZoneId.systemDefault());
        }
        return dateTimeFormatter;
    }
}
