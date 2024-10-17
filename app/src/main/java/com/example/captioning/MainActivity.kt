package com.example.captioning

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.captioning.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var photoUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        val MC = MakeCaption(this@MainActivity)
        val TTS = TextToSpeech(this@MainActivity)
        val VQA = VisualQuestionAnswering(this@MainActivity)
        var bitmap = BitmapFactory.decodeResource(resources, R.drawable.default_image)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            @RequiresApi(Build.VERSION_CODES.P)
            fun(it: Uri?) {
                try {
                    binding.image.setImageURI(it)
                    binding.textview.text = getString(R.string.wait)
                    bitmap = it?.let { it1 ->
                        ImageDecoder.createSource(contentResolver,
                            it1
                        )
                    }?.let { it2 -> ImageDecoder.decodeBitmap(it2) }!!

                    lifecycleScope.launch {
                        val caption = withContext(Dispatchers.IO) {
                            MC.sendImageToVertexAI(bitmap)  // 백그라운드 스레드에서 실행
                        }
                        Log.d("Caption", "Received caption: $caption")

                        // Update UI on the main thread
                        binding.textview.text = caption
                        binding.linearLayout4.visibility = View.VISIBLE

                        withContext(Dispatchers.IO) {
                            TTS.textToSpeech(caption)  // 백그라운드 스레드에서 실행
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
        val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { intent ->
                    val imagePath = intent.getStringExtra("image_path")
                    // 이미지 경로를 이용해 이미지를 로드하고, 처리 로직을 이어감
                }
            }

//        val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
//            if (success) {
//                bitmap =
//                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, photoUri))
//                binding.image.setImageURI(photoUri)
//                binding.textview.text = getString(R.string.wait)
//
//                lifecycleScope.launch {
//                    val caption = withContext(Dispatchers.IO) {
//                        MC.sendImageToVertexAI(bitmap)  // 백그라운드 스레드에서 실행
//                    }
//
//                    // Update UI on the main thread
//                    binding.textview.text = caption
//                    binding.linearLayout4.visibility = View.VISIBLE
//                    withContext(Dispatchers.IO) {
//                        TTS.textToSpeech(caption)  // 백그라운드 스레드에서 실행
//                    }
//                }
//            } else {
//                Log.e("MainActivity", "Failed to capture image.")
//            }
        }

        binding.GalleryBtn.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.CameraBtn.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            cameraLauncher.launch(intent)
        }
//        binding.CameraBtn.setOnClickListener {
//            checkCameraPermission()
//            cameraLauncher.launch(photoUri)
//        }

        binding.QuestionBtn.setOnClickListener{
            val prompt = binding.edittext.text.toString()
            binding.textview.text = getString(R.string.wait2)

            lifecycleScope.launch {
                val answer = withContext(Dispatchers.IO) {
                    VQA.askWithPrompt(bitmap, prompt)
                }
                binding.textview.text = answer
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun checkCameraPermission() {
        val photoFile = File.createTempFile("photo_", ".jpg", cacheDir)

        // Use FileProvider to get the Uri
        photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            photoFile
        )

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
    }
}