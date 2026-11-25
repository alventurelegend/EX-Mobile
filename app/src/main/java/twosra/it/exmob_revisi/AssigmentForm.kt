package twosra.it.exmob_revisi

import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Network
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder


private lateinit var bookSession: SharedPreferences
private lateinit var editor: SharedPreferences.Editor

//UNTUK MEMUTAR SOUND
private var mediaPlayer: MediaPlayer? = null

//JIKA APLIKASI TIDAK FOCUS
private var NotFocus = false

//UNTUK KONDISI dialog bawaan aplikasi
private var interDialog = false

//NILAI JUMLAH KECURANGAN
private var cheat: Int = 0

//PENGECEKAN LOCK TASK
private var checkCount = 0


//==================== PENGECEKAN JARINGAN INTERNET ====================//
class AssigmentForm : AppCompatActivity() {

    // ========= VARIABEL PENAMPUNG DATA UJIAN YANG DIKIRIMKAN DARI HALAMAN SCAN ========= //
    private lateinit var txtMapel: TextView
    private lateinit var txtGuru: TextView
    private lateinit var webView: WebView
    private lateinit var restart: Button

    var url: String? = ""
    var guruName: String? = ""
    var mapel: String? = ""
    // ====== END VARIABEL PENAMPUNG DATA UJIAN YANG DIKIRIMKAN DARI HALAMAN SCAN ========= //


    // ====== VARIABEL YANG MENANGANI PENGECEKAN BERULANG LOCK TASK MODE =========//
    var LockShow = false
    val handler = Handler(Looper.getMainLooper())
    var dialogShow = false
    // ====== END VARIABEL YANG MENANGANI PENGECEKAN BERULANG LOCK TASK MODE =====//


    // ===== VARIABEL UNTUK MENJALANKAN PENGECEKAN BERKALA PADA VOLUME ===========//
    private val stayVolume = object : Runnable {
        override fun run() {
            maxVolume()
            handler.postDelayed(this, 1000)
        }
    }
    // =============================== END ======================================//


    // ========= VARIABEL UNTUK MENAMPILKAN INFORMASI INTERNET ========== //
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var txtInternet: TextView
    // ========= END VARIABEL UNTUK MENAMPILKAN INFORMASI INTERNET ========== //


