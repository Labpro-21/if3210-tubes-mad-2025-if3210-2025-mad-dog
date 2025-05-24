package com.example.purrytify.ui.qrcode

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.purrytify.R
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import java.io.FileOutputStream
import android.content.Context

class QrCodeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set the theme programmatically if needed
        setTheme(R.style.Theme_Purrytify_Dialog)
        
        val deepLink = intent.getStringExtra("DEEP_LINK") ?: ""
        
        setContent {
            PurrytifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QRCodeContent(deepLink) { finish() }
                }
            }
        }
    }
}

@Composable
fun QRCodeContent(deepLink: String, onClose: () -> Unit) {
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    
    LaunchedEffect(deepLink) {
        qrCodeBitmap = generateQRCode(deepLink)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        qrCodeBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.size(300.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onClose) {
                Text("Close")
            }
            
            Button(onClick = {
                qrCodeBitmap?.let { bitmap ->
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, deepLink)
                        putExtra(
                            Intent.EXTRA_STREAM,
                            bitmap.saveToCache(context, "qr_code.png")
                        )
                        type = "image/png"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                }
            }) {
                Text("Share QR")
            }
        }
    }
}

private fun generateQRCode(content: String): Bitmap? {
    return try {
        val multiFormatWriter = MultiFormatWriter()
        val bitMatrix: BitMatrix = multiFormatWriter.encode(
            content,
            BarcodeFormat.QR_CODE,
            500,
            500
        )
        val barcodeEncoder = BarcodeEncoder()
        barcodeEncoder.createBitmap(bitMatrix)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun Bitmap.saveToCache(context: Context, fileName: String): Uri? {
    return try {
        val cacheDir = context.cacheDir
        val file = File(cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        try {
            this.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        } finally {
            outputStream.close()
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}