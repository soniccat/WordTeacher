package com.aglushkov.wordteacher.androidApp.general.views.compose

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.SwipeableState
import androidx.compose.material.contentColorFor
import androidx.compose.material.swipeable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aglushkov.wordteacher.androidApp.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// The code is based on Drawer.kt

/**
 * Possible values of [SideSheetState].
 */
enum class SideSheetValue {
    /**
     * The state of the drawer when it is closed.
     */
    Closed,

    /**
     * The state of the drawer when it is open.
     */
    Open
}

/**
 * State of the [ModalSideSheet] composable.
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Suppress("NotCloseable")
@OptIn(ExperimentalMaterialApi::class)
@Stable
class SideSheetState(
    initialValue: SideSheetValue,
    val confirmStateChange: (SideSheetValue) -> Boolean = { true }
) {

    internal val swipeableState = SwipeableState(
        initialValue = initialValue,
        animationSpec = AnimationSpec,
        confirmStateChange = confirmStateChange
    )

    /**
     * Whether the drawer is open.
     */
    val isOpen: Boolean
        get() = currentValue == SideSheetValue.Open

    /**
     * Whether the drawer is closed.
     */
    val isClosed: Boolean
        get() = currentValue == SideSheetValue.Closed

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the start the drawer
     * currently in. If a swipe or an animation is in progress, this corresponds the state drawer
     * was in before the swipe or animation started.
     */
    val currentValue: SideSheetValue
        get() {
            return swipeableState.currentValue
        }

    /**
     * Whether the state is currently animating.
     */
    val isAnimationRunning: Boolean
        get() {
            return swipeableState.isAnimationRunning
        }

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the open animation ended
     */
    suspend fun open() = animateTo(SideSheetValue.Open, AnimationSpec)

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the close animation ended
     */
    suspend fun close() = animateTo(SideSheetValue.Closed, AnimationSpec)

    /**
     * Set the state of the drawer with specific animation
     *
     * @param targetValue The new value to animate to.
     * @param anim The animation that will be used to animate to the new value.
     */
    @ExperimentalMaterialApi
    suspend fun animateTo(targetValue: SideSheetValue, anim: AnimationSpec<Float>) {
        swipeableState.animateTo(targetValue, anim)
    }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    @ExperimentalMaterialApi
    suspend fun snapTo(targetValue: SideSheetValue) {
        swipeableState.snapTo(targetValue)
    }

    /**
     * The target value of the drawer state.
     *
     * If a swipe is in progress, this is the value that the Drawer would animate to if the
     * swipe finishes. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
//    @ExperimentalMaterialApi
//    @get:ExperimentalMaterialApi
    val targetValue: SideSheetValue
        get() = swipeableState.targetValue

    /**
     * The current position (in pixels) of the drawer sheet.
     */
    @Suppress("EXPERIMENTAL_ANNOTATION_ON_WRONG_TARGET")
//    @ExperimentalMaterialApi
//    @get:ExperimentalMaterialApi
    val offset: State<Float>
        get() = swipeableState.offset

    companion object {
        /**
         * The default [Saver] implementation for [SideSheetState].
         */
        fun Saver(confirmStateChange: (SideSheetValue) -> Boolean) =
            Saver<SideSheetState, SideSheetValue>(
                save = { it.currentValue },
                restore = { SideSheetState(it, confirmStateChange) }
            )
    }
}

/**
 * Create and [remember] a [SideSheetState].
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
fun rememberSideSheetState(
    initialValue: SideSheetValue,
    confirmStateChange: (SideSheetValue) -> Boolean = { true }
): SideSheetState {
    return rememberSaveable(saver = SideSheetState.Saver(confirmStateChange)) {
        SideSheetState(initialValue, confirmStateChange)
    }
}

/**
 * <a href="https://material.io/components/navigation-drawer#modal-drawer" class="external" target="_blank">Material Design modal navigation drawer</a>.
 *
 * Modal navigation drawers block interaction with the rest of an app’s content with a scrim.
 * They are elevated above most of the app’s UI and don’t affect the screen’s layout grid.
 *
 * ![Modal drawer image](https://developer.android.com/images/reference/androidx/compose/material/modal-drawer.png)
 *
 * See [BottomDrawer] for a layout that introduces a bottom drawer, suitable when
 * using bottom navigation.
 *
 * @sample androidx.compose.material.samples.ModalDrawerSample
 *
 * @param sideSheetContent composable that represents content inside the drawer
 * @param modifier optional modifier for the drawer
 * @param sideSheetState state of the drawer
 * @param gesturesEnabled whether or not drawer can be interacted by gestures
 * @param sideSheetShape shape of the drawer sheet
 * @param sideSheetElevation drawer sheet elevation. This controls the size of the shadow below the
 * drawer sheet
 * @param sideSheetBackgroundColor background color to be used for the drawer sheet
 * @param sideSheetContentColor color of the content to use inside the drawer sheet. Defaults to
 * either the matching content color for [sideSheetBackgroundColor], or, if it is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param scrimColor color of the scrim that obscures content when the drawer is open
 * @param content content of the rest of the UI
 *
 * @throws IllegalStateException when parent has [Float.POSITIVE_INFINITY] width
 */
