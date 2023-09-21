# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/ryan/Downloads/adt-bundle-mac-x86_64-20131030/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-optimizationpasses 3
-dontoptimize
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-overloadaggressively
-allowaccessmodification
-useuniqueclassmembernames

-keeppackagenames com.alibaba.sdk.android.httpdns
-keep class com.alibaba.sdk.android.httpdns.HttpDns{*;}
-keep class com.alibaba.sdk.android.httpdns.HttpDnsService{*;}
-keep class com.alibaba.sdk.android.httpdns.SyncService{*;}
-keep class com.alibaba.sdk.android.httpdns.RequestIpType{*;}
-keep class com.alibaba.sdk.android.httpdns.net64.Net64Service{*;}
-keep class com.alibaba.sdk.android.httpdns.DegradationFilter{*;}
-keep class com.alibaba.sdk.android.httpdns.probe.IPProbeItem{*;}
-keep class com.alibaba.sdk.android.httpdns.ILogger{*;}
-keepclasseswithmembers class com.alibaba.sdk.android.httpdns.log.HttpDnsLog {
    public static *** setLogger(***);
    public static *** removeLogger(***);
    public static *** enable(***);
}
-keep class com.alibaba.sdk.android.httpdns.HTTPDNSResult{*;}
-keep class com.alibaba.sdk.android.httpdns.ApiForTest{*;}
-keep class com.alibaba.sdk.android.httpdns.test.** {*;}
-keep class com.alibaba.sdk.android.httpdns.interpret.InterpretHostResponse{*;}
-keep class com.alibaba.sdk.android.httpdns.interpret.ResolveHostResponse{*;}


