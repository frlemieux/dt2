package com.flemieux.dt2;

import java.net.URL;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/*  Log all requests.
*   There is 2 hooks in this class
*   One is handling the okHttp request
*   The other is handling the Volley request and HttpURLConnection */
public class HttpLogger implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.datatheorem.xposedtest";

    public void handleLoadPackage(final LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.equals(TARGET_PACKAGE)) {
            return;
        }

        hookOkHttp(loadPackageParam);
        hookHttpURLAndVolley(loadPackageParam);
    }

    private void hookOkHttp(LoadPackageParam loadPackageParam) {
        try {
            Class<?> okHttpClientClass = XposedHelpers.findClassIfExists("okhttp3.OkHttpClient", loadPackageParam.classLoader);
            if (okHttpClientClass == null) {
                return;
            }
            Class<?> realCallClass = XposedHelpers.findClassIfExists("okhttp3.internal.connection.RealCall", loadPackageParam.classLoader);
            if (realCallClass == null) {
                return;
            }
            // Get the okhttp3.internal.connection.RealCall.getResponseWithInterceptorChain$okhttp() method
            XposedHelpers.findAndHookMethod(realCallClass, "getResponseWithInterceptorChain$okhttp", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        // Get the okhttp3.Request object form the RealCall
                        Object request = XposedHelpers.getObjectField(param.thisObject, "originalRequest");

                        // Get the okhttp3.HttpUrl object from the Request
                        Object url = XposedHelpers.getObjectField(request, "url");
                        String urlString = String.valueOf(url);

                        XposedBridge.log("OkHttp request URL: " + urlString);
                    } catch (Exception e) {
                        // Log error and don't crash
                        XposedBridge.log("Failed to get OkHttp request URL: " + e);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private void hookHttpURLAndVolley(LoadPackageParam loadPackageParam) {
        try {
            Class<?> urlClass = XposedHelpers.findClassIfExists("java.net.URL", loadPackageParam.classLoader);
            if (urlClass == null) {
                return;
            }
            // Hook the openConnection method in the HttpURLConnection class
            XposedHelpers.findAndHookMethod("java.net.URL", loadPackageParam.classLoader, "openConnection", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    URL url = (URL) param.thisObject;
                    String urlString = url.toString();
                    XposedBridge.log("URL.openConnection URL: " + urlString);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}