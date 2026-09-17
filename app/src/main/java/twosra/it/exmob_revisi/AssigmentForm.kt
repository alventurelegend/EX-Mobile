package twosra.it.exmob_revisi

import android.provider.Settings
import android.util.Log
import android.net.Network
import android.net.ConnectivityManager
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.BatteryManager
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder


private lateinit var bookSession: SharedPreferences
private lateinit var editor: SharedPreferences.Editor

//==================== PENGECEKAN JARINGAN INTERNET ====================//
class AssigmentForm : AppCompatActivity() {

    var dialogShow = false

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var txtInternet: TextView

    //KE LANDIG PAGE AGAR TIDAK BUG TAMPILAN
    private fun keluarKeLandingPage() {

        stopLockTask()

        val intent = Intent(this, landing_page::class.java)

        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

        startActivity(intent)

        finish()
    }

    //MEMBUKA PENGATURAN WIFI JIKA INTERNET MATI
    private fun bukaPengaturanWifi() {

        MaterialAlertDialogBuilder(this)
            .setTitle("Peringatan Internet Anda Mati!")
            .setMessage(
                "Tenang, progres ujian Anda disimpan. " +
                        "Silakan hubungkan kembali perangkat dengan internet."
            )
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->

                dialog.dismiss()

                keluarKeLandingPage()

                val intent =
                    Intent(Settings.ACTION_WIFI_SETTINGS)

                startActivity(intent)
            }
            .show()
    }

    // KETIKA JARINGAN TERSEDIA & TIDAK
    private fun setupNetworkDetector() {

        txtInternet = findViewById(R.id.txtInternetStatus)

        networkCallback =
            object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: Network) {
                    Log.d(
                        "NETWORK_DETECTOR",
                        "JARINGAN TERSEDIA - ${System.currentTimeMillis()}"
                    )
                    runOnUiThread {
                        txtInternet.text = "INTERNET HIDUP"
                        txtInternet.setTextColor(
                            Color.parseColor("#55ff00")
                        )
                    }
                }

                override fun onLost(network: Network) {
                    Log.d(
                        "NETWORK_DETECTOR",
                        "JARINGAN HILANG - ${System.currentTimeMillis()}"
                    )
                    runOnUiThread {
                        txtInternet.text = "INTERNET MATI"
                        txtInternet.setTextColor(
                            Color.parseColor("#FF1E00")
                        )

                        bukaPengaturanWifi()
                    }
                }
            }


        connectivityManager.registerDefaultNetworkCallback(
            networkCallback
        )
    }


    private lateinit var txtMapel: TextView
    private lateinit var txtGuru: TextView
    private lateinit var webView: WebView
    private lateinit var restart: Button

    var url: String? = ""
    var guruName: String? = ""
    var mapel: String? = ""
    var LockShow = false

    // Handler untuk looping pengecekan Lock Task Mode (fitur keamanan)
    val handler = Handler(Looper.getMainLooper())




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //====== SET CONTENT DI XML DAN UPDATE TAMPILAN BATERAI ==========//
        setContentView(R.layout.assigment_form)
        enableEdgeToEdge()

        //============MENDAFTARKAN CONECTIVITY MANAGER===================//
        connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE)
                    as ConnectivityManager

        setupNetworkDetector()

        /*============ AMBIL LINK DARI SCAN_PAGE ========================*/
        url = intent.getStringExtra("URL")
        guruName = intent.getStringExtra("GURU")
        mapel = intent.getStringExtra("MAPEL")

        /*================== AMBIL TEXT&BUTTON DARI XML ========================*/
        txtMapel = findViewById<TextView>(R.id.textMapel)
        txtGuru = findViewById<TextView>(R.id.textGuru)
        restart = findViewById<Button>(R.id.btnRestart)

        /*================== AMBIL WEB VIEW & SETTINGS WEB VIEW ========================*/
        webView = findViewById<WebView>(R.id.formView)
        webView.settings.apply {
            supportZoom()
            builtInZoomControls = true
            displayZoomControls = false
        }


        //=====DIALOG APAKAH BERSEDIA UNTUK LOCK TASK MODE APK===========//
        class AllowLock : DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                isCancelable = false

                return activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.setTitle("MOHON DIBACA!!")
                        .setMessage("Anda harus mengijinkan Mode Kunci untuk melanjutkan ujian")
                        .setPositiveButton("Ijinkan") { dialog, which ->
                            // MUNCULKAN UJIAN
                            loadWeb()
                            // PENGECEKAN LOCK TASK
                            handler.postDelayed({ checkLocked() }, 1000)
                            // USER MELANJUTKAN UJIAN

                        }.setNegativeButton("Tidak") { dialog, which ->
                            val intent = Intent(this@AssigmentForm, scan_page::class.java)
                            startActivity(intent)
                            stopLockTask()
                            finish()
                        }
                    builder.create()
                } ?: throw IllegalStateException("Silakan pilih jawaban anda!")
            }
        }

        //LOADER SAAT UJIAN TERBUKA
        startLockTask()
        AllowLock().show(supportFragmentManager, "AllowLockDialog")
        updateBatteryOnly()

        //=================== DISABLE/HIDE OVERLAY =======================//
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }

        //========================= SESSION ASSIGMENT ====================//
        bookSession = getSharedPreferences("session", MODE_PRIVATE)
        editor = bookSession.edit()


        //==================BUAT HALAMAN RAHASIA / SECURE SEHINGGA TIDAK DAPAT SS / REKAM LAYAR==============//
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
        )

        /*================ INISIALISASI BUTTON & VARIABEL ===============*/
        val btnOut = findViewById<Button>(R.id.btnOut)

        /*================== MEMATIKAN POPUP COPY/SALIN =================*/
        webView.setOnLongClickListener { true }
        webView.isLongClickable = false



        /*==================== DIALOG FUNCTION =========================*/
        class OutAssigment : DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                return activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.setTitle("PERINGATAN")
                        .setMessage("Jika anda keluar anda tidak dapat mengakses Link Ujian kembali, Pastikan anda sudah menyelesaikan soal!")
                        .setPositiveButton("Oke") { dialog, which ->
                            removeSession()
                            val intent = Intent(this@AssigmentForm, scan_page::class.java)
                            startActivity(intent)
                            stopLockTask()
                            finish()
                        }.setNegativeButton("Tidak") { dialog, which ->
                            // USER CANCEL
                        }
                    builder.create()
                } ?: throw IllegalStateException("Silakan pilih jawaban anda!")
            }


            fun removeSession() {
                val kodeSaatIni = bookSession.getString("active", null)

                if (kodeSaatIni != null) {
                    editor.putBoolean("clear_$kodeSaatIni", true)
                    editor.remove("clear")
                    editor.apply()
                }
            }
        }

        //================KELUAR UJIAN====================//
        btnOut.setOnClickListener {
            OutAssigment().show(supportFragmentManager, "OUT_DIALOG")
        }

        //================RESTART UJIAN===================//
        restart.setOnClickListener {
            loadWeb()
            Toast.makeText(this, "Berhasil Merestart Halaman", Toast.LENGTH_SHORT).show()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            //============ DIKOSONGKAN AGAR USER TIDAK BISA KEMBALI ==============//
            }
        })
    }



    //==========LOADED WEB VIEW FORM=========//
    fun loadWeb() {
        url?.let { urlUjian ->
            webView.loadUrl(urlUjian)
            txtGuru.text = guruName
            txtMapel.text = mapel
        }

        //============AGAR BISA MENAMPILKAN WEB===========//
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true/* CEGAH KELUAR KE CHROME/BROWSER */
        webView.webViewClient = WebViewClient()
    }



    //======================= FUNGSI KHUSUS UPDATE BATERAI =======================//
    fun updateBatteryOnly() {
        val txtBattery = findViewById<TextView>(R.id.txtBattery)
        if (txtBattery != null) {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            txtBattery.text = "Baterai: $batLevel%"
        }
    }

    //=================== LOOPING PENGECEKAN LOCKMODE & UPDATE BATERAI ===================//
    fun checkLocked() {
        val ActManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager

        //========UPDATE BATERAI==========//
        updateBatteryOnly()

        if (!LockShow && ActManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
            LockShow = true
            hideSystemUI(this.window)
        } else {
            if (ActManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                showWarning()
                return
            }
        }
        handler.postDelayed({ checkLocked() }, 1000)
    }

    /* FUNGSI JIKA TERJADI KESALAHAN UNTUK MENAMPILKAN WARNING PIN */
    fun showWarning() {
        if (dialogShow) return
        dialogShow = true

        AlertDialog.Builder(this).setTitle("PERINGATAN")
            .setMessage("Anda Harus Menyematkan aplikasi untuk Melanjutkan Ujian!")
            .setCancelable(false).setPositiveButton("Keluar") { dialog, which ->
                val intent = Intent(this, scan_page::class.java)
                startActivity(intent)
                finish()
            }.show()
    }

    /* FUNGSI UNTUK MENGHILANGKAN NAVIGATION (FULLSCREEN TOTAL) */
    fun hideSystemUI(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controler = WindowCompat.getInsetsController(window, window.decorView)

        // Sembunyikan system bars bawaan agar tidak merusak UI kustom kita
        controler.hide(WindowInsetsCompat.Type.systemBars())
        controler.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /* LIFECYCLE ONDESTROY - Menghapus callback agar tidak bocor (Memory Leak) */
    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        if (::connectivityManager.isInitialized &&
            ::networkCallback.isInitialized
        ) {
            connectivityManager.unregisterNetworkCallback(
                networkCallback
            )
        }

        super.onDestroy()
    }
}