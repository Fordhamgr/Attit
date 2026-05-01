package com.example.attit.liquid

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

// --- MULTI-LENS SHADER (Supports up to 10 objects safely) ---
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
const val LIQUID_SHADER = """
    uniform float time;
    uniform float4 rects[10]; // Fixed array for stability
    uniform shader content;

    half4 main(float2 coord) {
        float2 totalDistortion = float2(0.0);
        float totalMask = 0.0;
        
        // Physics Constants
        float speed = 1.0;
        float strength = 20.0; 
        float frequency = 0.015;
        float edgeSoftness = 15.0;

        // Loop through all 10 potential lens slots
        for (int i = 0; i < 10; ++i) {
            float4 r = rects[i];
            
            // Optimization: Skip empty slots
            if (r.x == 0.0 && r.y == 0.0 && r.z == 0.0) continue;

            // Check if pixel is inside this lens
            if (coord.x > r.x && coord.x < r.z && coord.y > r.y && coord.y < r.w) {
                float wave = sin(coord.x * frequency + time * speed) + cos(coord.y * frequency + time * speed);
                float2 dist = float2(cos(wave * 2.5), sin(wave * 2.5)) * strength;
                
                // Anti-Leak Logic
                float2 edgeFade = smoothstep(r.xy, r.xy + edgeSoftness, coord) * (1.0 - smoothstep(r.zw - edgeSoftness, r.zw, coord));
                float mask = edgeFade.x * edgeFade.y;
                
                totalDistortion += dist * mask;
                totalMask += mask;
            }
        }

        if (totalMask > 0.0) {
            return content.eval(coord + totalDistortion);
        }
        return content.eval(coord);
    }
"""

@Stable
class LiquidState {
    var time by mutableFloatStateOf(0f)
    private val lenses = mutableStateMapOf<String, Rect>()
    private var cachedFlatRects = FloatArray(40)

    // BACKWARD COMPATIBILITY
    var lensRect: Rect
        get() = lenses["bottom_bar"] ?: Rect.Zero
        set(value) { lenses["bottom_bar"] = value }

    fun updateLens(id: String, rect: Rect) { lenses[id] = rect }
    fun removeLens(id: String) { lenses.remove(id) }
    fun getActiveRects(): List<Rect> = lenses.values.toList().take(10)

    /** Pre-flattened rects for shader (avoids allocating every frame). */
    fun getFlatRects(): FloatArray {
        val rects = getActiveRects()
        for (i in 0 until 10) {
            if (i < rects.size) {
                val r = rects[i]
                cachedFlatRects[i * 4] = r.left
                cachedFlatRects[i * 4 + 1] = r.top
                cachedFlatRects[i * 4 + 2] = r.right
                cachedFlatRects[i * 4 + 3] = r.bottom
            } else {
                cachedFlatRects[i * 4] = 0f
                cachedFlatRects[i * 4 + 1] = 0f
                cachedFlatRects[i * 4 + 2] = 0f
                cachedFlatRects[i * 4 + 3] = 0f
            }
        }
        return cachedFlatRects
    }

    suspend fun runAnimation() {
        while (true) {
            withInfiniteAnimationFrameMillis { frameTime -> time = (frameTime / 1000f) }
        }
    }
}

@Composable
fun rememberLiquidState(): LiquidState {
    val state = remember { LiquidState() }
    LaunchedEffect(state) { state.runAnimation() }
    return state
}

fun Modifier.liquidLens(state: LiquidState): Modifier = composed {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(LIQUID_SHADER) }

        this.graphicsLayer {
            shader.setFloatUniform("time", state.time)
            shader.setFloatUniform("rects", state.getFlatRects())
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
            clip = false
        }
    } else { this }
}

// Helper for Cards
fun Modifier.reportPosition(state: LiquidState, id: String): Modifier = composed {
    DisposableEffect(Unit) { onDispose { state.removeLens(id) } }
    this.onGloballyPositioned { coordinates ->
        if (coordinates.isAttached) state.updateLens(id, coordinates.boundsInRoot())
    }
}

fun Modifier.liquid(state: LiquidState) = this
fun Modifier.liquefiable(state: LiquidState) = this