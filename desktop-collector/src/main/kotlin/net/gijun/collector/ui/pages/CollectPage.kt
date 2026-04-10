package net.gijun.collector.ui.pages

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.gijun.collector.lcu.LcuStatus
import net.gijun.collector.service.CollectService
import net.gijun.collector.ui.theme.LolColors

data class LogLine(val type: String, val message: String)

private val typeColors = mapOf(
    "info" to Color(0xFF7EB8F7),
    "warn" to LolColors.Warning,
    "error" to LolColors.Error,
    "done" to LolColors.Win,
    "progress" to LolColors.TextSecondary,
)

@Composable
fun CollectPage(lcuStatus: LcuStatus, autoStatus: String) {
    val logs = remember { mutableStateListOf<LogLine>() }
    var collecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // 헤더
        Text("매치 수집", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LolColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("LCU에서 내전 데이터를 수집해 서버로 전송합니다", fontSize = 13.sp, color = LolColors.TextSecondary)
        Spacer(Modifier.height(24.dp))

        // 자동 수집 상태
        if (autoStatus.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LolColors.Info.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .border(1.dp, LolColors.Info.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("$autoStatus", fontSize = 13.sp, color = LolColors.Info)
            }
            Spacer(Modifier.height(16.dp))
        }

        // 수집 버튼
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, LolColors.Border),
        ) {
            Box(Modifier.padding(24.dp)) {
                Button(
                    onClick = {
                        logs.clear()
                        collecting = true
                        scope.launch {
                            CollectService.runCollect { type, message ->
                                logs.add(LogLine(type, message))
                                if (type == "done" || type == "error") collecting = false
                            }
                        }
                    },
                    enabled = !collecting && lcuStatus.connected,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LolColors.Primary,
                        contentColor = LolColors.TextInverse,
                        disabledContainerColor = LolColors.Primary.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        if (collecting) "수집 중..." else "수집 시작",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 로그
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = LolColors.BgCard),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, LolColors.Border),
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("수집 로그", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LolColors.Primary)
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF050D18), RoundedCornerShape(6.dp))
                        .border(1.dp, LolColors.Border, RoundedCornerShape(6.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                    ) {
                        if (logs.isEmpty()) {
                            Text(
                                if (lcuStatus.connected) "대기 중..." else "롤 클라이언트를 실행해주세요",
                                fontSize = 11.sp,
                                color = LolColors.TextSecondary,
                            )
                        } else {
                            logs.forEach { log ->
                                Text(
                                    log.message,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = typeColors[log.type] ?: Color(0xFF7EB8F7),
                                    fontWeight = if (log.type == "done") FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 1.dp),
                                )
                            }
                        }
                    }

                    // 자동 스크롤
                    LaunchedEffect(logs.size) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }
            }
        }
    }
}