    //========= MEMBUKA PENGATURAN WIFI JIKA INTERNET MATI ==========//
    private fun bukaPengaturanWifi() {

        MaterialAlertDialogBuilder(this).setTitle("Peringatan Internet Anda Mati!").setMessage(
                "Tenang, progres ujian Anda disimpan. " + "Silakan hubungkan kembali perangkat dengan internet."
            ).setCancelable(false).setPositiveButton("OK") { dialog, _ ->

                dialog.dismiss()

                keluarKeLandingPage()

                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)

                startActivity(intent)
            }.show()
    }
    //========= END MEMBUKA PENGATURAN WIFI JIKA INTERNET MATI ==========//


    //========== MENGATUR KONDISI TEXT KETIKA JARINGAN TERSEDIA & TIDAK ========//
    private fun setupNetworkDetector() {

        txtInternet = findViewById(R.id.txtInternetStatus)

        networkCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                Log.d(
                    "NETWORK_DETECTOR", "JARINGAN TERSEDIA - ${System.currentTimeMillis()}"
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
                    "NETWORK_DETECTOR", "JARINGAN HILANG - ${System.currentTimeMillis()}"
                )
                runOnUiThread {
                    txtInternet.text = "INTERNET MATI"
                    txtInternet.setTextColor(
                        Color.parseColor("#FF1E00")
                    )
                    interDialog = true
                    bukaPengaturanWifi()
                }
            }
        }

        connectivityManager.registerDefaultNetworkCallback(
            networkCallback
        )
    }
    //========== END MENGATUR KONDISI TEXT KETIKA JARINGAN TERSEDIA & TIDAK ========//


    override fun onCreate(savedInstanceState: Bundle?) {

        //================= MEMAKSA TEMA KE TERANG =====================//
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        //============================ END =============================//

        super.onCreate(savedInstanceState)
        //===========MEMASTIKAN VOLUME USER FULL SAAT MASUK APLIKASI============
        maxVolume()
        //======END======//

        setContentView(R.layout.assigment_form)
        enableEdgeToEdge()


        // ============== MEMANGGIL PENGECEKAN VOLUME TIAP 1s ================//
        handler.post(stayVolume)
        //======END======//


        // ====================== KLIK KELUAR UJIAN =====================//

        // ==============END================//


        //============MENDAFTARKAN CONECTIVITY MANAGER===================//
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        setupNetworkDetector()
        //======END======//


        /*============ AMBIL LINK DARI SCAN_PAGE ========================*/
        url = intent.getStringExtra("URL")
        guruName = intent.getStringExtra("GURU")
        mapel = intent.getStringExtra("MAPEL")
        //======END======//


        /*================== AMBIL TEXT&BUTTON DARI XML ========================*/
        txtMapel = findViewById<TextView>(R.id.textMapel)
        txtGuru = findViewById<TextView>(R.id.textGuru)
        restart = findViewById<Button>(R.id.btnRestart)
        //======END======//


        /*================== AMBIL WEB VIEW & SETTINGS WEB VIEW ================*/
        webView = findViewById<WebView>(R.id.formView)
        webView.settings.apply {
            supportZoom()
            builtInZoomControls = true
            displayZoomControls = false
        }
        //======END======//


        //===============DIALOG APAKAH BERSEDIA UNTUK LOCK TASK MODE APK===========//
        class AllowLock : DialogFragment() {
            override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                isCancelable = false

                return activity?.let {
                    val builder = MaterialAlertDialogBuilder(it)
                    builder.setTitle("MOHON DIBACA!!").setMessage(
                            """
                            1. Pastikan Internet dan Baterai dalam kondisi cukup.
                            2. Membuka Layar belah atau aplikasi mengambang akan terdeteksi.
                            3. Melakukan pelanggaran lebih dari 3x akan dikeluarkan dari Ujian dan memicu Alarm.
                            4. Token ujian hanya bisa digunakan 1x.
                            5. Jika keluar dari aplikasi karena HP crash, silakan masuk dengan Token Yang sama.
                            6. Kerjakan Ujian dengan teliti.
                        """.trimIndent()
                        ).setPositiveButton("OK") { dialog, which ->
                            // MUNCULKAN UJIAN
                            loadWeb()
                            // PENGECEKAN LOCK TASK
                            handler.postDelayed({ checkLocked() }, 1000)
                            // USER MELANJUTKAN UJIAN
                        }
                    builder.create()
                } ?: throw IllegalStateException("Silakan pilih jawaban anda!")
            }
        }
        //===============END DIALOG APAKAH BERSEDIA UNTUK LOCK TASK MODE APK===========//


        //===================== LOADER SAAT UJIAN TERBUKA ==================//
        startLockTask()
        interDialog = true
        AllowLock().show(supportFragmentManager, "AllowLockDialog")
        updateBatteryOnly()
        //================== END LOADER SAAT UJIAN TERBUKA ==================//


        //=================== DISABLE/HIDE OVERLAY =======================//
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }
        //============== END DISABLE/HIDE OVERLAY =======================//


        //========================= SESSION ASSIGMENT ====================//
        bookSession = getSharedPreferences("session", MODE_PRIVATE)
        editor = bookSession.edit()


        //==================BUAT HALAMAN RAHASIA / SECURE SEHINGGA TIDAK DAPAT SS / REKAM LAYAR==============//
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE
        )
        //======END======//


        //================ INISIALISASI BUTTON & VARIABEL ===============//
        val btnOut = findViewById<Button>(R.id.btnOut)
        //======END======//


        //================== MEMATIKAN POPUP COPY/SALIN =================//
        webView.setOnLongClickListener { true }
        webView.isLongClickable = false
        //======END======//


        //================KELUAR UJIAN====================//
        btnOut.setOnClickListener {
            interDialog = true
            finishExam()
        }
        //======END======//


        //================RESTART UJIAN===================//
        restart.setOnClickListener {
            loadWeb()
            Toast.makeText(this, "Berhasil Merestart Halaman", Toast.LENGTH_SHORT).show()
        }
        //======END======//


        //==========MEMATIKAN/DISABLE BUTTON KEMBALI=========//
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                //============ DIKOSONGKAN AGAR USER TIDAK BISA KEMBALI ==============//
            }
        })
        //======END======//
    }

    //========FUNCTION UNTUK MENDETEKSI APAKAH APLIKASI FOCUS========//
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (!hasFocus) {
            if (interDialog) {
                interDialog = false
                return
            }
            NotFocus = true
            if (anomalyCheck()) {
                if (cheat < 3) {
                    cheat += 1
                    alertFloat()
                } else {
                    alert()
                    warningFloat()
                }
            }
        }
    }
    //======END======//


    //=======MENYALAKAN SOUND ALERT========//
    override fun onResume() {
        super.onResume()
        maxVolume()
    }
    //======END======//


    //=======MENDISABLE BUTTON VOLUME========//
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            //JIKA KEY/TOMBOL VOLUME EVENT DI NAIKAN ATAU TURUNKAN PAKSA KE MAKSIMAL
            maxVolume()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    //======END======//


    //============ LIFECYCLE ONDESTROY ==========//
    override fun onDestroy() {

        //========MENGHAPUS CALLBACK INTERNET CHECKING=======//
        handler.removeCallbacksAndMessages(null)

        if (::connectivityManager.isInitialized && ::networkCallback.isInitialized) {
            connectivityManager.unregisterNetworkCallback(
                networkCallback
            )
        }

        //=======MEMATIKAN SOUND ALERT========//
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
    //======END======//


    //================== FUNCTION PEMUTARAN ALERT ======================//
    fun alert() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_examob)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

    }
    //======END======//


    //================== FUNCTION STOPING ALERT ======================//
    fun stopAlert() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    //======END======//


    //=============FUNCTION CHECK APLIKASI ON FOCUS / TIDAK============//
    fun anomalyCheck(): Boolean {

        val result: Boolean
        result = NotFocus
        NotFocus = false
        return result

    }
    //======END======//


    //======FUNCTION MEN MAX KAN VOLUME=======//
    fun maxVolume() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volumeMaks = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeMaks, 0)
        Log.d("MAKSIMAL AUDIO", "Berhasil memaksimalkan audio")
    }
    //======END======//


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
    //======END======//


    //======================= FUNGSI KHUSUS UPDATE BATERAI =======================//
    fun updateBatteryOnly() {
        val txtBattery = findViewById<TextView>(R.id.txtBattery)
        if (txtBattery != null) {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            txtBattery.text = "Baterai: $batLevel%"
        }
    }
    //======END======//


    //======================= PENDETEKSI KHUSUS TIAP DETIK LOCK TASK DETECTION =========================//
    fun checkLocked() {
        val ActManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager

        // ======== UPDATE BATERAI ========
        updateBatteryOnly()

        val isLocked = ActManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE

        checkCount++

        if (isLocked) {

            // Lock Task masih aktif
            if (!LockShow) {
                LockShow = true
                hideSystemUI(this.window)
            }

        } else {

            // Lock Task TIDAK aktif

            if (checkCount == 1) {
                // Terlepas pada pengecekan pertama
                showWarningFirst()
            } else {
                // Terlepas setelah detik pertama
                showWarningSecond()
            }
        }

        handler.postDelayed({ checkLocked() }, 1000)
    }
    //======END======//


    /* ============================= FUNGSI JIKA USER MENCOPOT LOCK APK DI AWAL ============================= */
    fun showWarningFirst() {
        if (dialogShow) return
        dialogShow = true

        MaterialAlertDialogBuilder(this).setTitle("PERINGATAN")
            .setMessage("Silakan aktifkan Kunci Layar/Pin Aplikasi yang muncul untuk melanjutkan ujian")
            .setCancelable(false).setPositiveButton("Keluar") { dialog, which ->
                val intent = Intent(this, scan_page::class.java)
                startActivity(intent)
                finish()
            }.show()
    }
    //======END======//


    // ============================== FUNGSI JIKA USER MENCOBA MENCOPOT LOCK APK ========================= //
    fun showWarningSecond() {
        if (dialogShow) return
        dialogShow = true

        MaterialAlertDialogBuilder(this).setTitle("PERINGATAN")
            .setMessage("Anda telah berusaha membuka kunci aplikasi, mohon jangan di ulangi lagi silakan login kembali untuk melanjutkan ujian")
            .setCancelable(false).setPositiveButton("Keluar") { dialog, which ->
                val intent = Intent(this, scan_page::class.java)
                startActivity(intent)
                finish()
            }.show()
    }
    //======END======//


    // ============================== PERINGATAN FLOATING APP SATUAN ================================== //
    fun alertFloat() {
        MaterialAlertDialogBuilder(this).setTitle("PERINGATAN!")
            .setMessage("Kamu terdeteksi melakukan kecurangan sebanyak ${cheat}x, jika melakukan kecurangan lebih dari 3x kamu akan di keluarkan dari Ujian!")
            .setCancelable(false).setPositiveButton("OK") { dialog, which ->
            }.show()
    }
    //======END======//


    // ============================== FUNGSI PERINGATAN TERAKHIR FLOATING APP ========================= //
    fun warningFloat() {
        // =========== MENGAMBIL BUTTON DARI DIALOG WARNING DARI XML DAN MENANGANI CLICK LISTENER NYA =====================//
        findViewById<LinearLayout>(R.id.lapisanWarningFloat).visibility = View.VISIBLE

        findViewById<Button>(R.id.btnKeluarWarning).setOnClickListener {
            stopAlert()
            removeSession()
            val intent = Intent(this, scan_page::class.java)
            startActivity(intent)
            stopLockTask()
            finish()
            cheat = 0
        }
    }
    //======END======//


    // ======================== FUNGSI UNTUK MENAMPILKAN DIALOG SELESAI EXAM============ //
    fun finishExam() {
        findViewById<FrameLayout>(R.id.dialogPeringatan).visibility = View.VISIBLE

        findViewById<Button>(R.id.btnSelesai).setOnClickListener {
            removeSession()
            val intent = Intent(this@AssigmentForm, scan_page::class.java)
            startActivity(intent)
            stopLockTask()
            finish()
            cheat = 0
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            findViewById<FrameLayout>(R.id.dialogPeringatan).visibility = View.GONE
        }
    }
    //======END======//


    // ================================== FUNGSI UNTUK MENGHILANGKAN NAVIGATION (FULLSCREEN TOTAL) ====================== //
    fun hideSystemUI(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controler = WindowCompat.getInsetsController(window, window.decorView)

        // Sembunyikan system bars bawaan agar tidak merusak UI kustom kita
        controler.hide(WindowInsetsCompat.Type.systemBars())
        controler.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    //======END======//


    // ========= FUNCTION REMOVE SESI KODE ========= //
    fun removeSession() {
        val kodeSaatIni = bookSession.getString("active", null)

        if (kodeSaatIni != null) {
            editor.putBoolean("clear_$kodeSaatIni", true)
            editor.remove("clear")
            editor.apply()
        }
    }
    //======END======//


    //======== KE LANDING PAGE AGAR TIDAK BUG TAMPILAN ======= //
    fun keluarKeLandingPage() {

        stopLockTask()

        val intent = Intent(this, landing_page::class.java)

        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        startActivity(intent)

        finish()
    }
    //======END======//
}