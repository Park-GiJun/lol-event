package net.gijun.collector.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.gijun.collector.ui.theme.LolColors
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

private val imageCache = ConcurrentHashMap<String, ImageBitmap?>()

private const val CDN = "https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/champion-icons"

fun champIconUrl(championId: Int): String = "$CDN/$championId.png"

@Composable
fun ChampionIcon(
    championId: Int,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier,
) {
    if (championId <= 0) {
        Box(
            modifier
                .size(size)
                .clip(RoundedCornerShape(4.dp))
                .background(LolColors.BgHover)
        )
        return
    }

    val url = champIconUrl(championId)
    var bitmap by remember(url) { mutableStateOf(imageCache[url]) }
    var loaded by remember(url) { mutableStateOf(imageCache.containsKey(url)) }

    LaunchedEffect(url) {
        if (!loaded) {
            val result = withContext(Dispatchers.IO) {
                try {
                    val bytes = URI(url).toURL().readBytes()
                    org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
            imageCache[url] = result
            bitmap = result
            loaded = true
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(4.dp))
        )
    } else {
        Box(
            modifier
                .size(size)
                .clip(RoundedCornerShape(4.dp))
                .background(LolColors.BgHover)
        )
    }
}
