package twosra.it.exmob_revisi

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

private lateinit var bookSession: SharedPreferences
private lateinit var editor: SharedPreferences.Editor

class scan_page : AppCompatActivity() {

    private lateinit var kodeUjian: EditText
    private lateinit var btnScan: Button
    private lateinit var btnStart: Button
    private lateinit var btnImage: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.scan_page)

        //=========================SESSION ASSIGMENT====================//
        bookSession = getSharedPreferences("session", MODE_PRIVATE)
        editor = bookSession.edit()

        //===================DISABLE/HIDE OVERLAY=======================//
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }

        //=====================DAFTAR VARIABEL===========================//
        kodeUjian = findViewById(R.id.txtKodeUjian)
        btnScan = findViewById(R.id.btnScan)
        btnStart = findViewById(R.id.btnCari)
        btnImage = findViewById(R.id.btnPickImage)

        /*===================BUTTON SCAN BARCODE CLICK===================*/
        val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                SelesaiScan(result.contents)
            }
        }

        btnScan.setOnClickListener {
            val option = ScanOptions()
            option.setPrompt("Scan Barcode/QR")
            option.setBeepEnabled(true)
            option.setOrientationLocked(false)

            barcodeLauncher.launch(option)
        }

        /* ===================JIKA MEMILIH DI GALERY==============*/
        val PickImage = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    val width = bitmap.width
                    val height = bitmap.height
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                    val source = RGBLuminanceSource(width, height, pixels)
                    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                    val result = MultiFormatReader().decode(binaryBitmap)

                    SelesaiScan(result.text)
                } catch (e: Exception) {
                    Toast.makeText(
                        this, "Gagal melakukan scan, pastikan gambar berupa Barcode dan jelas!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        btnImage.setOnClickListener {
            PickImage.launch("image/*")
        }


        /* ==================START EXAM BUTTON==============*/
        btnStart.setOnClickListener {
            checkingToken()
        }
    }


    fun checkingToken() {
        val tokenUjian = kodeUjian.text.toString().trim()

        if (tokenUjian.isEmpty()) {
            Toast.makeText(this@scan_page, "Masukan Kode Ujian Terlebih dahulu", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val validateToken = bookSession.getBoolean("clear_$tokenUjian", false)
        if (validateToken) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Peringatan!")
                .setMessage("Token Ujian Sudah Digunakan.")
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, which ->
                    dialog.dismiss()
                }.show()
        } else {
            editor.putString("active", tokenUjian)
            editor.apply()
            getExam(tokenUjian)
        }
    }


    /*=======================SCANNER AREA==================*/
    fun SelesaiScan(result: String?) {
        if (!result.isNullOrEmpty()) {
            kodeUjian.setText(result)
            Toast.makeText(this, "Berhasil di scan", Toast.LENGTH_SHORT).show()
        }
    }

    /* =======================HTTP REQ AREA==================*/
    fun getExam(kode: String) {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                chain.proceed(request)
            }
            .build()

        val url = "https://api.tkjsmkn2sragen.my.id/api/exam/$kode"

        /*Membuat Request Jika tidak ada Method yang ditulis artiny Default GET*/
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json") // Penting agar server tahu ini API request
            .addHeader("Content-Type", "application/json")
            .addHeader("ngrok-skip-browser-warning", "any")
            .build()

        /*Response Ketika Failure*/
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@scan_page,
                        "Internal Server Error silakan coba lagi!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(
                call: Call, response: Response
            ) {
                if (response.isSuccessful) {
                    val result = response.body?.string()

                    /*PARSING JSON MENJADI STRING AGAR BISA DITAMPILKAN DI UI*/
                    if (result == null) {
                        runOnUiThread {
                            Toast.makeText(
                                this@scan_page,
                                "Data Ujian kosong",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        try {
                            val jsonObject = JSONObject(result)
                            val data = jsonObject.getJSONObject("data")

                            if (data != null) {
                                val _txtToken = findViewById<EditText>(R.id.txtKodeUjian)

                                /*1. DEKLARASIKAN SEMUA VARIABEL PENAMPUNG JSON*/
                                val judul = data.getString("judul")
                                val linkUjian = data.getString("link_form")
                                val guru = data.getString("guru_name")

                                runOnUiThread {

                                    val intent = Intent(
                                        this@scan_page,
                                        AssigmentForm::class.java
                                    )
                                    /*AMBIL URL DIBERI NAMA URL ISINYA DARI VARIABEL AssigmentLink*/
                                    intent.putExtra("URL", linkUjian)
                                    intent.putExtra("GURU", guru)
                                    intent.putExtra("MAPEL", judul)
                                    /*BUKA HALAMAN*/
                                    startActivity(intent)

                                    _txtToken.text.clear()
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    runOnUiThread {
                        if (response.code == 404) {
                            Toast.makeText(
                                this@scan_page,
                                "Ruang Ujian tidak ditemukan, pastikan token ujian benar!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (response.code == 401) {
                            Toast.makeText(
                                this@scan_page, "Token Ujian Tidak Valid",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
                }
            }
        })
        /* =======================HTTP REQ AREA==================*/

    }
}

