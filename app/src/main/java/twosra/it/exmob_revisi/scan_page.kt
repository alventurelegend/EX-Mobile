            package twosra.it.exmob_revisi

            import android.app.ActivityManager
            import android.content.Context
            import android.content.Intent
            import android.media.MediaDataSource
            import android.net.Uri
            import android.os.Bundle
            import android.os.Handler
            import android.os.Looper
            import android.provider.MediaStore
            import android.util.Log
            import android.widget.Button
            import android.widget.EditText
            import android.widget.Toast
            import androidx.activity.enableEdgeToEdge
            import androidx.activity.result.contract.ActivityResultContracts
            import androidx.appcompat.app.AppCompatActivity
            import okhttp3.*
            import com.journeyapps.barcodescanner.ScanContract
            import com.journeyapps.barcodescanner.ScanOptions
            import java.io.IOException
            import org.json.JSONException
            import org.json.JSONObject
            import androidx.appcompat.app.AlertDialog
            import com.google.zxing.BinaryBitmap
            import com.google.zxing.MultiFormatReader
            import com.google.zxing.RGBLuminanceSource
            import com.google.zxing.common.HybridBinarizer

            class scan_page : AppCompatActivity() {

                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    enableEdgeToEdge()
                    setContentView(R.layout.scan_page)

                    /*1. =====================MENDEKLARASIKAN EDITEXT & BUTTON====================*/
                    val _KodeUjian = findViewById<EditText>(R.id.txtKodeUjian)
                    val _btnScan = findViewById<Button>(R.id.btnScan)
                    val _btnStart = findViewById<Button>(R.id.btnCari)
                    val btnImage = findViewById<Button>(R.id.btnPickImage)

                    /*2. =======================SCANNER AREA==================*/
                    /*a. FUNGSI JIKA SCAN SELESAI*/
                    fun SelesaiScan(result: String?) {
                        if (!result.isNullOrEmpty()) {
                            _KodeUjian.setText(result)
                            Toast.makeText(this, "Berhasil di scan", Toast.LENGTH_SHORT).show()
                        }
                    }

                    /*b. MENDKLARASIKAN LAUNCHER */
                    val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
                        if (result.contents != null) {
                            SelesaiScan(result.contents)
                        }
                    }
                    /* =======================SCANNER AREA==================*/

                    /*MENGATUR KETIKA BUTTON DI CLICK*/
                    _btnScan.setOnClickListener {
                        val option = ScanOptions()
                        option.setPrompt("Scan Barcode/QR")
                        option.setBeepEnabled(true)
                        option.setOrientationLocked(false)

                        barcodeLauncher.launch(option)
                    }

                    /* ===================JIKA MEMILIH DI GALERY==============*/
                    val PickImage = registerForActivityResult(
                        ActivityResultContracts.GetContent()
                    ) {uri ->
                        if(uri !=null) {
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
                                Toast.makeText(this, "Gagal melakukan scan, pastikan gambar berupa Barcode dan jelas!",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    btnImage.setOnClickListener {
                        PickImage.launch("image/*")
                    }
                    /* ==================JIKA MEMILIH DI GALERY==============*/

                    _btnStart.setOnClickListener {
                        val tokenUjian = _KodeUjian.text.toString().trim()

                        if (tokenUjian.isNotEmpty()) {
                            getExam(tokenUjian)
                        } else {
                            Toast.makeText(this@scan_page, "Masukan Kode Ujian Terlebih dahulu", Toast.LENGTH_SHORT).show()
                        }
                    }
                }


                    /* =======================HTTP REQ AREA==================*/
                    fun getExam(kode: String) {
                        val client = OkHttpClient.Builder()
                            .addInterceptor { chain ->
                                val request = chain.request()
                                Log.d("Request", "URL: ${request.url}, Headers: ${request.headers}")
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

                        Log.d("DEBUG_API", "Full URL: ${request.url}")
                        Log.d("DEBUG_API", "Headers: ${request.headers}")

                        /*Response Ketika Failure*/
                        client.newCall(request).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                e.printStackTrace()

                                runOnUiThread {
                                    Toast.makeText(
                                        this@scan_page,
                                        "Internal Server Error silakan coba lagi!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun onResponse(call: Call, response: Response
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
                                    }
                                    else {
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
                                            Toast.makeText(this@scan_page, "Ruang Ujian tidak ditemukan, pastikan token ujian benar!",
                                                Toast.LENGTH_SHORT).show()
                                        } else if (response.code == 401) {
                                            Toast.makeText(this@scan_page, "Token Ujian Tidak Valid",
                                                Toast.LENGTH_SHORT).show()
                                        }

                                    }
                                }
                            }
                        })
                        /* =======================HTTP REQ AREA==================*/

                    }
                }

