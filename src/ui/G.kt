package ca.kotlin.ui

import ca.kotlin.core.*


/**
 * modules/uiCore/G.js 的 Kotlin/JS 版本。
 *
 * G 是一组 Android UI 类别名 + 三个环境相关的初始化函数。
 * onCreate / initialize 会通过动态 this 访问 G 自身，因此用薄 JS wrapper
 * 把真实的 this 传回 Kotlin；imports 仍在安装时求值，保持与旧文件相同的时序。
 */
private val gApi: dynamic = js("({})")

private val gTruthyJs: dynamic = js("function (v) { return !!v; }")
private val gThisJs: dynamic = js("function (f) { return function () { return f(this); }; }")
private val gCtxUiJs: dynamic = js(
    "function (c) { return c.runOnUiThread ? c.runOnUiThread.bind(c) : null; }"
)
private val gMakeFallbackUiJs: dynamic = js(
    "function (uiThread) {" +
        " return function (f) {" +
        "  if (uiThread != java.lang.Thread.currentThread()) { gHandler.post(f); } else { f(); }" +
        " };" +
        "}"
)

private fun gOnCreate(self: dynamic): dynamic {
    val metrics = js("ctx").getResources().getDisplayMetrics()
    self.screenHeight = metrics.heightPixels
    self.screenWidth = metrics.widthPixels
    self.dp = metrics.density
    self.sp = metrics.scaledDensity
    self.scaleFactor = self.sp / self.dp

    val ctxBoundUi = gCtxUiJs(js("ctx"))
    if (ctxBoundUi != null) {
        self.ui = ctxBoundUi
    } else if (js("MapScript").host == "Android") {
        self.ui = js("ScriptInterface").runOnUiThread.bind(js("ScriptInterface"))
    } else {
        val uiThread = js("gHandler").getLooper().getThread()
        self.ui = gMakeFallbackUiJs(uiThread)
    }
    return js("undefined")
}

private fun gInitialize(self: dynamic): dynamic {
    self.supportFloat = js("MapScript").host == "AutoJs" || js("MapScript").host == "Android"
    self.shouldFloat = self.supportFloat
    if (gTruthyJs(self.supportFloat)) {
        if (!gTruthyJs(js("SettingsCompat").ensureCanFloat(true))) {
            self.supportFloat = false
            if (js("MapScript").host == "Android") {
                val activity = js("ScriptInterface").getBindActivity()
                if (activity != null) {
                    js("MapScript").global.ctx = activity
                } else {
                    self.supportFloat = true
                    js("ScriptInterface").quit()
                }
            }
        }
    }

    self.style = "Material"
    js("ctx").setTheme(android.R.style.Theme_Material_Light)
    return js("undefined")
}

