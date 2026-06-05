package com.douyin.tv

import android.webkit.WebView

enum class Shortcut(val key: String, val code: String, val legacyKeyCode: Int) {
    Previous("w", "KeyW", 87),
    Next("s", "KeyS", 83),
    LeftAction("r", "KeyR", 82),
    RightAction("z", "KeyZ", 90),
    OkAction("x", "KeyX", 88),
    PlayerFullscreen("h", "KeyH", 72),
    WebImmersive("y", "KeyY", 89),
    PlayPause(" ", "Space", 32),
    Autoplay("k", "KeyK", 75),
    ClearScreen("j", "KeyJ", 74)
}

class ShortcutDispatcher(private val webView: WebView) {
    fun dispatch(shortcut: Shortcut) {
        val key = JsEscaper.singleQuoted(shortcut.key)
        val code = JsEscaper.singleQuoted(shortcut.code)
        val js =
            "(function(){" +
                "var kc=${shortcut.legacyKeyCode};" +
                "function patch(ev){" +
                "try{" +
                "Object.defineProperty(ev,'keyCode',{get:function(){return kc;}});" +
                "Object.defineProperty(ev,'which',{get:function(){return kc;}});" +
                "}catch(e){}" +
                "}" +
                "var o={key:'$key',code:'$code',bubbles:true,cancelable:true,composed:true};" +
                "function blip(t){" +
                "var ts=[document,window];" +
                "if(document.body)ts.push(document.body);" +
                "if(document.documentElement)ts.push(document.documentElement);" +
                "var ae=document.activeElement;if(ae)ts.push(ae);" +
                "for(var i=0;i<ts.length;i++){" +
                "try{" +
                "if(!ts[i]||!ts[i].dispatchEvent)continue;" +
                "var ev=new KeyboardEvent(t,o);patch(ev);" +
                "ts[i].dispatchEvent(ev);" +
                "}catch(e){}" +
                "}" +
                "}" +
                "blip('keydown');blip('keyup');" +
                "})();"
        webView.evaluateJavascript(js, null)
    }
}