@Composable
@OptIn(ExperimentalMaterialApi::class)
fun ModalSideSheet(
    sideSheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sideSheetState: SideSheetState = rememberSideSheetState(SideSheetValue.Closed),
    gesturesEnabled: Boolean = true,
    sideSheetShape: Shape = MaterialTheme.shapes.large,
    sideSheetElevation: Dp = SideSheetDefaults.Elevation,
    sideSheetBackgroundColor: Color = MaterialTheme.colors.surface,
    sideSheetContentColor: Color = contentColorFor(sideSheetBackgroundColor),
    scrimColor: Color = SideSheetDefaults.scrimColor,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val modalSideSheetConstraints = constraints
        // TODO : think about Infinite max bounds case
        if (!modalSideSheetConstraints.hasBoundedWidth) {
            throw IllegalStateException("Drawer shouldn't have infinite width")
        }

        val minValue = 0.0f
        val maxValue = modalSideSheetConstraints.maxWidth.toFloat()

        val anchors = mapOf(maxValue to SideSheetValue.Closed, minValue to SideSheetValue.Open)
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        Box(
            Modifier.swipeable(
                state = sideSheetState.swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                enabled = gesturesEnabled,
                reverseDirection = isRtl,
                velocityThreshold = SideSheetVelocityThreshold,
                resistance = null
            )
        ) {
            Box {
                content()
            }
            Scrim(
                open = sideSheetState.isOpen,
                onClose = {
                    if (
                        gesturesEnabled &&
                        sideSheetState.confirmStateChange(SideSheetValue.Closed)
                    ) {
                        scope.launch { sideSheetState.close() }
                    }
                },
                fraction = {
                    calculateFraction(maxValue, minValue, sideSheetState.offset.value)
                },
                color = scrimColor
            )
            val navigationMenu = getString(Strings.NavigationMenu)
            Surface(
                modifier = with(LocalDensity.current) {
                    Modifier
                        .sizeIn(
                            minWidth = modalSideSheetConstraints.minWidth.toDp(),
                            minHeight = modalSideSheetConstraints.minHeight.toDp(),
                            maxWidth = modalSideSheetConstraints.maxWidth.toDp(),
                            maxHeight = modalSideSheetConstraints.maxHeight.toDp()
                        )
                }
                    .offset { IntOffset(sideSheetState.offset.value.roundToInt(), 0) }
                    .padding(start = EndSideSheetPadding)
                    .semantics {
                        paneTitle = navigationMenu
                        if (sideSheetState.isOpen) {
                            dismiss {
                                if (sideSheetState.confirmStateChange(SideSheetValue.Closed)) {
                                    scope.launch { sideSheetState.close() }
                                }; true
                            }
                        }
                    },
                shape = sideSheetShape,
                color = sideSheetBackgroundColor,
                contentColor = sideSheetContentColor,
                elevation = sideSheetElevation
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    content = sideSheetContent
                )
            }
        }
    }
}

/**
 * Object to hold default values for [ModalSideSheet] and [BottomDrawer]
 */
object SideSheetDefaults {

    /**
     * Default Elevation for drawer sheet as specified in material specs
     */
    val Elevation = 16.dp

    val scrimColor: Color
        @Composable
        get() = MaterialTheme.colors.onSurface.copy(alpha = ScrimOpacity)

    /**
     * Default alpha for scrim color
     */
    const val ScrimOpacity = 0.32f
}

private fun calculateFraction(a: Float, b: Float, pos: Float) =
    ((pos - a) / (b - a)).coerceIn(0f, 1f)

@Composable
private fun Scrim(
    open: Boolean,
    onClose: () -> Unit,
    fraction: () -> Float,
    color: Color
) {
    val closeSideSheet = getString(Strings.CloseSideSheet)
    val dismissSideSheet = if (open) {
        Modifier
            .pointerInput(onClose) { detectTapGestures { onClose() } }
            .semantics(mergeDescendants = true) {
                contentDescription = closeSideSheet
                onClick { onClose(); true }
            }
    } else {
        Modifier
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .then(dismissSideSheet)
    ) {
        drawRect(color, alpha = fraction())
    }
}

private val EndSideSheetPadding = 56.dp
private val SideSheetVelocityThreshold = 400.dp

// TODO: b/177571613 this should be a proper decay settling
// this is taken from the DrawerLayout's DragViewHelper as a min duration.
private val AnimationSpec = TweenSpec<Float>(durationMillis = 256)

@Suppress("INLINE_CLASS_DEPRECATED")
@Immutable
private class Strings private constructor(@Suppress("unused") private val value: Int) {
    companion object {
        val NavigationMenu = Strings(0)
        val CloseSideSheet = Strings(1)
        val CloseSheet = Strings(2)
        val DefaultErrorMessage = Strings(3)
        val ExposedDropdownMenu = Strings(4)
    }
}

@Composable
private fun getString(string: Strings): String {
    LocalConfiguration.current
    val resources = LocalContext.current.resources
    return when (string) {
        Strings.NavigationMenu -> resources.getString(R.string.navigation_menu)
        Strings.CloseSideSheet -> resources.getString(R.string.close_drawer)
        Strings.CloseSheet -> resources.getString(R.string.close_sheet)
        Strings.DefaultErrorMessage -> resources.getString(R.string.default_error_message)
        Strings.ExposedDropdownMenu -> resources.getString(R.string.dropdown_menu)
        else -> ""
    }
}
