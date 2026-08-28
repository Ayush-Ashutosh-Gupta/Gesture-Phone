package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.GestureType
import com.example.model.HandType
import com.example.model.TrackedHand

data class GestureGuideItem(
    val symbolEmoji: String,
    val icon: ImageVector,
    val title: String,
    val triggerCondition: String,
    val actionDescription: String,
    val gestureType: GestureType,
    val badgeTag: String
)

@Composable
fun GestureGuideDialog(
    activeHands: List<TrackedHand>,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Right Hand (Pointer/Nav), 1: Left Hand (System/Audio/Light)

    val leftHandGestures = listOf(
        GestureGuideItem(
            symbolEmoji = "🤘",
            icon = Icons.Default.FlashlightOn,
            title = "Flashlight / Light",
            triggerCondition = "Hold 'Rock On' (Index + Pinky) for 0.4s",
            actionDescription = "Toggles the phone flashlight / torch instantly in the air.",
            gestureType = GestureType.ROCK_ON,
            badgeTag = "LIGHT CONTROL"
        ),
        GestureGuideItem(
            symbolEmoji = "👍",
            icon = Icons.Default.VolumeUp,
            title = "Volume Up (+8%)",
            triggerCondition = "Thumbs Up gesture with Left Hand",
            actionDescription = "Increases media & system audio volume smoothly.",
            gestureType = GestureType.THUMBS_UP,
            badgeTag = "AUDIO UP"
        ),
        GestureGuideItem(
            symbolEmoji = "👎",
            icon = Icons.Default.VolumeDown,
            title = "Volume Down (-8%)",
            triggerCondition = "Thumbs Down gesture with Left Hand",
            actionDescription = "Decreases audio volume step by step.",
            gestureType = GestureType.THUMBS_DOWN,
            badgeTag = "AUDIO DOWN"
        ),
        GestureGuideItem(
            symbolEmoji = "✋",
            icon = Icons.Default.VolumeUp,
            title = "Vertical Volume Slider",
            triggerCondition = "Move open palm vertically up / down",
            actionDescription = "Continuous real-time volume slider (Top = 100%, Bottom = 0%).",
            gestureType = GestureType.PALM_OPEN,
            badgeTag = "SMOOTH SLIDER"
        ),
        GestureGuideItem(
            symbolEmoji = "✊",
            icon = Icons.Default.PlayArrow,
            title = "Play / Pause Media",
            triggerCondition = "Hold clenched fist for 0.45s",
            actionDescription = "Dispatches global media key event to pause/resume music or video.",
            gestureType = GestureType.FIST_HOLD,
            badgeTag = "MEDIA PLAY/PAUSE"
        ),
        GestureGuideItem(
            symbolEmoji = "✌️",
            icon = Icons.Default.VolumeMute,
            title = "Mute / Unmute",
            triggerCondition = "Peace / Victory sign (2 fingers)",
            actionDescription = "Instantly silences or restores audio stream.",
            gestureType = GestureType.PEACE_SIGN,
            badgeTag = "AUDIO MUTE"
        ),
        GestureGuideItem(
            symbolEmoji = "👈",
            icon = Icons.Default.SkipNext,
            title = "Next Track",
            triggerCondition = "Swipe Left with Left Hand",
            actionDescription = "Skips to next song / video in active media player.",
            gestureType = GestureType.SWIPE_LEFT,
            badgeTag = "NEXT TRACK"
        ),
        GestureGuideItem(
            symbolEmoji = "👉",
            icon = Icons.Default.SkipPrevious,
            title = "Previous Track",
            triggerCondition = "Swipe Right with Left Hand",
            actionDescription = "Restarts current song or goes back to previous track.",
            gestureType = GestureType.SWIPE_RIGHT,
            badgeTag = "PREV TRACK"
        )
    )

    val rightHandGestures = listOf(
        GestureGuideItem(
            symbolEmoji = "☝️",
            icon = Icons.Default.Mouse,
            title = "Virtual Cursor / Pointer",
            triggerCondition = "Point with Index Finger or Palm",
            actionDescription = "Smoothly positions the high-tech floating pointer anywhere on screen.",
            gestureType = GestureType.CURSOR_POINT,
            badgeTag = "POINTER"
        ),
        GestureGuideItem(
            symbolEmoji = "🤏",
            icon = Icons.Default.TouchApp,
            title = "Pinch Click / Tap",
            triggerCondition = "Pinch Thumb & Index finger together",
            actionDescription = "Dispatches a physical touch click at the exact cursor coordinates.",
            gestureType = GestureType.PINCH_CLICK,
            badgeTag = "CLICK / TAP"
        ),
        GestureGuideItem(
            symbolEmoji = "🤏⏳",
            icon = Icons.Default.TouchApp,
            title = "Hold & Drag / Long Press",
            triggerCondition = "Pinch and hold for > 0.35s",
            actionDescription = "Performs touch down hold for dragging apps, selecting text, or context menus.",
            gestureType = GestureType.PINCH_CLICK,
            badgeTag = "HOLD / DRAG"
        ),
        GestureGuideItem(
            symbolEmoji = "🤏🤏",
            icon = Icons.Default.TouchApp,
            title = "Double Click",
            triggerCondition = "Pinch twice rapidly (< 0.3s)",
            actionDescription = "Executes double-tap to zoom or open files.",
            gestureType = GestureType.PINCH_CLICK,
            badgeTag = "DOUBLE CLICK"
        ),
        GestureGuideItem(
            symbolEmoji = "👈",
            icon = Icons.Default.ArrowBack,
            title = "System Back",
            triggerCondition = "Swipe hand Left in the air",
            actionDescription = "Triggers global Back action across any application.",
            gestureType = GestureType.SWIPE_LEFT,
            badgeTag = "BACK BUTTON"
        ),
        GestureGuideItem(
            symbolEmoji = "👉",
            icon = Icons.Default.ViewCarousel,
            title = "Recent Apps",
            triggerCondition = "Swipe hand Right in the air",
            actionDescription = "Opens Android task switcher / overview of recent applications.",
            gestureType = GestureType.SWIPE_RIGHT,
            badgeTag = "RECENTS"
        ),
        GestureGuideItem(
            symbolEmoji = "👆",
            icon = Icons.Default.Home,
            title = "Home Screen",
            triggerCondition = "Swipe hand Up in the air",
            actionDescription = "Returns to device home launcher instantly.",
            gestureType = GestureType.SWIPE_UP,
            badgeTag = "HOME"
        ),
        GestureGuideItem(
            symbolEmoji = "👇",
            icon = Icons.Default.Notifications,
            title = "Notification Shade",
            triggerCondition = "Swipe hand Down in the air",
            actionDescription = "Pulls down status bar notifications and quick settings tile.",
            gestureType = GestureType.SWIPE_DOWN,
            badgeTag = "NOTIFICATIONS"
        )
    )

    val activeLeftGesture = activeHands.find { it.handType == HandType.LEFT }?.activeGesture
    val activeRightGesture = activeHands.find { it.handType == HandType.RIGHT }?.activeGesture

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF080E18),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A5F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gesture Symbol Dictionary",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Dual-Hand Precision Air Gestures",
                            fontSize = 12.sp,
                            color = Color(0xFF00E5FF)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hand Tabs: Right Hand (Pointer & Nav) vs Left Hand (System & Light)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0D1B2A),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = if (selectedTab == 0) Color(0xFF00E5FF) else Color(0xFFFF3366)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF))
                                )
                                Text(
                                    "RIGHT HAND (Pointer / Nav)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (selectedTab == 0) Color(0xFF00E5FF) else Color.LightGray
                                )
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3366))
                                )
                                Text(
                                    "LEFT HAND (System / Light)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (selectedTab == 1) Color(0xFFFF3366) else Color.LightGray
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Gesture List
                val items = if (selectedTab == 0) rightHandGestures else leftHandGestures
                val currentActiveGesture = if (selectedTab == 0) activeRightGesture else activeLeftGesture
                val themeColor = if (selectedTab == 0) Color(0xFF00E5FF) else Color(0xFFFF3366)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        val isCurrentlyActive = currentActiveGesture == item.gestureType
                        GestureCard(
                            item = item,
                            isActive = isCurrentlyActive,
                            themeColor = themeColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GestureCard(
    item: GestureGuideItem,
    isActive: Boolean,
    themeColor: Color
) {
    val borderColor by animateColorAsState(
        targetValue = if (isActive) themeColor else Color(0x333A506B),
        label = "border_anim"
    )
    val cardBg = if (isActive) themeColor.copy(alpha = 0.15f) else Color(0xFF0D1B2A)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(if (isActive) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Emoji & Icon Symbol Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF050B14))
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.symbolEmoji,
                    fontSize = 22.sp
                )
            }

            // Description Body
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) themeColor else Color(0x2200E5FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isActive) "ACTIVE NOW" else item.badgeTag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.Black else themeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Trigger: ${item.triggerCondition}",
                    fontSize = 11.sp,
                    color = Color(0xFFA0B4C8),
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = item.actionDescription,
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}