/** 原文件在对象字面量构造时一次性求值这些 Android 类别名。 */
private fun installGImports() {
    gApi.AbsListView = android.widget.AbsListView
    gApi.AbsoluteSizeSpan = android.text.style.AbsoluteSizeSpan
    gApi.AccelerateInterpolator = android.view.animation.AccelerateInterpolator
    gApi.AdapterView = android.widget.AdapterView
    gApi.AlignmentSpan = android.text.style.AlignmentSpan
    gApi.AlphaAnimation = android.view.animation.AlphaAnimation
    gApi.AnimatedImageDrawable = android.graphics.drawable.AnimatedImageDrawable
    gApi.Animation = android.view.animation.Animation
    gApi.AnimationSet = android.view.animation.AnimationSet
    gApi.BackgroundColorSpan = android.text.style.BackgroundColorSpan
    gApi.Bitmap = android.graphics.Bitmap
    gApi.BitmapDrawable = android.graphics.drawable.BitmapDrawable
    gApi.BitmapFactory = android.graphics.BitmapFactory
    gApi.BitmapShader = android.graphics.BitmapShader
    gApi.BulletSpan = android.text.style.BulletSpan
    gApi.Button = android.widget.Button
    gApi.Canvas = android.graphics.Canvas
    gApi.CheckBox = android.widget.CheckBox
    gApi.Color = android.graphics.Color
    gApi.ColorDrawable = android.graphics.drawable.ColorDrawable
    gApi.CompoundButton = android.widget.CompoundButton
    gApi.CycleInterpolator = android.view.animation.CycleInterpolator
    gApi.DecelerateInterpolator = android.view.animation.DecelerateInterpolator
    gApi.Drawable = android.graphics.drawable.Drawable
    gApi.EditText = android.widget.EditText
    gApi.EditorInfo = android.view.inputmethod.EditorInfo
    gApi.ForegroundColorSpan = android.text.style.ForegroundColorSpan
    gApi.FrameLayout = android.widget.FrameLayout
    gApi.Gravity = android.view.Gravity
    gApi.GridView = android.widget.GridView
    gApi.HorizontalScrollView = android.widget.HorizontalScrollView
    gApi.Html = android.text.Html
    gApi.ImageDecoder = android.graphics.ImageDecoder
    gApi.ImageSpan = android.text.style.ImageSpan
    gApi.ImageView = android.widget.ImageView
    gApi.InputFilter = android.text.InputFilter
    gApi.InputMethodManager = android.view.inputmethod.InputMethodManager
    gApi.InputType = android.text.InputType
    gApi.Layout = android.text.Layout
    gApi.LinearLayout = android.widget.LinearLayout
    gApi.LinkMovementMethod = android.text.method.LinkMovementMethod
    gApi.ListAdapter = android.widget.ListAdapter
    gApi.ListView = android.widget.ListView
    gApi.MotionEvent = android.view.MotionEvent
    gApi.Paint = android.graphics.Paint
    gApi.Path = android.graphics.Path
    gApi.PixelFormat = android.graphics.PixelFormat
    gApi.PopupWindow = android.widget.PopupWindow
    gApi.PorterDuff = android.graphics.PorterDuff
    gApi.PorterDuffXfermode = android.graphics.PorterDuffXfermode
    gApi.R = android.R
    gApi.Rect = android.graphics.Rect
    gApi.RelativeSizeSpan = android.text.style.RelativeSizeSpan
    gApi.ScaleAnimation = android.view.animation.ScaleAnimation
    gApi.ScrollView = android.widget.ScrollView
    gApi.SeekBar = android.widget.SeekBar
    gApi.Selection = android.text.Selection
    gApi.Shader = android.graphics.Shader
    gApi.Space = android.widget.Space
    gApi.SpanWatcher = android.text.SpanWatcher
    gApi.SpannableString = android.text.SpannableString
    gApi.SpannableStringBuilder = android.text.SpannableStringBuilder
    gApi.Spanned = android.text.Spanned
    gApi.StrikethroughSpan = android.text.style.StrikethroughSpan
    gApi.StyleSpan = android.text.style.StyleSpan
    gApi.SubscriptSpan = android.text.style.SubscriptSpan
    gApi.SuperscriptSpan = android.text.style.SuperscriptSpan
    gApi.Surface = android.view.Surface
    gApi.TabStopSpan = android.text.style.TabStopSpan
    gApi.TableLayout = android.widget.TableLayout
    gApi.TableRow = android.widget.TableRow
    gApi.TextUtils = android.text.TextUtils
    gApi.TextView = android.widget.TextView
    gApi.TextWatcher = android.text.TextWatcher
    gApi.Toast = android.widget.Toast
    gApi.TranslateAnimation = android.view.animation.TranslateAnimation
    gApi.Typeface = android.graphics.Typeface
    gApi.TypefaceSpan = android.text.style.TypefaceSpan
    gApi.URLSpan = android.text.style.URLSpan
    gApi.UnderlineSpan = android.text.style.UnderlineSpan
    gApi.ValueAnimator = android.animation.ValueAnimator
    gApi.View = android.view.View
    gApi.ViewConfiguration = android.view.ViewConfiguration
    gApi.ViewGroup = android.view.ViewGroup
    gApi.WebView = android.webkit.WebView
    gApi.WindowManager = android.view.WindowManager
}

fun installG() {
    installGImports()

    // onCreate / initialize 都通过 G 自身读写属性，保留原来的动态 this。
    gApi.onCreate = gThisJs { self: dynamic -> gOnCreate(self) }
    gApi.initialize = gThisJs { self: dynamic -> gInitialize(self) }

    if (js("typeof MapScript !== 'undefined'")) {
        MapScript.loadModule("G", gApi)
    }
}
