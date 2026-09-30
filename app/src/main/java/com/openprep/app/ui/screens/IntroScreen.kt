package com.openprep.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroScreen(onFinishIntro: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    
    val pages = listOf(
        Triple("Absolute Privacy", "Zero ads, zero tracking. OpenPrep is 100% Free and Open Source.", Icons.Default.PrivacyTip),
        Triple("Bring Your Own Server", "Host your own course manifests, videos, and quizzes seamlessly.", Icons.Default.LibraryBooks),
        Triple("Offline Mode", "Download PDFs and videos natively to study anywhere, anytime.", Icons.Default.CloudDownload)
    )

    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                // Page Indicators
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(8.dp))
                    }
                }
                
                Button(
                    onClick = onFinishIntro,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(if (pagerState.currentPage == pages.size - 1) "Get Started" else "Skip")
                }
            }
        }
    ) { padding ->
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().padding(padding)) { page ->
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = pages[page].third,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(48.dp))
                Text(text = pages[page].first, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = pages[page].second, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}
