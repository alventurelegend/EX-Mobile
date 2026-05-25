package twosra.it.exmob_revisi

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import okio.Lock

private lateinit var bookSession: SharedPreferences
private lateinit var editor: SharedPreferences.Editor

class AssigmentForm : AppCompatActivity() {

    var dialogShow = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //===================DISABLE/HIDE OVERLAY=======================//
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }

        //=========================SESSION ASSIGMENT====================//
        bookSession = getSharedPreferences("session", Context.MODE_PRIVATE)
        editor = bookSession.edit()

        startLockTask()
        handler.postDelayed({ checkLocked()}, 5000)

        /*BUAT HALAMAN RAHASIA / SECURE SEHINGGA TIDAK DAPAT SS / REKAM LAYAR */
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(R.layout.assigment_form)

        /*================INISIALISASI BUTTON & VARIABEL===============*/
        val btnOut = findViewById<Button>(R.id.btnOut)
        val webView = findViewById<WebView>(R.id.formView)

        /*AMBIL LINK DARI SCAN_PAGE*/
        val url = intent.getStringExtra("URL")
        val guruName = intent.getStringExtra("GURU")
        val mapel = intent.getStringExtra("MAPEL")

        /*AMBIL text DARI XML*/
        val txtMapel = findViewById<TextView>(R.id.textMapel)
        val txtGuru = findViewById<TextView>(R.id.textGuru)

        /*MEMATIKAN POPUP COPY/SALIN*/
        webView.setOnLongClickListener { true }
        webView.isLongClickable = false

        /*DIALOG FUNCTION*/
        class OutAssigment : DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                return activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder
                        .setTitle("PERINGATAN")
                        .setMessage("Jika anda keluar anda tidak dapat mengakses Link Ujian kembali, Pastikan anda sudah menyelesaikan soal!")
                        .setPositiveButton("Oke") { dialog, which ->
                            removeSession()
                            val intent = Intent(this@AssigmentForm, scan_page::class.java)
                            startActivity(intent)
                            stopLockTask()
                            finish()
                        }
                        .setNegativeButton("Tidak") { dialog, which ->
                            //USER CANCEL
                        }
                    builder.create()
                } ?: throw IllegalStateException("Silakan pilih jawaban anda!")
            }

            fun removeSession() {
                val kodeSaatIni = bookSession.getString("active", null)

                if(kodeSaatIni != null) {
                    editor.putBoolean("clear_$kodeSaatIni", true)
                    editor.remove("clear")
                    editor.apply()
                }
            }
        }



        /*BUTTON KELUAR UJIAN*/
        btnOut.setOnClickListener {
            OutAssigment().show(supportFragmentManager, "OUT_DIALOG")
        }


        /*WEBSITE BUTUH JAVASCRIPT DAN DOM*/
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        /*CEGAH KELUAR KE CHROME/BROWSER*/
        webView.webViewClient = WebViewClient()

        /* JIKA URL TIDAK KOSONG LOAD URL DARI KIRIMAN*/
        if (url != null) {
            webView.loadUrl(url)
            txtGuru.setText(guruName)
            txtMapel.setText(mapel)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                /*DIKOSONGKAN AGAR USER TIDAK BISA BALEKK*/
            }
        })
    }

    /*MEMBERIKAN JEDA SEBELUM PENGECEKAN LOCKMODE*/
    val handler = Handler(Looper.getMainLooper())
    var LockShow = false
    fun checkLocked() {
        val ActManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if(!LockShow && ActManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
            LockShow = true
            hideSystemUI(this.window)
        } else {
            if (ActManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                showWarning()
                return
            }
        }
        handler.postDelayed({checkLocked()}, 3000)
    }

    /*FUNGSI JIKA TERJADI KESALAHAN UNTUK MENAMPILKAN WARNING*/
    fun showWarning() {
        if (dialogShow) return
        dialogShow = true

        AlertDialog.Builder(this)
            .setTitle("PERINGATAN")
            .setMessage("Anda Harus Mengijinkan pin aplikasi untuk Melanjutkan Ujian!")
            .setCancelable(false)
            .setPositiveButton("Keluar") {dialog, which ->
                val intent = Intent(this, scan_page::class.java)
                startActivity(intent)
                finish()
            }.show()
    }

    /*FUNGSI UNTUK MENGHILANGKAN NAVIGATION*/
    fun hideSystemUI(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controler = WindowCompat.getInsetsController(window, window.decorView)
        controler.hide(WindowInsetsCompat.Type.navigationBars())
        controler.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

}
