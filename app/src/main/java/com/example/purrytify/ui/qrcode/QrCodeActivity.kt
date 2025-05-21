package com.example.purrytify.ui.qrcode

import android.graphics.Bitmap
import android.os.Bundle
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
import androidx.compose.ui.unit.dp
import com.example.purrytify.R
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder

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
        
        Button(onClick = onClose) {
            Text("Close")
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