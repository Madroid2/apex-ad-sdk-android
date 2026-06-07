package com.apexads.sdk.banner.mraid;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.utils.AdLog;

/**
 * MRAID 3.0 JavaScript bridge injected into ad WebViews.
 *
 * MRAID (Mobile Rich media Ad Interface Definitions) is the IAB standard that
 * lets HTML5 creatives interact with the host app container — close, expand,
 * open URLs, query state, etc.
 *
 * Ref: https://www.iab.com/guidelines/mraid/
 */
public final class MRAIDBridge {

    public enum MRAIDState { LOADING, DEFAULT, EXPANDED, HIDDEN, RESIZED }

    public interface MRAIDListener {
        void onClose();
        void onExpand(@Nullable String url);
        void onResize(int width, int height, int offsetX, int offsetY, boolean allowOffscreen);
        void onOpen(@NonNull String url);
        void onLog(@NonNull String message, @NonNull String logLevel);
        void onStateChange(@NonNull MRAIDState state);
    }

    private MRAIDState currentState = MRAIDState.LOADING;
    private boolean isViewable = false;
    private final MRAIDListener listener;

    public MRAIDBridge(@NonNull MRAIDListener listener) {
        this.listener = listener;
    }

    @JavascriptInterface
    public void close() {
        AdLog.d("MRAID: close()");
        currentState = MRAIDState.HIDDEN;
        listener.onClose();
    }

    @JavascriptInterface
    public void expand(@Nullable String url) {
        AdLog.d("MRAID: expand(url=%s)", url);
        currentState = MRAIDState.EXPANDED;
        listener.onExpand(url);
    }

    @JavascriptInterface
    public void resize(int width, int height, int offsetX, int offsetY, boolean allowOffscreen) {
        AdLog.d("MRAID: resize(%dx%d)", width, height);
        currentState = MRAIDState.RESIZED;
        listener.onResize(width, height, offsetX, offsetY, allowOffscreen);
    }

    @JavascriptInterface
    public void open(@NonNull String url) {
        AdLog.d("MRAID: open(%s)", url);
        listener.onOpen(url);
    }

    @JavascriptInterface
    public void log(@NonNull String message, @NonNull String logLevel) {
        AdLog.d("[MRAID] [%s] %s", logLevel, message);
        listener.onLog(message, logLevel);
    }

    @JavascriptInterface
    public String getState() { return currentState.name().toLowerCase(); }

    @JavascriptInterface
    public boolean isViewable() { return isViewable; }

    @JavascriptInterface
    public String getVersion() { return MRAID_VERSION; }

    @JavascriptInterface
    public boolean supports(@NonNull String feature) {
        switch (feature) {
            case "sms": case "tel": case "calendar":
            case "storePicture": case "inlineVideo": case "vpaid":
                return true;
            default:
                return false;
        }
    }

    public void notifyReady(@NonNull WebView webView, @NonNull String placementType) {
        currentState = MRAIDState.DEFAULT;
        isViewable = true;
        String js = "(function(){" +
            "if(typeof mraid!=='undefined'){" +
            "mraid.fireEvent('ready');" +
            "mraid.fireChangeEvent({state:'default',viewable:true,placementType:'" + placementType + "'});" +
            "}})();";
        webView.evaluateJavascript(js, null);
    }

    public void notifyViewableChange(@NonNull WebView webView, boolean viewable) {
        isViewable = viewable;
        webView.evaluateJavascript(
            "if(typeof mraid!=='undefined'){mraid.fireChangeEvent({viewable:" + viewable + "});}",
            null);
    }

    // ── Inline MRAID 3.0 JavaScript polyfill ──────────────────────────────

    public static final String MRAID_VERSION = "3.0";

    /** Returns the full MRAID 3.0 JS polyfill to inject before ad markup. */
    public static String getMRAIDScript() {
        return "(function(){" +
            "var _s='loading',_v=false,_l={},_p={};" +
            "window.mraid={" +
            "getVersion:function(){return '3.0';}," +
            "getState:function(){return _s;}," +
            "isViewable:function(){return _v;}," +
            "supports:function(f){return ['sms','tel','calendar','storePicture','inlineVideo','vpaid'].indexOf(f)>-1;}," +
            "close:function(){ApexMRAID.close();}," +
            "expand:function(u){ApexMRAID.expand(u||'');}," +
            "open:function(u){ApexMRAID.open(u);}," +
            "log:function(m,l){ApexMRAID.log(m,l||'INFO');}," +
            "addEventListener:function(e,f){(_l[e]=_l[e]||[]).push(f);}," +
            "removeEventListener:function(e,f){if(!f){delete _l[e];}else{var a=_l[e]||[];_l[e]=a.filter(function(h){return h!==f;});}}," +
            "fireEvent:function(e,d){(_l[e]||[]).forEach(function(f){try{f(d);}catch(x){}});}," +
            "fireChangeEvent:function(c){Object.assign(_p,c);" +
            "if(c.state)_s=c.state;" +
            "if(typeof c.viewable!=='undefined')_v=c.viewable;" +
            "this.fireEvent('stateChange',_s);this.fireEvent('viewableChange',_v);}," +
            "getPlacementType:function(){return 'inline';}," +
            "getExpandProperties:function(){return{width:window.innerWidth,height:window.innerHeight,useCustomClose:false,isModal:true};}," +
            "setExpandProperties:function(p){}," +
            "getCurrentPosition:function(){return{x:0,y:0,width:window.innerWidth,height:window.innerHeight};}," +
            "getDefaultPosition:function(){return{x:0,y:0,width:window.innerWidth,height:window.innerHeight};}," +
            "getMaxSize:function(){return{width:window.innerWidth,height:window.innerHeight};}," +
            "getScreenSize:function(){return{width:screen.width,height:screen.height};}," +
            "};})();";
    }
}
