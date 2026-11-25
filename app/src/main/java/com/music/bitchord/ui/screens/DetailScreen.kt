package com.music.bitchord.ui.screens

import com.music.bitchord.ui.components.SourceQuality
import com.music.bitchord.ui.components.SourceQualityBadge
import com.music.bitchord.ui.components.sourceQualityBadge
import androidx.compose.runtime.produceState
import com.music.bitchord.ui.components.trackArtSize
import android.os.Build
import com.music.bitchord.R

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import com.music.bitchord.ui.components.liquidGlassRim
import com.music.bitchord.ui.components.continuous
import com.music.bitchord.ui.components.FadingText
import com.music.bitchord.ui.components.longPressMenuClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import com.music.bitchord.data.model.LibraryState
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawBehind
import com.music.bitchord.data.sources.ServerLibrary
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.transformations
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import com.music.bitchord.data.LocalGroups
import com.music.bitchord.data.OfflinePlaylists
import com.music.bitchord.data.matchesSearch
import com.music.bitchord.data.sortedForLibrary
import com.music.bitchord.data.useTrackTags
import com.music.bitchord.ui.components.CoverPlaceholder
import com.music.bitchord.data.canvas.AppleArtistArt
import com.music.bitchord.data.DebugLog as Log
import com.music.bitchord.data.canvas.AppleArtistArtRepository
import com.music.bitchord.data.canvas.appleCatalogue
import com.music.bitchord.data.canvas.ArtistSignature
import com.music.bitchord.data.canvas.ArtistSignatureRepository
import com.music.bitchord.data.canvas.SignatureInkTransformation
import com.music.bitchord.data.canvas.keyColors
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import com.music.bitchord.data.canvas.CanvasArtwork
import com.music.bitchord.data.canvas.CanvasSource
import com.music.bitchord.data.canvas.CanvasRepository
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.DetailPage
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.HEADER_ART_PX
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.canvas.AppleMusicCanvas
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.data.settings.CollectionBackground
import com.music.bitchord.ui.player.FullArtworkBlurBackdrop
import com.music.bitchord.ui.player.rememberFullArtworkBlurImage
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.settings.SongSort
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.SubscriptionState
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.model.durationMillis
import com.music.bitchord.data.model.playingRowMatcher
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.DownloadedBadge
import com.music.bitchord.ui.components.ExplicitSongTitle
import com.music.bitchord.ui.components.LIBRARY_GRID_SPACING
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.BAR_GUTTER
import com.music.bitchord.ui.components.RowMoreButton
import com.music.bitchord.ui.components.ROW_DIVIDER_INSET
import com.music.bitchord.ui.components.NUMBERED_ROW_DIVIDER_INSET
import com.music.bitchord.ui.components.SHELF_CARD_WIDTH
import com.music.bitchord.ui.components.SongRow
import com.music.bitchord.ui.components.SearchPlayingBars
import com.music.bitchord.ui.components.libraryGrid
import com.music.bitchord.ui.components.lightweightLiquidGlass
import com.music.bitchord.ui.components.detailSkeleton
import com.music.bitchord.ui.components.ShelfSkeleton
import com.music.bitchord.ui.components.songListSkeleton
import com.music.bitchord.ui.components.topBarContentPadding
import com.music.bitchord.ui.components.trackColumnWidth
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.icons.BitChordIcons
import com.music.bitchord.ui.graphics.FADE_CURVE_STOPS
import com.music.bitchord.ui.graphics.smootherStep
import com.music.bitchord.ui.player.CanvasArtworkPlayer
import com.music.bitchord.ui.theme.ArtworkPalette
import com.music.bitchord.ui.theme.rememberArtworkPalette
import com.music.bitchord.ui.theme.ArtworkKeyColors
import kotlin.math.roundToInt
import java.util.Locale

private const val MAX_ARTIST_SONGS = 20
private const val SONGS_PER_COLUMN = 4
private const val ARTIST_ROW_MAX_ITEMS = 5

/** The artist photo, very slightly taller than it is wide. */
private const val ARTIST_PHOTO_RATIO = 0.95f

/** The framed sleeve on a release page, as a fraction of the page width. */
private const val SLEEVE_FRACTION = 0.76f

/** ...and of its height, so a landscape window still shows the title under it. */
private const val SLEEVE_MAX_HEIGHT = 0.5f

private val SLEEVE_SHAPE = RoundedCornerShape(12.dp)
private val SLEEVE_SHADOW = 18.dp

/**
 * The tallest a full-bleed sleeve may be, as a fraction of the page's height.
 * A square the page's width that would stand taller than this — a landscape
 * window, a wide tablet — keeps the framed sleeve, so the title is never
 * pushed off the bottom of the screen.
 */
private const val FULL_BLEED_MAX_HEIGHT = 0.62f

/** Apple's tall motion cover is 3:4 (width / height). */
private const val TALL_COVER_ASPECT = 3f / 4f

/** How much of a full-bleed sleeve's foot dissolves into the page — the player's banner's share. */
private const val FULL_BLEED_FADE_FRACTION = 0.42f

/** How far the title rides up into that dissolve, where the sleeve has all but gone. */
private val FULL_BLEED_TITLE_OVERLAP = 44.dp

/** Shuffle and Add to Library either side of the release page's Play pill. */
private val RELEASE_SIDE_BUTTON = 48.dp
private val RELEASE_ACTION_GAP = 16.dp

/** Play's fixed width, so the three controls sit as a compact centred group (Glaze / Apple Music). */
private val RELEASE_PLAY_WIDTH = 160.dp

/** A 3:4 header reports this much of its width as height, so the title rides up into its fade. */
private const val TALL_HEADER_LAYOUT_RATIO = 1.02f

/** Space between an album's footer text and the hairlines above and below it. */
private val ALBUM_FOOTER_GAP = 20.dp

/** Past this the Play pill stops growing, so a tablet doesn't get a bar. */
private val RELEASE_ACTIONS_MAX_WIDTH = 420.dp
private val PILL_SHAPE = RoundedCornerShape(12.dp)

/**
 * Where the search field sits once it is open — directly under the header,
 * which is item zero. The one place that has to know, so that opening the
 * search can carry the page up to it.
 */
private const val SEARCH_ITEM_INDEX = 1

/** The inset the header text and the action pills share. */
private val HEADER_GUTTER = PAGE_GUTTER + 8.dp

/** Extra breathing room for the editorial copy on album and artist pages. */
private val ABOUT_GUTTER = PAGE_GUTTER + 6.dp

/** The artist bio and everything below it: the page edge every page shares. */
private val ARTIST_CONTENT_GUTTER = PAGE_GUTTER

/**
 * How far past the foot of the artist photo the name is allowed to hang.
 *
 * Sat flush to the bottom of the picture it lands wherever the picture happens
 * to be busy, and on a photo with anything going on down there the name reads
 * as part of it rather than as a caption to it. Dropped clear, it sits on the
 * blurred colour instead, which has nothing in it to compete.
 */
private val HEADER_DROP = 44.dp

/** How far the artist name/logo and everything under it sit above the usual [HEADER_DROP]. */
private val ARTIST_HEADER_LIFT = 64.dp

/** The artist action row: a large Play flanked by two smaller glass circles. */
private val ARTIST_PLAY_BUTTON = 70.dp

/** A larger share of a smaller circle than the release headers' 0.44. */
private const val ARTIST_PLAY_ICON_SCALE = 0.56f
private val ARTIST_SIDE_BUTTON = 52.dp
private val ARTIST_ACTION_GAP = 24.dp

/** The title logo's widest share of the page, and tallest share of the photo. */
private const val ARTIST_LOGO_WIDTH = 0.82f

/** The signature closing the About section. */
private val ABOUT_LOGO_HEIGHT = 56.dp

/** A signature is drawn at this share of a title logo's size. */
private const val SIGNATURE_SCALE = 0.7f
private const val ARTIST_LOGO_MAX_HEIGHT = 0.30f

private const val LOGO_RETRIES = 2
private const val LOGO_RETRY_DELAY_MS = 800L

/** Vector signatures decode at this width so small files stay crisp laid out large. */
private const val SVG_DECODE_WIDTH = 1600

private val CREDIT_SEPARATOR = Regex(""",\s|\s&\s|\s(?:x|and|feat\.?|ft\.?|with)\s""", RegexOption.IGNORE_CASE)

/** Whether this reads as several artists credited together rather than one name. */
private fun String.looksLikeCredit() = CREDIT_SEPARATOR.containsMatchIn(this)

/**
 * Album / artist / playlist page. Rendered inside the main content area
 * rather than as a sheet, so the tab bar and mini player stay visible.
 *
 * The page paints itself in the artwork's own colours — one solid tint behind
 * everything, secondary text in a lighter shade of the same hue, and an accent
 * taken off the sleeve for whatever is playing. See [rememberArtworkPalette]
 * for how those are derived and kept legible.
 *
 * A release is one layer: the list, sleeve and all, on a flat colour. An
 * artist page adds [PageBackground] under it — the photograph, dissolving into
 * the tint — since that picture runs edge to edge up under the bar. Nothing in
 * either is re-blurred while the page scrolls.
 */
@Composable
fun DetailScreen(
    page: DetailPage,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (List<Song>, Int) -> Unit,
    onSongLongPress: (Song) -> Unit,
    onSongSwipe: (Song) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    /** The Play button: the songs from the top, in order. */
    onPlay: (List<Song>) -> Unit,
    onSectionItemClick: (ShelfItem) -> Unit,
    onArtistClick: (String, String) -> Unit,
    onAddSuggested: (Song) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    activeShelf: HomeShelf? = null,
    onActiveShelfChange: (HomeShelf?) -> Unit = {},
    /**
     * Holding one of the album cards on an artist page — the same menu the
     * shelves on every other tab open, so a release can be queued from
     * wherever it is seen rather than only from its own page.
     */
    onSectionItemLongPress: ((ShelfItem) -> Unit)? = null,
    /**
     * Whether the filter box is open under the header. Owned by the caller
     * because the control that opens it sits in the top bar, beside the sort
     * and overflow buttons, not on this page.
     */
    searching: Boolean = false,
    /** The find-in-list text; its field floats in the top bar (MainActivity). */
    query: String = "",
    onSearchingChange: (Boolean) -> Unit = {},
    /**
     * Saves this release to the account's library, or takes it out —
     * [DetailPage.library] says which way round. Null hides the control
     * entirely, which is the answer for a guest and for the pages YouTube never
     * offers to save; there is nothing to show a signed-out user here that
     * wouldn't just be refused.
     */
    onToggleLibrary: (() -> Unit)? = null,
    /** Downloads this album into the music server through its companion; null without one. */
    onDownloadToServer: (() -> Unit)? = null,
    serverDownload: com.music.bitchord.data.sources.GlazeCompanion.Job? = null,
    /**
     * Subscribes to this artist's channel, or unsubscribes —
     * [DetailPage.subscription] says which way round. The artist page's answer
     * to [onToggleLibrary], and null in the same circumstances: a guest, or a
     * page whose header never offered the button.
     */
    onToggleSubscription: (() -> Unit)? = null,
    /** Library state of releases shown as cards on this page, by browse id. */
    releaseLibrary: Map<String, LibraryState> = emptyMap(),
    onLoadReleaseLibrary: ((String) -> Unit)? = null,
    /** Null where saving isn't offered — a guest. */
    onToggleReleaseLibrary: ((String) -> Unit)? = null,
    /**
     * How the track list is ordered — the release's own running order by
     * default, or alphabetical. Owned by the caller rather than this page
     * because the control for it lives in the top bar, alongside the account
     * photo, not in this header.
     */
    songSort: SongSort = SongSort.DEFAULT,
    /**
     * Set when this page is a device folder — Downloads, Local Music and the
     * like. The header is the same as any playlist's; under it, instead of one
     * running order, come the folder's Songs / Artists / Albums tabs. See
     * [LocalFolder].
     */
    folder: LocalFolder? = null,
    /**
     * Set while this playlist's songs are being rearranged: drawn in place of
     * the track list, over the same page colour, and handed the page's
     * [ArtworkPalette] so it reads as this page rather than a sheet over it.
     */
    reorder: (@Composable (ArtworkPalette) -> Unit)? = null,
) {
    val rawSongs = (page.songs as? UiState.Success)?.data.orEmpty()
    // A folder keeps its own sort — the one its top bar menu sets — rather
    // than the per-page one a release takes.
    val folderSort = folder?.sort
    val songs = remember(rawSongs, songSort, folderSort) {
        if (folderSort != null) rawSongs.sortedForLibrary(folderSort) else rawSongs.sortedForDetail(songSort)
    }
    // What a numbered row shows regardless of [songSort] — an album's track
    // numbers are the sleeve's own and must not relabel themselves to match
    // wherever a sort put the row. An album grouped off a device's files
    // numbers from its tags where they say, since a release with a track
    // missing must not renumber the rest.
    val originalTrackNumbers = remember(rawSongs) {
        val byTag = rawSongs.useTrackTags()
        rawSongs.withIndex().associate { (i, s) -> s.videoId to ((if (byTag) s.trackNumber else null) ?: (i + 1)) }
    }
    val isArtist = page.type == BrowseType.ARTIST
    // Whether the top release card's album is already saved isn't on the shelf
    // item; it is read off the album once, as soon as the card is known.
    // The page's own pick (newest year) shows at once; the newest release to the day, joint ones
    // included, replaces it when it's found (see [NewestRelease]), and the page's pick stays if not.
    val topReleaseState = produceState(if (isArtist) page.sections.topRelease() else null, isArtist, page.sections) {
        if (!isArtist) return@produceState
        value = page.sections.topRelease()
        if (page.sections.isEmpty()) return@produceState
        // Deezer files joint releases (Offset & Quavo) under their own artist, so its newest can be
        // older than the page's; it only ever replaces the page's pick with one from the same year or later.
        val pageYear = page.sections.topRelease()?.releaseYear() ?: 0
        com.music.bitchord.data.canvas.NewestRelease.find(page.title, page.sections.flatMap { it.items })
            ?.takeIf { (it.releaseYear() ?: 0) >= pageYear }?.let { value = it }
    }
    // An album the companion knows is coming from this artist takes the card's place.
    val upcoming by com.music.bitchord.data.sources.GlazeCompanion.upcoming.collectAsStateWithLifecycle()
    // Asked of the companion for this artist by name: works for anyone, followed or not.
    val announced by produceState(emptyList<com.music.bitchord.data.sources.GlazeCompanion.Release>(), page.title, isArtist) {
        val server = com.music.bitchord.data.sources.GlazeCompanion.server()
        if (isArtist && server != null) {
            value = runCatching { com.music.bitchord.data.sources.GlazeCompanion.releasesByName(server, page.title) }.getOrDefault(emptyList())
        }
    }
    val nextRelease = remember(upcoming, announced, page.title, isArtist) {
        if (!isArtist) return@remember null
        val name = page.title.trim().lowercase()
        val today = java.time.LocalDate.now().toString()
        (upcoming + announced).filter { it.releaseDate.take(10) >= today }.filter { release ->
            release.artist.lowercase().split(",", "&", " feat. ", " x ").any { it.trim() == name }
        }.minByOrNull { it.releaseDate }
    }
    // The card's + Pre-Saves it, as the release page's own + does; flipped at once, then confirmed.
    var nextSaved by remember(nextRelease?.id) { mutableStateOf(nextRelease?.saved) }
    val presaveScope = rememberCoroutineScope()
    val toggleNextSaved: (() -> Unit)? = nextRelease?.let { release ->
        com.music.bitchord.data.sources.GlazeCompanion.server()?.let { server ->
            {
                val want = nextSaved != true
                nextSaved = want
                presaveScope.launch {
                    val result = runCatching { com.music.bitchord.data.sources.GlazeCompanion.setPreSaved(server, release.id, want) }.getOrNull()
                    if (result == null) nextSaved = !want else com.music.bitchord.data.sources.GlazeCompanion.updated(result)
                }
            }
        }
    }
    val upcomingRelease = remember(nextRelease) {
        nextRelease?.let { release ->
            ShelfItem(
                title = release.title,
                subtitle = "Album • " + com.music.bitchord.readableReleaseDay(release.releaseDate),
                thumbnailUrl = release.cover,
                videoId = null,
                browseId = com.music.bitchord.data.sources.GlazeCompanion.RELEASE_PREFIX + release.id,
            )
        }
    }
    val topRelease = topReleaseState.value
    val topReleaseId = topRelease?.browseId
    LaunchedEffect(topReleaseId) { topReleaseId?.let { onLoadReleaseLibrary?.invoke(it) } }
    // Apple Music's hero photo and title logo for this artist, found by name and
    // confirmed by a shared title in both catalogues — so it waits for the
    // page's songs. Null until (and unless) it arrives; the YouTube header
    // stands in meanwhile.
    var appleArt by remember(page.browseId) {
        mutableStateOf(if (isArtist) AppleArtistArtRepository.cached(page.browseId) else null)
    }
    val songsLoading = page.songs is UiState.Loading
    LaunchedEffect(page.browseId, page.title, songsLoading) {
        if (isArtist && !songsLoading) {
            appleArt = AppleArtistArtRepository.artFor(page.browseId, page.title) { page.appleCatalogue() } ?: appleArt
        }
    }
    // Secondary provider: handwritten signature from the user's own
    // artist-signature API. Drawn only when Apple has no title logo.
    val signatureEnabled by AppSettings.signatureApiEnabled.collectAsStateWithLifecycle()
    val signatureBaseUrl by AppSettings.signatureApiUrl.collectAsStateWithLifecycle()
    val signatureUpdates by ArtistSignatureRepository.updates.collectAsStateWithLifecycle()
    var signature by remember(page.browseId) { mutableStateOf<ArtistSignature?>(null) }
    // Opened from a track, the title is the whole credit ("A, B & C") until the
    // page loads and swaps in the one artist's name. Searching a credit finds
    // nobody, so a title that reads as several artists waits for that swap; any
    // other name starts the moment the page opens.
    val nameSettled = !page.title.looksLikeCredit() || page.songs !is UiState.Loading
    LaunchedEffect(page.browseId, page.title, nameSettled, signatureEnabled, signatureBaseUrl, signatureUpdates) {
        if (!isArtist || !nameSettled) return@LaunchedEffect
        val base = ArtistSignatureRepository.effectiveBaseUrl(signatureEnabled, signatureBaseUrl)
        if (base == null) {
            signature = null
            return@LaunchedEffect
        }
        // Bounded retries with backoff: a sleeping self-hosted API needs
        // ~30-60s to wake (Render free tier), and a single miss here would
        // otherwise stand until the page is reopened. Genuine misses aren't
        // cached, so retrying is cheap. Covers roughly a full wake cycle.
        val backoff = listOf(0L, 5_000L, 15_000L, 30_000L)
        for (wait in backoff) {
            if (wait > 0) delay(wait)
            signature = ArtistSignatureRepository.cached(page.title, base)
                ?: ArtistSignatureRepository.signatureFor(page.title, base)
                ?: signature
            if (signature != null) return@LaunchedEffect
        }
    }
    // Genre, full release date, lossless and Apple's cover colour (shared with the bars).
    val albumInfo = rememberAlbumInfo(page)
    // Glaze: a multi-disc album is split into "Disc 1", "Disc 2"…, numbered from 1 on each.
    val discs = remember(rawSongs, albumInfo) {
        if (page.type == BrowseType.ALBUM) discNumbers(rawSongs, albumInfo?.trackDiscs.orEmpty()) else emptyMap()
    }
    val discTrackNumbers = remember(rawSongs, discs) {
        if (discs.isEmpty()) emptyMap() else {
            val seen = HashMap<Int, Int>()
            rawSongs.associate { song ->
                val disc = discs[song.videoId] ?: 1
                song.videoId to seen.merge(disc, 1, Int::plus)!!
            }
        }
    }
    val pageBackground by AppSettings.collectionBackground.collectAsStateWithLifecycle()
    val palette = rememberPagePalette(page, appleArt?.heroUrl ?: page.thumbnailUrl, appleArt?.keyColors(), albumInfo)

    // Narrowing the running order in place — the release equivalent of the
    // filter box on the Local Music tab, and the one thing a long track list
    // needs that scrolling can't give it. [searching] says whether it is open;
    // the text itself is the page's, and goes when the box does: a filter left
    // on an album that was closed and reopened would be a page that appears to
    // have lost most of its tracks.
    val closeSearch = { onSearchingChange(false) }
    // Back closes the search first — this handler is registered after the one
    // that pops the page, so it is the one that answers while it's enabled.
    BackHandler(enabled = searching) { closeSearch() }

    // Each surviving row still knows where it sat in the full running order, so
    // an album's track numbers stay the album's rather than becoming positions
    // in the filtered list.
    val matches = remember(songs, query) { songs.matching(query) }
    // What a tap plays: the filtered set of tracks currently standing in the
    // list. When no filter is active, matches contains the full running order
    // and plays the complete release/playlist from the tapped position.
    val queue = remember(matches) { matches.map { it.value } }
    // Spotify / Glaze: songs like a playlist's own, from radios of a few of its
    // tracks, when YouTube offers no suggestions of its own (playlists you don't own).
    var playlistRecs by remember(page.browseId) { mutableStateOf(emptyList<Song>()) }
    var playlistRecsLoading by remember(page.browseId) { mutableStateOf(false) }
    LaunchedEffect(page.browseId, songs.isNotEmpty()) {
        if (page.type != BrowseType.PLAYLIST || page.browseId.startsWith("local:") ||
            page.suggestedSongs.isNotEmpty() || songs.isEmpty() || playlistRecs.isNotEmpty()
        ) return@LaunchedEffect
        playlistRecsLoading = true
        try {
            playlistRecs = runCatching { playlistRecommendations(songs) }.getOrDefault(emptyList())
        } finally {
            playlistRecsLoading = false
        }
    }
    val suggested = remember(page.suggestedSongs, query) {
        page.suggestedSongs.matching(query).map { it.value }
    }

    // A device folder's tab and Downloads selection. The selection answers
    // Back before the search does: it is registered after it, so it is asked
    // first while it is on.
    val folderState = folder?.state
    val folderGroups = if (folder != null && folderState != null) {
        rememberLocalFolderGroups(page.browseId, folder, folderState.tab, rawSongs, query)
    } else {
        emptyList()
    }
    BackHandler(enabled = folderState?.selecting == true) { folderState?.clear() }

    // What marks a row as already downloaded, tinted from the sleeve like the
    // rest of the page. Null on any page that is itself a reading of this
    // device — the Downloads folder, one downloaded playlist — where every row
    // qualifies and the badge would be decoration rather than information.
    val downloadedTint = palette.accent.takeUnless { page.browseId.startsWith("local:") }

    // Animated cover art on the header, the same feature the player has.
    // Albums only: a playlist's artwork is a collage and an artist page's is a
    // photograph, and neither is something a label publishes a canvas for.
    val canvasEnabled by AppSettings.animatedCanvas.collectAsStateWithLifecycle()
    val fullBleedArtwork by AppSettings.fullBleedDetailArtwork.collectAsStateWithLifecycle()
    val autoDetailArtwork by AppSettings.autoDetailArtworkLayout.collectAsStateWithLifecycle()
    val prioritizeSpotifyCanvas by AppSettings.effectivePrioritizeSpotifyCanvas.collectAsStateWithLifecycle()
    val disableSpotifyCanvas by AppSettings.disableSpotifyCanvas.collectAsStateWithLifecycle()
    // The credit line the header shows is the artist as far as the catalogue
    // services are concerned. A browse card's subtitle sometimes omits it, in
    // which case the tracks themselves know who it is.
    val credit = page.headerLines(songs.size).first.ifBlank { songs.firstOrNull()?.artist.orEmpty() }
    var canvas by remember(page.browseId) { mutableStateOf<CanvasArtwork?>(null) }
    LaunchedEffect(page.browseId, page.title, credit, canvasEnabled, prioritizeSpotifyCanvas, disableSpotifyCanvas) {
        // An artist's clip is set below, by the lookup that finds it.
        if (isArtist) return@LaunchedEffect
        if (!canvasEnabled || page.type != BrowseType.ALBUM) {
            canvas = null
            return@LaunchedEffect
        }
        // As on the player: the credit fills in once the tracks load, so this
        // can run twice. Keep a clip that is already playing if the second
        // pass comes back empty.
        if (disableSpotifyCanvas && canvas?.source == CanvasSource.SPOTIFY) canvas = null
        canvas = CanvasRepository.canvasForAlbum(page.title, credit)
            ?: primaryArtist(credit)?.let { CanvasRepository.canvasForAlbum(page.title, it) }
            ?: canvas
    }
    // An artist's clip comes with the same Apple lookup as its photograph.
    val artistVideo = appleArt?.videoUrl
    LaunchedEffect(artistVideo, canvasEnabled) {
        if (isArtist) canvas = artistVideo?.takeIf { canvasEnabled }?.let { CanvasArtwork(url = it) }
    }
    val visibleCanvas = canvas?.takeIf { !disableSpotifyCanvas || it.source != CanvasSource.SPOTIFY }

    // What a source can actually serve these tracks at, asked of the sources the way playback
    // would: Lossless / Hi-Res only when one really has it, High-Quality when that is the best
    // it has, nothing otherwise.
    val sourceQuality by produceState<SourceQuality?>(null, page.browseId, songs.firstOrNull()?.videoId) {
        value = if (page.type == BrowseType.ALBUM) withContext(Dispatchers.IO) { albumSourceQuality(songs) } else null
    }
    // "More from <artist>" and "Similar albums", built from the artist's YouTube Music page.
    var recommendations by remember(page.browseId) { mutableStateOf(emptyList<HomeShelf>()) }
    // Skeleton shelves stand in until the lookup has answered (or given up).
    var recommendationsLoading by remember(page.browseId) { mutableStateOf(page.type == BrowseType.ALBUM) }
    val albumArtistId = songs.firstNotNullOfOrNull { it.artistId }
    val moreFromTitle = stringResource(R.string.more_from_artist, credit)
    val similarTitle = stringResource(R.string.similar_albums)
    LaunchedEffect(page.browseId, albumArtistId) {
        if (page.type != BrowseType.ALBUM || songs.isEmpty()) return@LaunchedEffect
        try {
            // A server's album names its artist by the server's own id: YouTube Music's artist
            // of the same name stands in, so its pages get these shelves too.
            val artistId = albumArtistId?.takeIf { it.startsWith("UC") }
                ?: youtubeArtistId(primaryArtist(credit) ?: credit)
                ?: return@LaunchedEffect
            // YouTube Music answers these artist pages unreliably under load, so a
            // miss is retried a couple of times before the page goes without.
            for (wait in listOf(0L, 2_000L, 6_000L)) {
                if (wait > 0) delay(wait)
                val found = runCatching {
                    albumRecommendations(page.browseId, artistId, moreFromTitle, similarTitle, page.title)
                }.getOrDefault(emptyList())
                if (found.isNotEmpty()) {
                    recommendations = found
                    break
                }
            }
        } finally {
            recommendationsLoading = false
        }
    }

    // Opening the search carries the page up to it, so the field lands just
    // clear of the frosted bar with the tracks under it rather than at the foot
    // of a screen still filled with artwork. Done as an effect rather than in
    // the tap, so the row it scrolls to is already in the list by the time it
    // runs.
    // Glaze's gesture: pulling down at the top of the list reveals the floating
    // search bar; scrolling the list away again hides it while it is empty.
    val canSearch = !isArtist && songs.isNotEmpty()
    val openSearch by rememberUpdatedState({ if (canSearch) onSearchingChange(true) })
    val hideSearch by rememberUpdatedState({ if (query.isBlank()) onSearchingChange(false) })
    val pullDistance = with(LocalDensity.current) { 48.dp.toPx() }
    val hideDistance = with(LocalDensity.current) { 24.dp.toPx() }
    val searchGesture = remember(listState, pullDistance, hideDistance) {
        object : NestedScrollConnection {
            var pull = 0f
            var away = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (!listState.canScrollBackward && available.y > 0f) {
                    pull += available.y
                    away = 0f
                    if (pull >= pullDistance) { openSearch(); pull = 0f }
                } else if (consumed.y < 0f) {
                    away -= consumed.y
                    pull = 0f
                    if (away >= hideDistance) { hideSearch(); away = 0f }
                } else {
                    pull = 0f
                    away = 0f
                }
                return Offset.Zero
            }
        }
    }

    AnimatedContent(
        targetState = activeShelf,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
        },
        label = "artist_shelf_transition",
        modifier = modifier.fillMaxSize(),
    ) { targetShelf ->
        if (targetShelf == null) {
            // An artist's photograph dissolves into [ArtworkPalette.wash]; a
            // release's sleeve sits framed on one flat colour with nothing to
            // dissolve, so it takes the same [ArtworkPalette.background] the
            // bars at either end of the screen are already fading into.
            val pageColor = if (isArtist) palette.wash else palette.background
            BoxWithConstraints(Modifier.fillMaxSize().background(pageColor)) {
                // Measured rather than read off the window. The player runs
                // full-screen at every window size, so this page's own width in
                // landscape is the *whole* window. Straight off that width, the ratio hands back
                // a picture taller than the window itself — the artwork and track list
                // end up scrolled out of sight beneath what reads as a blank page.
                // Capping against the window's own height is what keeps the ratio's
                // math honest once the width it's fed is no longer guaranteed to be
                // the narrow one it was written for.
                val artHeight = (maxWidth / ARTIST_PHOTO_RATIO).coerceAtMost(maxHeight * 0.6f)
                val sleeveSize = (maxWidth * SLEEVE_FRACTION).coerceAtMost(maxHeight * SLEEVE_MAX_HEIGHT)
                // Auto: only a release with a 3:4 motion cover goes full screen, like Apple Music.
                val wantsBleed = if (autoDetailArtwork) canvas?.portraitUrl != null else fullBleedArtwork
                val fullBleedSleeve = wantsBleed && maxWidth <= maxHeight * FULL_BLEED_MAX_HEIGHT
                // Glaze / Apple Music: a soft, lighter glow behind a framed cover,
                // centred on it and following it as the page scrolls.
                if (!isArtist && pageBackground == CollectionBackground.BLUR) {
                    // Arpeggi: the cover blurred across the page, behind everything.
                    FullArtworkBlurBackdrop(
                        image = rememberFullArtworkBlurImage(page.thumbnailUrl),
                        modifier = Modifier.matchParentSize(),
                    )
                }
                if (!isArtist && !fullBleedSleeve && pageBackground == CollectionBackground.APPLE) {
                    val darkPage = palette.onBackground.luminance() > 0.5f
                    val glowColor = lerp(palette.background, Color.White, if (darkPage) 0.17f else 0.32f)
                    val coverCentrePx = with(LocalDensity.current) { (topBarContentPadding() + sleeveSize / 2).toPx() }
                    Box(
                        Modifier
                            .matchParentSize()
                            .drawWithCache {
                                val header = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
                                val glowY = if (header != null) header.offset + coverCentrePx else -size.width
                                val brush = Brush.radialGradient(
                                    0f to glowColor,
                                    1f to palette.background,
                                    center = Offset(size.width / 2f, glowY),
                                    radius = size.width * 0.86f,
                                )
                                onDrawBehind { drawRect(brush) }
                            },
                    )
                }
                // A folder's grid view: the same column count the Library's
                // "Show all" grids reach at this width.
                val gridColumns = libraryGrid(maxWidth - PAGE_GUTTER * 2).columns

                // The artist photo is drawn behind the list rather than in it, so
                // both need to agree on its height without being able to ask each
                // other — see [artHeight]. A release has no such layer: its sleeve
                // is an ordinary item at the top of the list.
                if (isArtist) {
                    PageBackground(
                        page = page,
                        palette = palette,
                        canvas = visibleCanvas,
                        artHeight = artHeight,
                        listState = listState,
                        heroUrl = appleArt?.heroUrl,
                        modifier = Modifier.matchParentSize(),
                    )
                }

                if (reorder != null) {
                    reorder(palette)
                } else LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().nestedScroll(searchGesture),
                    // The artist photo runs edge-to-edge up under the glass bar;
                    // a release header clears the bar itself, see [ReleaseHeader].
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                ) {
            item(key = "header") {
                if (isArtist) {
                    ArtistHeader(page = page, palette = palette, artHeight = artHeight, appleArt = appleArt, signature = signature)
                } else {
                    ReleaseHeader(
                        page = page,
                        palette = palette,
                        sleeveSize = sleeveSize,
                        fullBleed = fullBleedSleeve,
                        canvas = visibleCanvas,
                        songs = songs,
                        albumInfo = albumInfo,
                        sourceQuality = sourceQuality,
                        onPlay = { onPlay(songs) },
                        onShuffle = { onShuffle(songs) },
                        // A folder's credit says where its tracks live, not who made them.
                        onArtistClick = onArtistClick.takeIf { folder == null },
                        onToggleLibrary = onToggleLibrary,
                        onDownloadToServer = onDownloadToServer,
                        serverDownload = serverDownload,
                        onDownloadAll = folder?.onDownload?.let { download -> { download(songs) } },
                    )
                }
            }


            val description = page.description

            if (songs.isNotEmpty() && isArtist) {
                item(key = "actions") {
                    ActionRow(
                        onPlay = { onPlay(songs) },
                        onShuffle = { onShuffle(songs) },
                        subscription = page.subscription?.takeIf { onToggleSubscription != null },
                        onToggleSubscription = onToggleSubscription,
                        bottomSpace = 22.dp,
                        playColor = appleArt?.keyColor?.let { Color(it) },
                    )
                }
            }

            // The artist's next album when one is announced, otherwise their newest release of
            // any kind (album, EP or single), pulled out as a card of its own.
            if (isArtist && upcomingRelease != null) {
                item(key = "top-release") {
                    TopReleaseCard(
                        item = upcomingRelease,
                        palette = palette,
                        onClick = { onSectionItemClick(upcomingRelease) },
                        onLongPress = null,
                        saved = nextSaved,
                        onToggleSaved = toggleNextSaved,
                        label = stringResource(R.string.upcoming_album),
                    )
                }
            } else if (topRelease != null) {
                item(key = "top-release") {
                    TopReleaseCard(
                        item = topRelease,
                        palette = palette,
                        onClick = { onSectionItemClick(topRelease) },
                        onLongPress = onSectionItemLongPress?.let { { it(topRelease) } },
                        saved = topRelease.browseId?.let { releaseLibrary[it]?.saved },
                        onToggleSaved = onToggleReleaseLibrary?.let { toggle ->
                            topRelease.browseId?.let { id -> { toggle(id) } }
                        },
                    )
                }
            }

            // YouTube's own editorial blurb — an album or an artist only, per
            // [DetailPage.description]. A playlist never carries one, and the
            // section is skipped for it even on the rare response that does.
            //
            // On an artist page it closes the page, after the shelves, rather
            // than sitting between the action row and the songs.
            // The artist's counts close it, under the text, so a page whose blurb is
            // empty still gets the section for them.
            val hasStats = isArtist &&
                (page.subscriberCountText != null || page.monthlyListenerCount != null)
            // The header holds Apple's logo here, so the ink goes at the foot of the page instead.
            val aboutSignature = if (isArtist && appleArt?.logoUrl != null) signature else null
            val showAbout = (!description.isNullOrBlank() &&
                (page.type == BrowseType.ALBUM || isArtist)) || hasStats || aboutSignature != null
            fun LazyListScope.aboutItem() {
                if (!showAbout) return
                if (!isArtist && page.type == BrowseType.ALBUM && !description.isNullOrBlank()) {
                    item(key = "about") { AlbumDescription(description, palette) }
                    return
                }
                item(key = "about") {
                    // Clear of the last shelf above it.
                    Box(Modifier.padding(top = if (isArtist) 28.dp else 0.dp)) {
                    AboutSection(
                        title = stringResource(
                            if (isArtist) R.string.about_artist else R.string.about_album,
                        ),
                        text = description?.takeIf { it.isNotBlank() },
                        palette = palette,
                        horizontalPadding = if (isArtist) ARTIST_CONTENT_GUTTER else ABOUT_GUTTER,
                        stats = if (hasStats) {
                            {
                                ArtistStatsRow(
                                    subscriberCountText = page.subscriberCountText,
                                    monthlyListenerCount = page.monthlyListenerCount,
                                    palette = palette,
                                )
                            }
                        } else {
                            null
                        },
                        footer = aboutSignature?.let { sig ->
                            {
                                AboutSignature(
                                    signature = sig,
                                    name = page.title,
                                    palette = palette,
                                    horizontalPadding = ARTIST_CONTENT_GUTTER,
                                )
                            }
                        },
                    )
                    }
                }
            }
            if (!isArtist) aboutItem()
            if (page.browseId.startsWith(com.music.bitchord.data.sources.GlazeCompanion.RELEASE_PREFIX)) {
                item(key = "release-countdown") {
                    val upcoming by com.music.bitchord.data.sources.GlazeCompanion.upcoming.collectAsStateWithLifecycle()
                    val release = upcoming.firstOrNull { "release:${it.id}" == page.browseId }
                        ?: com.music.bitchord.data.sources.GlazeCompanion.pageReleases[page.browseId.removePrefix("release:")]
                    if (release != null) ReleaseCountdown(release, palette)
                }
            }
            if (!isArtist && songs.isNotEmpty()) {
                item(key = "tracks-divider") {
                    // A playlist's rows carry artwork, so its separators start after it.
                    AlbumDivider(palette, start = if (page.type == BrowseType.ALBUM) ALBUM_DIVIDER_INSET else ROW_DIVIDER_INSET)
                }
            }

            when (val state = page.songs) {
                is UiState.Loading -> detailSkeleton(isArtist)
                is UiState.Error -> item { MessageState(state.message) }
                is UiState.Success -> if (folder != null && folderState != null) {
                    localFolderItems(
                        folder = folder,
                        state = folderState,
                        songs = queue,
                        groups = folderGroups,
                        query = query,
                        columns = gridColumns,
                        palette = palette,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongClick = onSongClick,
                        onSongLongPress = onSongLongPress,
                        onSongSwipe = onSongSwipe,
                    )
                } else if (isArtist) {
                    // An artist's full song list would bury the album shelves, so
                    // it pages sideways four at a time and stops at twenty.
                    item {
                        val top = state.data.take(MAX_ARTIST_SONGS)
                        val isCurrentRow = playingRowMatcher(top, currentSong)
                        SectionHeading(
                            title = stringResource(R.string.top_songs),
                            palette = palette,
                            horizontalPadding = ARTIST_CONTENT_GUTTER,
                        )
                        BoxWithConstraints {
                            val columnWidth = trackColumnWidth(maxWidth)
                            val topSongsState = rememberLazyListState()
                            LazyRow(
                                state = topSongsState,
                                contentPadding = PaddingValues(horizontal = ARTIST_CONTENT_GUTTER),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                flingBehavior = rememberSnapFlingBehavior(lazyListState = topSongsState),
                            ) {
                                items(top.chunked(SONGS_PER_COLUMN)) { column ->
                                    Column(Modifier.width(columnWidth)) {
                                        column.forEach { song ->
                                            CompactSongRow(
                                                song = song,
                                                palette = palette,
                                                onClick = { onSongClick(top, top.indexOf(song)) },
                                                onLongPress = { onSongLongPress(song) },
                                                downloadedTint = downloadedTint,
                                                isCurrent = isCurrentRow(song),
                                                isPlaying = isCurrentRow(song) && isPlaying,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Every row on an album carries the same sleeve, which is
                    // already the largest thing on the page — Apple Music
                    // numbers those rows instead, and so does this.
                    val numbered = page.type == BrowseType.ALBUM
                    val isCurrentRow = playingRowMatcher(matches.map { it.value }, currentSong)
                    if (matches.isEmpty() && state.data.isNotEmpty()) {
                        item(key = "no-matches") {
                            MessageState(stringResource(R.string.nothing_matches, query))
                        }
                    }
                    itemsIndexed(
                        items = matches,
                        key = { _, entry -> "${entry.index}_${entry.value.videoId}" },
                    ) { position, entry ->
                        val song = entry.value
                        val isCurrent = isCurrentRow(song)
                        // Disc headers only while the rows are in the album's own order.
                        val showDiscs = numbered && discs.isNotEmpty() && songSort == SongSort.DEFAULT && query.isBlank()
                        val disc = discs[song.videoId]
                        if (showDiscs && disc != null && (position == 0 || discs[matches[position - 1].value.videoId] != disc)) {
                            DiscHeading(disc, palette)
                        }
                        SongRow(
                            song = if (numbered) {
                                song
                            } else {
                                song.copy(thumbnailUrl = song.thumbnailUrl ?: page.thumbnailUrl)
                            },
                            onClick = {
                                onSongClick(queue, position)
                            },
                            onLongPress = { onSongLongPress(song) },
                            onSwipeToQueue = { onSongSwipe(song) },
                            rowBackground = Color.Transparent,
                            // The track's place on the release, not its place in
                            // what the filter — or a sort — left standing.
                            trackNumber = (discTrackNumbers[song.videoId] ?: originalTrackNumbers[song.videoId]).takeIf { numbered },
                            subtitleColor = palette.onBackgroundVariant,
                            downloadedTint = downloadedTint,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            activeTint = palette.accent,
                            searchPlayingStyle = true,
                        )
                        val nextStartsDisc = showDiscs && position < matches.lastIndex &&
                            discs[matches[position + 1].value.videoId] != disc
                        if (position < matches.lastIndex && !nextStartsDisc) {
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    start = if (numbered) NUMBERED_ROW_DIVIDER_INSET else ROW_DIVIDER_INSET,
                                    end = ALBUM_DIVIDER_INSET,
                                ),
                                thickness = 0.5.dp,
                                color = palette.divider,
                            )
                        }
                    }
                }
            }

            // Tracks YouTube offers to round the playlist out, never folded
            // into the list above — see [DetailPage.suggestedSongs].
            if (suggested.isNotEmpty()) {
                item(key = "suggested-heading") {
                    SectionHeading(stringResource(R.string.suggested), palette)
                }
                val isSuggestedCurrent = playingRowMatcher(suggested, currentSong)
                itemsIndexed(
                    suggested,
                    key = { _, song -> "suggested-${song.videoId}" },
                ) { index, song ->
                    SuggestedSongRow(
                        song = song,
                        palette = palette,
                        onClick = { onSongClick(suggested, index) },
                        onLongPress = { onSongLongPress(song) },
                        onAdd = { onAddSuggested(song) },
                        downloadedTint = downloadedTint,
                        isCurrent = isSuggestedCurrent(song),
                        isPlaying = isSuggestedCurrent(song) && isPlaying,
                    )
                    if (index < suggested.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = ROW_DIVIDER_INSET),
                            thickness = 0.5.dp,
                            color = palette.divider,
                        )
                    }
                }
            }

            if (suggested.isEmpty() && playlistRecsLoading && query.isBlank()) {
                songListSkeleton(count = 4, keyPrefix = "skeleton:recs")
            }
            if (suggested.isEmpty() && playlistRecs.isNotEmpty() && query.isBlank()) {
                item(key = "recs-separator") {
                    // The heading carries 10dp of its own above the text, so this
                    // leaves the same gap on both sides of the line.
                    Box(Modifier.padding(top = RECS_SEPARATOR_GAP).bottomSectionShade()) {
                        Box(Modifier.padding(bottom = RECS_SEPARATOR_GAP - 10.dp)) {
                            AlbumDivider(palette, start = ROW_DIVIDER_INSET)
                        }
                    }
                }
                item(key = "recs-heading") {
                    Column(Modifier.bottomSectionShade()) {
                        SectionHeading(stringResource(R.string.recommended_songs), palette)
                        Text(
                            text = stringResource(R.string.recommended_songs_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.onBackgroundVariant,
                            modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, bottom = 8.dp),
                        )
                    }
                }
                val isRecCurrent = playingRowMatcher(playlistRecs, currentSong)
                itemsIndexed(playlistRecs, key = { _, song -> "rec-${song.videoId}" }) { index, song ->
                    Box(Modifier.bottomSectionShade()) { SuggestedSongRow(
                        song = song,
                        palette = palette,
                        onClick = { onSongClick(playlistRecs, index) },
                        onLongPress = { onSongLongPress(song) },
                        // Not your playlist to add to, so the button queues it instead.
                        onAdd = { onSongSwipe(song) },
                        addIcon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        addLabel = stringResource(R.string.add_to_queue),
                        downloadedTint = downloadedTint,
                        isCurrent = isRecCurrent(song),
                        isPlaying = isRecCurrent(song) && isPlaying,
                    ) }
                }
                item(key = "section-tail") { BottomSectionTail() }
            }

            if (page.type == BrowseType.ALBUM && songs.isNotEmpty() && query.isBlank()) {
                item(key = "album-footer") {
                    AlbumFooter(
                        releaseDate = albumInfo?.releaseDate,
                        songCount = songs.size,
                        playtime = songs.playtime(),
                        palette = palette,
                        recordLabel = albumInfo?.recordLabel,
                    )
                }
            }
            // A release that isn't out yet has no "more like this": no shelves, and no darker tail.
            val isReleasePage = page.browseId.startsWith(com.music.bitchord.data.sources.GlazeCompanion.RELEASE_PREFIX)
            val albumSection = page.type == BrowseType.ALBUM && songs.isNotEmpty() && query.isBlank() && !isReleasePage

            // Albums / Singles & EPs carousels (artist pages), and an album's recommendations.
            itemsIndexed(if (isReleasePage) emptyList() else page.sections + recommendations, key = { i, _ -> "shelf-$i" }) { _, shelf ->
                val canShowAll = shelf.items.size > ARTIST_ROW_MAX_ITEMS
                val displayItems = remember(shelf.items) {
                    if (canShowAll) shelf.items.take(ARTIST_ROW_MAX_ITEMS) else shelf.items
                }
                // An album's shelves sit as close under the footer's line as the footer does.
                Column(
                    Modifier
                        .then(if (albumSection) Modifier.bottomSectionShade() else Modifier)
                        .padding(top = if (page.type == BrowseType.ALBUM) ALBUM_FOOTER_GAP - 10.dp else 22.dp),
                ) {
                    SectionHeading(
                        title = shelf.title,
                        palette = palette,
                        onShowAll = if (canShowAll) { { onActiveShelfChange(shelf) } } else null,
                        horizontalPadding = ARTIST_CONTENT_GUTTER,
                    )
                    val carouselState = rememberLazyListState()
                    LazyRow(
                        state = carouselState,
                        contentPadding = PaddingValues(horizontal = ARTIST_CONTENT_GUTTER),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        flingBehavior = rememberSnapFlingBehavior(lazyListState = carouselState),
                    ) {
                        items(displayItems) { item ->
                            SectionCard(
                                item = item,
                                palette = palette,
                                onClick = { onSectionItemClick(item) },
                                onLongPress = onSectionItemLongPress?.let { { it(item) } },
                                modifier = Modifier.width(if (item.isVideo) VIDEO_CARD_WIDTH else SHELF_CARD_WIDTH),
                            )
                        }
                    }
                }
            }
            if (albumSection && recommendationsLoading && recommendations.isEmpty()) {
                items(2, key = { "skeleton:recs:$it" }) { index ->
                    Box(Modifier.bottomSectionShade().padding(top = ALBUM_FOOTER_GAP - 10.dp)) { ShelfSkeleton(index = index) }
                }
            }
            if (albumSection) item(key = "section-tail") { BottomSectionTail() }
            if (isArtist) aboutItem()
        }

                if (folder != null && folderState != null && folderState.selecting) {
                    LocalFolderSelectionBar(
                        folder = folder,
                        state = folderState,
                        songs = songs,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = topBarContentPadding(), start = PAGE_GUTTER, end = PAGE_GUTTER),
                    )
                }

    }
} else {
    ArtistShelfGridPage(
        shelf = targetShelf,
        palette = palette,
        onItemClick = onSectionItemClick,
        onItemLongPress = onSectionItemLongPress,
        contentPadding = contentPadding,
    )
}
}
}

/**
 * An album or playlist, laid out as Apple Music does: the sleeve framed in the
 * middle of the page, the title, credit and meta under it, then Shuffle, a
 * labelled Play pill and Add to Library across one row.
 *
 * One flat page colour behind all of it — the sleeve is a picture *on* the
 * page, not the page's top edge, so there is nothing to blend into and no
 * layer drawn behind the list. Search, sort and the overflow menu are the top
 * bar's, not this header's.
 */
@Composable
private fun ReleaseHeader(
    page: DetailPage,
    palette: ArtworkPalette,
    sleeveSize: Dp,
    /**
     * Runs the sleeve the page's width off the top of the screen, dissolving
     * into the page colour like the player's banner, in place of the framed
     * one. Everything under it is unchanged.
     */
    fullBleed: Boolean,
    canvas: CanvasArtwork?,
    songs: List<Song>,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onArtistClick: ((String, String) -> Unit)?,
    albumInfo: AppleMusicCanvas.AlbumInfo? = null,
    sourceQuality: SourceQuality? = null,
    onToggleLibrary: (() -> Unit)?,
    onDownloadToServer: (() -> Unit)? = null,
    serverDownload: com.music.bitchord.data.sources.GlazeCompanion.Job? = null,
    /** The Cached songs folder's: saves every track on it to Downloads. Null hides it. */
    onDownloadAll: (() -> Unit)? = null,
) {
    val (credit, meta) = page.headerLines(songs.size, songs.playtime())
    // Every row on a release carries the same credit — see [pageCredit] — so
    // the first one speaks for the whole page, the same source the rows'
    // own long-press "Open artist" already reads from.
    val artist = songs.firstOrNull()
    var artFailed by remember(page.thumbnailUrl) { mutableStateOf(false) }
    // A placeholder is a sleeve or it is nothing, as on the player: blown out
    // to the page's width it would only tint the top of the screen.
    val bleed = fullBleed && page.thumbnailUrl != null && !artFailed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Clear of the floating bar, which only a full-bleed sleeve runs under.
            // The same 12dp below the controls as between the tags and the controls.
            .padding(top = if (bleed) 0.dp else topBarContentPadding(), bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (bleed) {
            // A 3:4 motion cover fills a taller header in its own shape; otherwise the square sleeve.
            val tallClip = canvas?.portraitUrl?.let { canvas.copy(url = it, fallbackUrl = canvas.url, portraitUrl = null) }
            // The clip fades out its own bottom edge, so a still left under it shows
            // through there as a second, mismatched cover. Hidden once a frame is up.
            var clipShown by remember(canvas) { mutableStateOf(false) }
            Box(
                Modifier
                    .fillMaxWidth()
                    // Laid out shorter than it draws, so the title rides up into
                    // the dissolve rather than starting under a band of page
                    // colour where the sleeve has already gone.
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val height = if (tallClip != null) {
                            (placeable.width * TALL_HEADER_LAYOUT_RATIO).toInt()
                        } else {
                            (placeable.height - FULL_BLEED_TITLE_OVERLAP.roundToPx()).coerceAtLeast(0)
                        }
                        layout(placeable.width, height) { placeable.place(0, 0) }
                    }
                    .aspectRatio(if (tallClip != null) TALL_COVER_ASPECT else 1f),
            ) {
                var canvasCover by remember(canvas) { mutableStateOf(0f) }
                AsyncImage(
                    model = page.thumbnailUrl.artworkAt(HEADER_ART_PX),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onState = { artFailed = it is AsyncImagePainter.State.Error },
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(if (clipShown) 0f else 1f)
                        // The mask below erases part of what this layer drew,
                        // which it can only do in a buffer of its own.
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                            // Hand off to the clip so its dissolve reveals the
                            // page colour, not the still sleeve underneath.
                            alpha = 1f - canvasCover
                        }
                        .drawWithContent {
                            drawContent()
                            // The player banner's dissolve, along smootherstep so
                            // neither end of it shows as a line.
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = Array(FADE_CURVE_STOPS + 1) { i ->
                                        val t = i / FADE_CURVE_STOPS.toFloat()
                                        t to Color.Black.copy(alpha = 1f - smootherStep(t))
                                    },
                                    startY = size.height * (1f - FULL_BLEED_FADE_FRACTION),
                                    endY = size.height,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                        .background(palette.elevated),
                )
                // Beside the still rather than inside its layer: a clip's
                // TextureView takes no Compose mask, so it dissolves itself.
                (tallClip ?: canvas)?.let { clip ->
                    CanvasArtworkPlayer(
                        canvas = clip,
                        isPlaying = true,
                        bottomFade = FULL_BLEED_FADE_FRACTION,
                        onRenderedChanged = { clipShown = it },
                        onCoverChanged = { canvasCover = it },
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        } else Box(
            Modifier
                .size(sleeveSize)
                .shadow(SLEEVE_SHADOW, SLEEVE_SHAPE)
                .background(palette.elevated),
        ) {
            // A cover that isn't there, or wouldn't load — a device folder, a
            // playlist with nothing on it yet, files with no picture — gets a
            // placeholder sleeve rather than an empty square. Not shown while a
            // real one is still on its way, so a loading cover doesn't flash
            // a glyph first.
            if (page.thumbnailUrl == null || artFailed) {
                CoverPlaceholder(
                    icon = page.placeholderIcon(),
                    base = palette.elevated,
                    tint = palette.onBackgroundVariant,
                    modifier = Modifier.matchParentSize(),
                )
            }
            AsyncImage(
                model = page.thumbnailUrl.artworkAt(HEADER_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onState = { artFailed = it is AsyncImagePainter.State.Error },
                modifier = Modifier.matchParentSize(),
            )
            // Animated cover art, inside the same frame. Always running:
            // unlike the player's sleeve there is no transport here to follow,
            // and the page is only up while it's being read.
            canvas?.let { clip ->
                CanvasArtworkPlayer(
                    canvas = clip,
                    isPlaying = true,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

        Spacer(Modifier.height(if (bleed) 6.dp else 19.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = palette.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = HEADER_GUTTER),
        )
        if (credit.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = credit,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                color = palette.onBackground.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = HEADER_GUTTER)
                    .let { m ->
                        val id = artist?.artistId
                        if (id == null || onArtistClick == null) {
                            m
                        } else {
                            m.clip(RoundedCornerShape(6.dp))
                                .clickable { onArtistClick(id, artist.artist) }
                        }
                    },
            )
        }
        // Apple Music's line for an album: genre, year, and Lossless when it can be had.
        // Track count and length move to the footer under the tracks.
        val isAlbum = page.type == BrowseType.ALBUM
        val year = albumInfo?.releaseDate?.take(4) ?: RELEASE_YEAR.find(page.subtitle)?.value
        val headerMeta = if (isAlbum) listOfNotNull(albumInfo?.genre, year).joinToString(" · ") else meta
        val shownQuality = sourceQuality.takeIf { isAlbum && (it == SourceQuality.HI_RES_LOSSLESS || it == SourceQuality.LOSSLESS || it == SourceQuality.HIGH_QUALITY) }
        if (headerMeta.isNotBlank() || shownQuality != null) {
            Spacer(Modifier.height(5.dp))
            Row(
                modifier = Modifier.padding(horizontal = HEADER_GUTTER),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val metaStyle = MaterialTheme.typography.bodySmall
                if (headerMeta.isNotBlank()) {
                    Text(
                        text = headerMeta,
                        style = metaStyle,
                        color = palette.onBackgroundVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                if (shownQuality != null) {
                    if (headerMeta.isNotBlank()) Text(" · ", style = metaStyle, color = palette.onBackgroundVariant)
                    SourceQualityBadge(quality = shownQuality, color = palette.onBackgroundVariant, style = metaStyle)
                }
            }
        }

        if (songs.isNotEmpty()) {
            // Only where YouTube said the release can be saved and the caller
            // is willing to take the write — see [onToggleLibrary].
            val library = page.library?.takeIf { onToggleLibrary != null }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .widthIn(max = RELEASE_ACTIONS_MAX_WIDTH)
                    .fillMaxWidth()
                    .padding(horizontal = HEADER_GUTTER),
                horizontalArrangement = Arrangement.spacedBy(RELEASE_ACTION_GAP, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (page.browseId.startsWith("local:")) {
                    // Already on this device — a folder, a download, a local
                    // playlist — so there is no library to add it to, and a
                    // greyed-out plus would only promise one. Apple Music's
                    // own pair for that case: Play and Shuffle, side by side.
                    ActionPill(
                        icon = BitChordIcons.Play,
                        label = stringResource(R.string.play),
                        onClick = onPlay,
                        filled = true,
                        size = RELEASE_SIDE_BUTTON,
                        modifier = Modifier.weight(1f),
                    )
                    ActionPill(
                        icon = BitChordIcons.Shuffle,
                        label = stringResource(R.string.shuffle),
                        onClick = onShuffle,
                        filled = false,
                        size = RELEASE_SIDE_BUTTON,
                        modifier = Modifier.weight(1f),
                    )
                    // The song cache's copies, saved to Downloads in one go —
                    // from disk, so it works offline. See [LocalFolder.onDownload].
                    onDownloadAll?.let {
                        CircleIconButton(
                            icon = Icons.Rounded.Download,
                            contentDescription = stringResource(R.string.download_all),
                            onClick = it,
                            size = RELEASE_SIDE_BUTTON,
                        )
                    }
                    return@Row
                }
                CircleIconButton(
                    icon = BitChordIcons.Shuffle,
                    contentDescription = stringResource(R.string.shuffle),
                    onClick = onShuffle,
                    haptic = Haptic.Resume,
                    size = RELEASE_SIDE_BUTTON,
                )
                ActionPill(
                    icon = BitChordIcons.Play,
                    label = stringResource(R.string.play),
                    onClick = onPlay,
                    filled = true,
                    size = RELEASE_SIDE_BUTTON,
                    modifier = Modifier.width(RELEASE_PLAY_WIDTH),
                )
                if (library != null) {
                    CircleIconButton(
                        // A tick, not a filled-in plus: the pair reads as
                        // "not yet / done", which is what the state is.
                        icon = if (library.saved) BitChordIcons.Check else BitChordIcons.Plus,
                        contentDescription = if (library.saved) {
                            stringResource(R.string.remove_from_library)
                        } else {
                            stringResource(R.string.add_to_library)
                        },
                        onClick = { onToggleLibrary?.invoke() },
                        haptic = if (library.saved) Haptic.ToggleOff else Haptic.ToggleOn,
                        size = RELEASE_SIDE_BUTTON,
                    )
                } else if (onDownloadToServer != null) {
                    // No library to save to here, but a music server to download into:
                    // the companion fetches the album into it (a tick once it's there).
                    CircleIconButton(
                        icon = when {
                            serverDownload?.finished == true -> BitChordIcons.Check
                            serverDownload?.active == true -> Icons.Rounded.HourglassTop
                            serverDownload?.failed == true -> Icons.Rounded.Refresh
                            else -> Icons.Rounded.CloudDownload
                        },
                        contentDescription = stringResource(R.string.download_to_server),
                        onClick = onDownloadToServer,
                        size = RELEASE_SIDE_BUTTON,
                    )
                } else {
                    // Shown but greyed out for a guest, rather than leaving a
                    // hole beside Play.
                    CircleIconButton(
                        icon = BitChordIcons.Plus,
                        contentDescription = stringResource(R.string.add_to_library),
                        onClick = {},
                        size = RELEASE_SIDE_BUTTON,
                        enabled = false,
                    )
                }
            }
        }
    }
}


internal fun List<Song>.sortedForDetail(sort: SongSort): List<Song> = when (sort) {
    SongSort.DEFAULT -> this
    // Untagged rows go after the tagged ones, in the order they already had,
    // rather than interleaving at the head; a page with no tags at all is
    // left exactly as it came.
    SongSort.TRACK_NUMBER -> if (none { it.trackNumber != null }) {
        this
    } else {
        withIndex()
            .sortedWith(
                compareBy<IndexedValue<Song>> { (_, song) -> song.trackNumber ?: Int.MAX_VALUE }
                    .thenBy { (position, _) -> position },
            )
            .map { it.value }
    }
    SongSort.TITLE_ASC -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    SongSort.TITLE_DESC -> sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
    // A catalogue row carries no added date, so its place in the running
    // order stands in for one: a playlist is appended to as songs are added,
    // and reversed that puts the most recent addition on top. Rows that do
    // carry a MediaStore timestamp — device tracks, downloads — are dated
    // properly, with the position order left to break the ties.
    SongSort.DATE_ADDED_ASC -> withIndex()
        .sortedWith(
            compareBy<IndexedValue<Song>> { (_, song) ->
                song.localDateAddedSeconds ?: Long.MAX_VALUE
            }.thenBy { (position, _) -> position },
        )
        .map { it.value }
    SongSort.DATE_ADDED_DESC -> withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<Song>> { (_, song) ->
                song.localDateAddedSeconds ?: Long.MIN_VALUE
            }.thenByDescending { (position, _) -> position },
        )
        .map { it.value }
}

/**
 * The rows a typed query leaves standing, each still carrying its place in the
 * full list — see the track numbers on an album, which are the release's own
 * and not positions in whatever the filter left.
 */
internal fun List<Song>.matching(query: String): List<IndexedValue<Song>> {
    val all = withIndex().toList()
    if (query.isBlank()) return all
    return all.filter { (_, song) -> song.matchesSearch(query) }
}

/**
 * An artist: their name across the foot of the photo [PageBackground] is
 * drawing behind this.
 *
 * The photograph itself is not here — it is pinned behind the list while this
 * scrolls over it. What this item holds in its place is a spacer of exactly
 * the picture's height, which is what keeps the two in step: the list
 * reserves the room, the background fills it.
 */
@Composable
private fun ArtistHeader(
    page: DetailPage,
    palette: ArtworkPalette,
    artHeight: Dp,
    appleArt: AppleArtistArt?,
    signature: ArtistSignature?,
) {
    Box(Modifier.fillMaxWidth()) {
        Spacer(Modifier.fillMaxWidth().height(artHeight + HEADER_DROP - ARTIST_HEADER_LIFT))
        val logoUrl = appleArt?.logoUrl
        // Secondary provider: drawn only when Apple has no title logo.
        val signatureUrl = signature?.imageUrl.takeIf { logoUrl == null }
        val imageUrl = logoUrl ?: signatureUrl
        // Signature vectors are dark ink on transparency — over a dark artist
        // photo they vanish, so they are tinted white like the web UI's
        // inverted plate. Scanned photos (jpeg/png) keep their own pixels:
        // tinting an opaque light scan would flatten it into a white blob.
        val tint = if (logoUrl == null && signature?.format == "svg") {
            ColorFilter.tint(Color.White)
        } else {
            null
        }
        // Whether the logo has actually been drawn. Until it has — a first fetch
        // of an Apple logo is a few hundred KB the CDN may still be resizing — the
        // name stands in for it, so the header is never left with nothing in it.
        var logoShown by remember(imageUrl) { mutableStateOf(false) }
        if (imageUrl != null) {
            val context = LocalPlatformContext.current
            var attempt by remember(imageUrl) { mutableIntStateOf(0) }
            var failed by remember(imageUrl) { mutableStateOf(false) }
            val isSignatureSvg = logoUrl == null && signature?.format == "svg"
            // Scanned paper (jpeg/png) goes through the ink transformation
            // (white paper to transparency, strokes to white); vectors are
            // tinted at draw time instead.
            val isSignatureScan = logoUrl == null && signature != null && !isSignatureSvg
            val aspect = (if (logoUrl != null) appleArt?.logoAspect else signature?.aspect)
                ?.coerceIn(0.4f, 6f) ?: 1f
            // Remembered, and at the size the URL already names: nothing about the
            // load waits on layout to decide how big to decode, and a recomposition
            // can't hand Coil a new request that cancels the one in flight.
            // Vectors decode small (Charlie Puth's is 213px) and would upscale
            // ~3x into a blur, so SVGs render large; rasters keep ORIGINAL
            // since decoding past their pixels only spends memory.
            val request = remember(imageUrl, attempt) {
                ImageRequest.Builder(context).data(imageUrl).apply {
                    if (isSignatureSvg) {
                        size(SVG_DECODE_WIDTH, (SVG_DECODE_WIDTH / aspect).roundToInt())
                    } else {
                        size(coil3.size.Size.ORIGINAL)
                    }
                    if (isSignatureScan) transformations(SignatureInkTransformation)
                }.build()
            }
            // A dropped fetch gets two more tries before the name is left in place.
            LaunchedEffect(failed) {
                if (failed && attempt < LOGO_RETRIES) {
                    delay(LOGO_RETRY_DELAY_MS)
                    failed = false
                    attempt++
                }
            }
            // Apple's own title logo stands in for the name, laid over its photo.
            // Capped in height as well as width: a logo set on two or three
            // lines is nearly square, and by width alone it would swallow the
            // photograph it is meant to sit on. A signature is a panoramic ink
            // strip and reads best smaller than a title logo.
            val isSignature = logoUrl == null && signature != null
            val sizeFactor = if (isSignature) SIGNATURE_SCALE else 1f
            val maxLogoHeight = artHeight * ARTIST_LOGO_MAX_HEIGHT * sizeFactor
            Column(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            AsyncImage(
                model = request,
                contentDescription = page.title,
                contentScale = ContentScale.Fit,
                colorFilter = tint,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> logoShown = true
                        is AsyncImagePainter.State.Error -> {
                            val cause = (state as? AsyncImagePainter.State.Error)
                                ?.result?.throwable?.let { "${it.javaClass.simpleName}: ${it.message}" }
                            Log.d("ArtistHeader", "image load failed: $imageUrl attempt $attempt cause=$cause")
                            failed = true
                        }
                        else -> Unit
                    }
                },
                modifier = Modifier
                    .padding(top = 14.dp, bottom = if (isSignature) 6.dp else 20.dp)
                    // Sized from the incoming width in layout rather than through a
                    // BoxWithConstraints, which would subcompose the image.
                    .layout { measurable, constraints ->
                        val widest = constraints.maxWidth * ARTIST_LOGO_WIDTH * sizeFactor
                        val height = minOf(widest / aspect, maxLogoHeight.toPx()).roundToInt()
                        val width = (height * aspect).roundToInt()
                        val placeable = measurable.measure(Constraints.fixed(width, height))
                        layout(width, height) { placeable.place(0, 0) }
                    },
            )
            if (isSignature && logoShown) {
                SignedByCaption(
                    name = page.title,
                    palette = palette,
                    modifier = Modifier.padding(horizontal = HEADER_GUTTER).padding(bottom = 20.dp),
                )
            }
            }
        }
        if (!logoShown) Text(
            text = page.title,
            style = MaterialTheme.typography.displayLarge,
            color = palette.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Bottom is half the top inset — the stats pills (or, absent
                // those, the action row) sit closer under the name than the
                // name sits under the artwork.
                .padding(start = HEADER_GUTTER, end = HEADER_GUTTER, top = 14.dp, bottom = 7.dp),
        )
    }
}

/**
 * Everything on an artist page that is colour rather than words: the photo,
 * with its foot dissolved into the solid [ArtworkPalette.wash] the page is
 * painted in. A release page has no such layer — its sleeve is framed.
 *
 * A layer of its own so the list can be scrolled over it rather than carry it.
 * It follows the scroll instead of being scrolled: the list owns the gesture
 * and reserves the room, and the picture is offset to follow whatever the list
 * did with item zero. Read in a placement block, so a scroll moves it without
 * recomposing anything.
 *
 * The join used to be hidden by a live blur laid across it, re-run on every
 * frame of every scroll. A blurred copy of the sleeve does the same job here
 * drawn once: nothing about it changes as the page moves, so it is rasterised
 * on the first frame and only composited after that.
 */
@Composable
private fun PageBackground(
    page: DetailPage,
    palette: ArtworkPalette,
    canvas: CanvasArtwork?,
    artHeight: Dp,
    listState: LazyListState,
    heroUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    // Not under a canvas: a still blurred over the foot of a moving clip would
    // freeze the bottom of the video into the wrong picture.
    val softenFoot = canvas == null && !reduceDynamicBlur &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val art = heroUrl ?: page.thumbnailUrl.artworkAt(HEADER_ART_PX)

    Box(modifier.clipToBounds()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(artHeight)
                .offset { IntOffset(0, listState.headerTop(artHeight.toPx()).roundToInt()) },
        ) {
            AsyncImage(
                model = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .background(palette.elevated),
            )

            // Above the still art but below the scrim, so the scrim that
            // settles the header into the page still sits over it. Always
            // running: unlike the player's sleeve there is no transport here
            // to follow, and the page is only up while it's being read.
            canvas?.let { clip ->
                CanvasArtworkPlayer(
                    canvas = clip,
                    isPlaying = true,
                    modifier = Modifier.matchParentSize(),
                )
            }

            // The same sleeve, blurred and faded in over the lower half, so the
            // picture loses its detail before it loses its colour — a merge
            // rather than a fade to a flat tint. The same request as the sharp
            // copy, so it is a memory-cache hit and lands on the same frame.
            if (softenFoot) {
                AsyncImage(
                    model = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        // Offscreen so the mask cuts the blurred result rather
                        // than each draw beneath it.
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(SOFT_FOOT_MASK, blendMode = BlendMode.DstIn)
                        }
                        // Rectangle keeps the edges clamped to the picture's own
                        // colour rather than fading to transparent at the foot.
                        .blur(SOFT_FOOT_BLUR, BlurredEdgeTreatment.Rectangle),
                )
            }

            // Ends on the page colour at full strength, so there is no edge
            // left where the artwork stops. Eased rather than run straight: a
            // gradient that changes slope at a stop shows a line at that stop,
            // however close the colours either side of it are.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            0.65f to palette.wash.copy(alpha = 0.30f),
                            0.82f to palette.wash.copy(alpha = 0.72f),
                            0.94f to palette.wash.copy(alpha = 0.95f),
                            1.00f to palette.wash,
                        ),
                    ),
            )
        }
    }
}

/**
 * Where the top of the artwork currently is.
 *
 * The list is the one being scrolled; the background only has to agree with it.
 * While the header is item zero and on screen, how far it has been scrolled off
 * the top is exactly the offset the picture behind it needs. Once it isn't,
 * there is nothing to agree with, and the picture parks two artwork-heights up
 * — far enough that no part of it comes back down.
 */
private fun LazyListState.headerTop(artHeightPx: Float): Float =
    if (firstVisibleItemIndex == 0) -firstVisibleItemScrollOffset.toFloat() else -artHeightPx * 2f

/** Where the blurred copy starts to show and where it has fully taken over. */
private val SOFT_FOOT_MASK = Brush.verticalGradient(
    0.55f to Color.Transparent,
    0.85f to Color.Black,
)

/**
 * Wide enough that no shapes survive where the blurred copy is at full
 * strength — a blur that leaves them reads as a blurred photograph, and a
 * blurred photograph next to a flat colour is still two surfaces.
 */
private val SOFT_FOOT_BLUR = 48.dp

/**
 * Shuffle • Play • Star — the artist page's action row, as Apple Music lays
 * it out: a large Play circle with a small glass circle either side.
 *
 * The star is the page's existing subscribe toggle. Where the page offers none
 * (a guest) it stays, greyed out and untappable, so Play keeps the middle.
 */
@Composable
private fun ActionRow(
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    bottomSpace: Dp = 22.dp,
    /** The artist header's subscribe state, or null where it isn't offered. */
    subscription: SubscriptionState? = null,
    onToggleSubscription: (() -> Unit)? = null,
    /** Apple's fill for the Play circle; null leaves it white. */
    playColor: Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HEADER_GUTTER),
        horizontalArrangement = Arrangement.spacedBy(ARTIST_ACTION_GAP, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            icon = BitChordIcons.Shuffle,
            contentDescription = stringResource(R.string.shuffle),
            onClick = onShuffle,
            haptic = Haptic.Resume,
            size = ARTIST_SIDE_BUTTON,
        )

        PlayCircle(
            onClick = onPlay,
            size = ARTIST_PLAY_BUTTON,
            iconScale = ARTIST_PLAY_ICON_SCALE,
            containerColor = playColor ?: Color.White,
        )

        if (subscription != null) {
            CircleIconButton(
                icon = if (subscription.subscribed) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = stringResource(
                    if (subscription.subscribed) R.string.unsubscribe else R.string.subscribe,
                ),
                onClick = { onToggleSubscription?.invoke() },
                haptic = if (subscription.subscribed) Haptic.ToggleOff else Haptic.ToggleOn,
                size = ARTIST_SIDE_BUTTON,
            )
        } else {
            CircleIconButton(
                icon = Icons.Rounded.StarBorder,
                contentDescription = stringResource(R.string.subscribe),
                onClick = {},
                size = ARTIST_SIDE_BUTTON,
                enabled = false,
            )
        }
    }
    Spacer(Modifier.height(bottomSpace))
}

/**
 * A labelled pill in the release page's action row. [filled] is Play: white
 * with black content, so the primary action survives every palette, filling
 * whatever width the row leaves it. Otherwise it wears the light veil and glass
 * rim of the circles beside it — Shuffle, on a page with no Add to sit opposite.
 */
@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    filled: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
) {
    // Resume rather than a flat tap: both start a queue, and the rising pair
    // says so.
    val haptics = rememberHaptics()
    val content = if (filled) Color.Black else MaterialTheme.colorScheme.onSurface
    // Filled: the icon and label are punched out of the white pill, so the page shows through
    // them, as on the artist page's Play circle. Offscreen, so the cut takes only the pill.
    val punch = if (filled) Modifier.graphicsLayer { blendMode = BlendMode.DstOut } else Modifier
    Row(
        modifier = modifier
            .height(size)
            .clip(CircleShape)
            .then(
                if (filled) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .background(Color.White)
                } else {
                    Modifier
                        .background(Color.White.copy(alpha = LIGHT_FILL_ALPHA), CircleShape)
                        .liquidGlassRim(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = TOP_RELEASE_EDGE_ALPHA), CircleShape)
                },
            )
            .clickable {
                haptics.play(Haptic.Resume)
                onClick()
            }
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(18.dp).then(punch),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = content,
            maxLines = 1,
            modifier = punch,
        )
    }
}

/**
 * The artist page's Play: a filled circle with the triangle punched out of it,
 * so the page shows through, as Apple's does.
 */
@Composable
private fun PlayCircle(
    onClick: () -> Unit,
    size: Dp,
    iconScale: Float,
    containerColor: Color,
) {
    val haptics = rememberHaptics()
    val description = stringResource(R.string.play)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable {
                haptics.play(Haptic.Resume)
                onClick()
            }
            // Offscreen so the clear below cuts the circle drawn under it,
            // not whatever the page has behind.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawCircle(containerColor)
                val box = this.size.minDimension * iconScale
                val unit = box / 24f
                val origin = Offset((this.size.width - box) / 2f, (this.size.height - box) / 2f)
                val triangle = androidx.compose.ui.graphics.Path().apply {
                    moveTo(origin.x + 6.8f * unit, origin.y + 4.8f * unit)
                    lineTo(origin.x + 19.2f * unit, origin.y + 12f * unit)
                    lineTo(origin.x + 6.8f * unit, origin.y + 19.2f * unit)
                    close()
                }
                drawPath(triangle, Color.Black, blendMode = BlendMode.Clear)
                // The same round-joined pen the icon is drawn with.
                drawPath(
                    triangle,
                    Color.Black,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2f * unit,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                    blendMode = BlendMode.Clear,
                )
            }
            .semantics { contentDescription = description },
    )
}

/**
 * Small circular icon-only button — Shuffle and its partner either side of
 * Play, on both the release and the artist page. A light veil with a hairline
 * edge, Apple's look for these, rather than the dark glass.
 */
@Composable
private fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    haptic: Haptic = Haptic.Tap,
    size: Dp = 50.dp,
    /**
     * False greys the button out and stops it taking taps, for an action the
     * page offers but this user can't take — a guest's Add or Subscribe.
     * Dimmed rather than dropped, so the row keeps its shape.
     */
    enabled: Boolean = true,
) {
    val haptics = rememberHaptics()
    Box(
        modifier = Modifier
            .size(size)
            .alpha(if (enabled) 1f else DISABLED_ACTION_ALPHA)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = LIGHT_FILL_ALPHA), CircleShape)
            // The glass rim the floating bars wear, with Liquid Glass on. Rim
            // only: over the page's flat tint a blurred fill would look the same.
            .liquidGlassRim(CircleShape)
            .border(1.dp, Color.White.copy(alpha = TOP_RELEASE_EDGE_ALPHA), CircleShape)
            .clickable(enabled = enabled) {
                haptics.play(haptic)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

/** "1.2M subscribers" and "3.4M monthly listeners", off the artist header. */
@Composable
private fun ArtistStatsRow(
    subscriberCountText: String?,
    monthlyListenerCount: String?,
    palette: ArtworkPalette,
) {
    // Stacked and left-aligned with the text under it, since this now sits in
    // the About section rather than across the header.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ARTIST_CONTENT_GUTTER, end = ARTIST_CONTENT_GUTTER, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // YouTube's own count text already reads "1.2M subscribers" in full,
        // so only the number is kept and the label re-said in the app's own
        // words — the one way to fit both stats on one line on a narrow
        // screen without either wrapping into two.
        subscriberCountText?.let {
            StatChip(
                icon = Icons.Rounded.Person,
                text = stringResource(R.string.subscribers, it.substringBefore(' ')),
                palette = palette,
            )
        }
        monthlyListenerCount?.let {
            StatChip(
                icon = Icons.Rounded.GraphicEq,
                text = stringResource(R.string.monthly_listeners, it.substringBefore(' ')),
                palette = palette,
            )
        }
    }
}

@Composable
private fun StatChip(icon: ImageVector, text: String, palette: ArtworkPalette) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .lightweightLiquidGlass(
                shape = CircleShape,
                fallbackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.onBackgroundVariant,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = palette.onBackgroundVariant,
        )
    }
}

/**
 * YouTube's own editorial note for a release or an artist, collapsed to a
 * few lines with a tap to read the rest — the same "About" block Apple
 * Music and YouTube Music itself show under the header.
 *
 * Whether there's anything to expand is only knowable once the text has
 * been laid out at the collapsed line count, so the "More" toggle is held
 * back until that measurement says the clipped text actually lost
 * something — otherwise a two-line bio would show a toggle with nothing
 * behind it to reveal.
 */
/** "Signed by **Name**" — the artist's name in bold, whatever order the language puts it in. */
@Composable
private fun SignedByCaption(
    name: String,
    palette: ArtworkPalette,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    val signed = stringResource(R.string.signed_by, name)
    val nameAt = signed.indexOf(name)
    Text(
        text = buildAnnotatedString {
            append(signed)
            if (nameAt >= 0) {
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), nameAt, nameAt + name.length)
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        color = palette.onBackground.copy(alpha = 0.8f),
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * The handwritten signature with its signed-by line, closing the About section of an artist whose
 * header already carries Apple's title logo.
 */
@Composable
private fun AboutSignature(
    signature: ArtistSignature,
    name: String,
    palette: ArtworkPalette,
    horizontalPadding: Dp,
) {
    val context = LocalPlatformContext.current
    val isSvg = signature.format == "svg"
    val aspect = signature.aspect.coerceIn(0.4f, 6f)
    val request = remember(signature.imageUrl) {
        ImageRequest.Builder(context).data(signature.imageUrl).apply {
            if (isSvg) {
                size(SVG_DECODE_WIDTH, (SVG_DECODE_WIDTH / aspect).roundToInt())
            } else {
                size(coil3.size.Size.ORIGINAL)
                transformations(SignatureInkTransformation)
            }
        }.build()
    }
    var loaded by remember(signature.imageUrl) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
        horizontalAlignment = Alignment.Start,
    ) {
        AsyncImage(
            model = request,
            contentDescription = name,
            contentScale = ContentScale.Fit,
            colorFilter = if (isSvg) ColorFilter.tint(Color.White) else null,
            onState = { if (it is AsyncImagePainter.State.Success) loaded = true },
            modifier = Modifier
                .padding(top = 20.dp, bottom = 6.dp)
                .height(ABOUT_LOGO_HEIGHT)
                .aspectRatio(aspect),
        )
        if (loaded) SignedByCaption(name = name, palette = palette, textAlign = TextAlign.Start)
    }
}

@Composable
private fun AboutSection(
    title: String,
    text: String?,
    palette: ArtworkPalette,
    horizontalPadding: Dp = ABOUT_GUTTER,
    /** Drawn under the text. */
    stats: (@Composable () -> Unit)? = null,
    /** Drawn last, under the stats. */
    footer: (@Composable () -> Unit)? = null,
) {
    var expanded by remember(text) { mutableStateOf(false) }
    var clipped by remember(text) { mutableStateOf(false) }
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.onBackground,
            modifier = Modifier.padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 2.dp,
                bottom = 6.dp,
            ),
        )
        if (text != null) Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = palette.onBackgroundVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> if (!expanded) clipped = result.hasVisualOverflow },
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = horizontalPadding)
                .let { m -> if (clipped || expanded) m.clickable { expanded = !expanded } else m },
        )
        if (clipped || expanded) {
            Text(
                text = stringResource(if (expanded) R.string.less else R.string.more),
                style = MaterialTheme.typography.labelLarge,
                color = palette.accent,
                modifier = Modifier
                    .padding(horizontal = horizontalPadding, vertical = 4.dp)
                    .clickable { expanded = !expanded },
            )
        }
        if (stats != null) {
            Spacer(Modifier.height(if (text != null) 12.dp else 4.dp))
            stats()
        }
        footer?.invoke()
    }
}

@Composable
private fun SectionHeading(
    title: String,
    palette: ArtworkPalette,
    onShowAll: (() -> Unit)? = null,
    horizontalPadding: Dp = PAGE_GUTTER,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 10.dp,
                bottom = 8.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.onBackground,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (onShowAll != null) {
            Text(
                text = stringResource(R.string.show_all),
                style = MaterialTheme.typography.titleSmall,
                color = palette.accent,
                modifier = Modifier
                    .clickable(onClick = onShowAll)
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
}

/** Compact row used inside the artist song grid; no swipe, to keep the
 *  horizontal pager's gestures unambiguous. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactSongRow(
    song: Song,
    palette: ArtworkPalette,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    downloadedTint: Color? = null,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.trackArtSize(48.dp, song.isVideo)) {
            AsyncImage(
                model = song.artworkAt(ROW_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(7.dp))
                    .background(palette.elevated),
            )
            if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            ExplicitSongTitle(
                song = song,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) palette.accent else palette.onBackground,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.onBackgroundVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (downloadedTint != null) {
            DownloadedBadge(song.videoId, downloadedTint)
        }
        RowMoreButton(onClick = onLongPress, tint = palette.onBackgroundVariant)
    }
}

/**
 * A row under "Suggested" — a track YouTube offers to round the playlist
 * out but that was never added. [onAdd] is the point of the row, so it gets
 * the trailing spot a track already on the playlist spends on "more"; the
 * long-press sheet is still one gesture away for anything else.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestedSongRow(
    song: Song,
    palette: ArtworkPalette,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onAdd: () -> Unit,
    downloadedTint: Color? = null,
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    addIcon: ImageVector = Icons.Rounded.Add,
    addLabel: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .longPressMenuClickable(
                onClick = onClick,
                onLongClick = onLongPress,
                liftPadding = PaddingValues(horizontal = PAGE_GUTTER),
            )
            .padding(horizontal = PAGE_GUTTER, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.trackArtSize(52.dp, song.isVideo)) {
            AsyncImage(
                model = song.artworkAt(ROW_ART_PX),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.elevated),
            )
            if (isCurrent && isPlaying) SearchPlayingBars(Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            ExplicitSongTitle(
                song = song,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) palette.accent else palette.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.onBackgroundVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (downloadedTint != null) {
            DownloadedBadge(song.videoId, downloadedTint)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(palette.accent.copy(alpha = 0.16f))
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                addIcon,
                contentDescription = addLabel ?: stringResource(R.string.add_to_playlist),
                tint = palette.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The newest release on an artist's shelves — an album, or a single or EP that
 * came out after it. Playlists, videos and related artists share these shelves
 * and are skipped: only an `MPRE…` browse id is a release.
 *
 * Newest by the year in the card's subtitle, which is all a shelf item says
 * about when. Ties and releases with no year keep YouTube's own order, which
 * lists albums first.
 */
private fun List<HomeShelf>.topRelease(): ShelfItem? =
    asSequence()
        .flatMap { it.items.asSequence() }
        .filter { it.browseId?.startsWith("MPRE") == true }
        .withIndex()
        .maxWithOrNull(
            compareBy<IndexedValue<ShelfItem>> { it.value.releaseYear() ?: 0 }
                .thenByDescending { it.index },
        )?.value

private val RELEASE_YEAR = Regex("""\b(19|20)\d{2}\b""")

private fun ShelfItem.releaseYear(): Int? =
    RELEASE_YEAR.find(subtitle)?.value?.toIntOrNull()

/** "Recent Single" and "Recent EP" where the card says so, otherwise "Recent Album". */
@androidx.annotation.StringRes
private fun ShelfItem.recentLabel(): Int {
    val kind = subtitle.lowercase()
    return when {
        kind.contains("single") -> R.string.recent_single
        Regex("""\bep\b""").containsMatchIn(kind) -> R.string.recent_ep
        else -> R.string.recent_album
    }
}

/**
 * The artist page's top-release card: sleeve, what it is and when, and its
 * title, in a squircle panel the width of the page.
 *
 * Proportioned off Apple Music's: a 24pt continuous corner, a 96pt sleeve 12pt
 * in from the edge, the text 12pt after it, and a 32pt add button 19pt in
 * from the end. The panel and its button are a light veil with a light
 * hairline, as Apple's containers read slightly white against the page.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopReleaseCard(
    item: ShelfItem,
    palette: ArtworkPalette,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    /** Whether the album is in the library; null while that is still being read. */
    saved: Boolean?,
    /** Null hides the button. */
    onToggleSaved: (() -> Unit)?,
    /** The line under the title; "Recent Album/EP/Single" when null. */
    label: String? = null,
) {
    val shape = remember { RoundedCornerShape(TOP_RELEASE_CORNER).continuous() }
    // Concentric with the panel: its corner less the inset the sleeve sits at.
    val coverShape = remember { RoundedCornerShape(TOP_RELEASE_CORNER - TOP_RELEASE_INSET).continuous() }
    val haptics = rememberHaptics()
    val hasButton = onToggleSaved != null && saved != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ARTIST_CONTENT_GUTTER)
            .padding(bottom = 22.dp)
            // A light veil rather than the dark glass: Apple's containers read
            // slightly white against the page, and a dark panel reads as a hole.
            .clip(shape)
            .background(Color.White.copy(alpha = LIGHT_FILL_ALPHA), shape)
            .border(1.dp, Color.White.copy(alpha = TOP_RELEASE_EDGE_ALPHA), shape)
            .clip(shape)
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(
                start = TOP_RELEASE_INSET,
                top = TOP_RELEASE_INSET,
                bottom = TOP_RELEASE_INSET,
                end = if (hasButton) 19.dp else TOP_RELEASE_INSET,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(coverShape)
                .border(1.dp, Color.White.copy(alpha = TOP_RELEASE_COVER_EDGE_ALPHA), coverShape)
                .background(palette.elevated),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // The lines either side of the title in the player's secondary
            // treatment, the artist line's: the page's own colour, partly
            // transparent.
            val secondary = palette.onBackground.copy(alpha = TOP_RELEASE_SECONDARY_ALPHA)
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Faded out at the end of its last line, as the player's title is,
            // and set a little tighter than the scale for when it takes two.
            FadingText(
                text = item.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    lineHeight = TOP_RELEASE_TITLE_LINE_HEIGHT,
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
                ),
                color = palette.onBackground,
                maxLines = 2,
            )
            Text(
                text = label ?: stringResource(item.recentLabel()),
                style = MaterialTheme.typography.bodyMedium,
                color = secondary,
                maxLines = 1,
            )
        }
        if (onToggleSaved != null && saved != null) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = LIGHT_FILL_ALPHA), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = TOP_RELEASE_EDGE_ALPHA), CircleShape)
                    .clickable {
                        haptics.play(if (saved) Haptic.ToggleOff else Haptic.ToggleOn)
                        onToggleSaved()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (saved) Icons.Rounded.Check else Icons.Rounded.Add,
                    contentDescription = stringResource(
                        if (saved) R.string.remove_from_library else R.string.add_to_library,
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

/** The top-release panel's continuous corner, and the inset its sleeve sits at. */
private val TOP_RELEASE_CORNER = 24.dp
private val TOP_RELEASE_INSET = 12.dp

/** The panel's secondary lines, at the player's artist line's strength. */
private const val TOP_RELEASE_SECONDARY_ALPHA = 0.55f

/** The panel's title on two lines: a little under titleLarge's natural 24sp. */
private val TOP_RELEASE_TITLE_LINE_HEIGHT = 22.sp

/** The white veil under Apple-style containers and their buttons. */
private const val LIGHT_FILL_ALPHA = 0.07f
/** A side button this user can't use — a guest's Add or Subscribe. */
private const val DISABLED_ACTION_ALPHA = 0.38f

/** Brighter than the default 0.15 hairlines, as on Apple Music's own card. */
private const val TOP_RELEASE_EDGE_ALPHA = 0.10f
private const val TOP_RELEASE_COVER_EDGE_ALPHA = 0.215f

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionCard(
    item: ShelfItem,
    palette: ArtworkPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(SHELF_CARD_WIDTH),
    onLongPress: (() -> Unit)? = null,
) {
    // An artist's Videos shelf: widescreen, as on the web player.
    if (item.isVideo) {
        VideoShelfCard(
            item = item,
            onClick = onClick,
            onLongPress = onLongPress,
            modifier = modifier,
            titleColor = palette.onBackground,
            subtitleColor = palette.onBackgroundVariant,
            placeholderColor = palette.elevated,
        )
        return
    }
    Column(
        modifier = modifier
            .longPressMenuClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        AsyncImage(
            model = item.thumbnailUrl.artworkAt(CARD_ART_PX),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.elevated),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.onBackgroundVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ArtistShelfGridPage(
    shelf: HomeShelf,
    palette: ArtworkPalette,
    onItemClick: (ShelfItem) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    BoxWithConstraints(modifier.fillMaxSize().background(palette.background)) {
        val grid = libraryGrid(maxWidth - PAGE_GUTTER * 2)
        LazyVerticalGrid(
            columns = GridCells.Fixed(grid.columns),
            state = gridState,
            contentPadding = PaddingValues(
                top = topBarContentPadding(),
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(LIBRARY_GRID_SPACING),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = PAGE_GUTTER),
        ) {
            items(shelf.items, key = { it.browseId ?: it.videoId ?: it.title }) { item ->
                SectionCard(
                    item = item,
                    palette = palette,
                    onClick = { onItemClick(item) },
                    onLongPress = onItemLongPress?.let { { it(item) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Splits the one subtitle a browse row hands over — "Album • Travis Scott •
 * 2023", or sometimes just "Travis Scott" — into the credit line and the
 * metadata line the header shows separately.
 *
 * Everything is optional, because every caller supplies a different amount of
 * it: the player knows an album's artist but not its year, search knows both,
 * and a home card frequently knows neither.
 */
@Composable
private fun DetailPage.headerLines(trackCount: Int, playtime: String? = null): Pair<String, String> {
    val parts = subtitle.split("•", "·").map { it.trim() }.filter { it.isNotEmpty() }
    val year = parts.lastOrNull { it.length == 4 && it.all(Char::isDigit) }
    val kind = parts.firstOrNull { it.lowercase(Locale.ROOT) in KIND_WORDS }
    val credit = parts.filter { it != year && it != kind }.joinToString(", ")
    // Title case, "Playlist · 2022 · 10 Tracks · 38 min" — except the
    // playtime, whose "min" and "1h 25m" are units, not words.
    // An artist grouped out of a device folder is a plain track list, not an
    // artist page, but it is still billed as the artist it is.
    // A device directory plays like a playlist, but it is billed as a folder.
    val localKind = when {
        LocalGroups.isArtist(browseId) -> stringResource(R.string.artist)
        LocalGroups.isDirectory(browseId) -> stringResource(R.string.folder)
        else -> null
    }
    val meta = listOfNotNull(
        (kind ?: localKind ?: type.localizedLabel())?.capitalizeWords(),
        year,
        trackCount.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.track_count_plural, it, it).capitalizeWords()
        },
        playtime,
    ).joinToString(" · ")
    return credit to meta
}

/** The glyph on a release page's placeholder sleeve: what kind of thing has no cover. */
private fun DetailPage.placeholderIcon(): ImageVector = deviceFolderIcon(browseId) ?: when {
    LocalGroups.isArtist(browseId) -> Icons.Rounded.Person
    OfflinePlaylists.isLikedPage(browseId) -> Icons.Rounded.Favorite
    LocalGroups.isDirectory(browseId) -> Icons.Rounded.Folder
    type == BrowseType.ALBUM -> Icons.Rounded.Album
    type == BrowseType.PLAYLIST -> Icons.AutoMirrored.Rounded.QueueMusic
    else -> BitChordIcons.MusicNote
}

/** "10 tracks" → "10 Tracks", "playlist" → "Playlist". */
private fun String.capitalizeWords(): String =
    split(' ').joinToString(" ") { word ->
        word.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

/** Subtitle words that name what a page *is* rather than who made it. */
private val KIND_WORDS = setOf(
    "album", "single", "ep", "playlist", "artist", "podcast", "episode", "song", "video",
)

@Composable
private fun BrowseType.localizedLabel(): String? = when (this) {
        BrowseType.ALBUM -> stringResource(R.string.album)
        BrowseType.PLAYLIST -> stringResource(R.string.playlist)
        BrowseType.ARTIST -> stringResource(R.string.artist)
        BrowseType.OTHER -> null
    }

/**
 * How long the page plays for — "41 min", "1h 25m" — summed over the rows
 * on it, or null when none of them carry a duration. A playlist still filling
 * in counts up with it, so the figure is never ahead of the list it sits over.
 */
@Composable
private fun List<Song>.playtime(): String? {
    val minutes = sumOf { it.durationMillis() } / 60_000
    return when {
        minutes <= 0 -> null
        minutes < 60 -> stringResource(R.string.minutes_short, minutes.toInt())
        else -> stringResource(R.string.hours_minutes_short, (minutes / 60).toInt(), (minutes % 60).toInt())
    }
}


/**
 * Glaze's album blurb: two lines with MORE at the end; a tap opens the rest in
 * place. A hairline under it separates the header from the tracks.
 */
@Composable
private fun AlbumDescription(text: String, palette: ArtworkPalette) {
    var open by remember(text) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = if (open) text else text.replace("\n\n", " "),
                modifier = Modifier.weight(1f),
                maxLines = if (open) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                color = palette.onBackgroundVariant,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 18.sp),
            )
            if (!open) {
                Text(
                    text = stringResource(R.string.more_caps),
                    modifier = Modifier.padding(start = 4.dp),
                    color = palette.onBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Glaze's full-width hairline between an album's sections. */
@Composable
private fun AlbumDivider(palette: ArtworkPalette, start: Dp = ALBUM_DIVIDER_INSET) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = start, end = ALBUM_DIVIDER_INSET)
            .height(1.dp)
            .background(palette.divider),
    )
}

/** Glaze's inset for an album's hairlines, at both ends; track separators end here too. */
private val ALBUM_DIVIDER_INSET = 17.dp

/** Space on each side of the line before a playlist's recommended songs. */
private val RECS_SEPARATOR_GAP = 20.dp

/** Arpeggi's darker floor under an album's footer or a playlist's recommendations. */
private val BOTTOM_SECTION_SHADE = Color.Black.copy(alpha = 0.22f)

/** Drawn by the section's own items so it moves (and stretches) with them. */
private fun Modifier.bottomSectionShade() = drawBehind { drawRect(BOTTOM_SECTION_SHADE) }

/** Carries the shade from the last item down past the bottom of the screen. */
@Composable
private fun BottomSectionTail() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind { drawRect(BOTTOM_SECTION_SHADE, size = Size(size.width, size.height + 10_000f)) },
    )
}

/** Under an album's tracks, as Apple Music closes it: the release date, then the count and length. */
@Composable
private fun AlbumFooter(
    releaseDate: String?,
    songCount: Int,
    playtime: String?,
    palette: ArtworkPalette,
    recordLabel: String? = null,
) {
    val date = remember(releaseDate) {
        releaseDate?.let {
            runCatching {
                java.time.LocalDate.parse(it).format(
                    java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG),
                )
            }.getOrNull()
        }
    }
    Column(Modifier.fillMaxWidth()) {
        AlbumDivider(palette)
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = ALBUM_FOOTER_GAP)) {
            if (date != null) {
                Text(date, color = palette.onBackground, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(3.dp))
            }
            Text(
                text = listOfNotNull(
                    pluralStringResource(R.plurals.song_count_plural, songCount, songCount),
                    playtime,
                ).joinToString(" · "),
                color = palette.onBackgroundVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (recordLabel != null) {
                Text(recordLabel, color = palette.onBackgroundVariant, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/**
 * "More from <artist>" (their other releases) and "Similar albums" (the first
 * release of each artist YouTube Music lists as similar), from artist pages.
 */
private suspend fun albumRecommendations(
    albumId: String,
    artistId: String,
    moreFromTitle: String,
    similarTitle: String,
    albumTitle: String,
): List<HomeShelf> = withContext(Dispatchers.IO) {
    fun ShelfItem.isRelease() = browseId?.startsWith("MPREb") == true
    val items = YtMusicRepository.artistLanding(artistId).getOrNull()?.sections?.flatMap { it.items }
        ?: return@withContext emptyList()
    // By title too: a server's album has its own id, and isn't "more from" itself.
    val releases = items.filter { it.isRelease() && it.browseId != albumId && !it.title.equals(albumTitle, ignoreCase = true) }
        .distinctBy { it.browseId }.take(12)
    val similarArtists = items.filter { it.browseId?.startsWith("UC") == true && it.browseId != artistId }
        .distinctBy { it.browseId }.take(8)
    val similar = coroutineScope {
        similarArtists.map { artist ->
            async {
                YtMusicRepository.artistLanding(artist.browseId!!).getOrNull()?.sections
                    ?.flatMap { it.items }?.firstOrNull { it.isRelease() }
                    ?.copy(subtitle = artist.title)
            }
        }.awaitAll().filterNotNull()
    }
    listOfNotNull(
        HomeShelf(moreFromTitle, releases).takeIf { releases.isNotEmpty() },
        HomeShelf(similarTitle, similar).takeIf { similar.isNotEmpty() },
    )
}


/** YouTube Music's artist called [name] exactly (ignoring case), for pages that only have the name. */
private suspend fun youtubeArtistId(name: String): String? = withContext(Dispatchers.IO) {
    YtMusicRepository.search(name, com.music.bitchord.data.model.SearchFilter.ARTISTS).getOrNull()
        ?.filterIsInstance<com.music.bitchord.data.model.SearchResult.Browse>()
        ?.map { it.item }
        ?.firstOrNull { it.title.equals(name.trim(), ignoreCase = true) }
        ?.browseId?.takeIf { it.startsWith("UC") }
}

/**
 * The first name of a joint credit ("Travis Scott & Grand Theft Auto VI"), or
 * null for a single artist. Catalogues often file a joint release under its
 * lead artist alone, so a lookup that needs every credited name to match retries with this.
 */
private fun primaryArtist(credit: String): String? =
    credit.split(" & ", ", ", " x ", " X ", " feat. ", " ft. ")
        .first().trim().takeIf { it.isNotEmpty() && it != credit.trim() }


/**
 * Apple's catalogue entry for an album page (genre, date, lossless, cover
 * colour). Shared by the page and MainActivity's bars, so both tint alike;
 * [AppleMusicCanvas.albumInfo] caches, so the second caller costs nothing.
 */
@Composable
internal fun rememberAlbumInfo(page: DetailPage?): AppleMusicCanvas.AlbumInfo? {
    val album = page?.takeIf { it.type == BrowseType.ALBUM }
    val credit = album?.let { p ->
        p.headerLines(0).first.ifBlank { (p.songs as? UiState.Success)?.data?.firstOrNull()?.artist.orEmpty() }
    }.orEmpty()
    var info by remember(album?.browseId) { mutableStateOf<AppleMusicCanvas.AlbumInfo?>(null) }
    LaunchedEffect(album?.browseId, album?.title, credit) {
        if (album == null || credit.isBlank()) return@LaunchedEffect
        info = withContext(Dispatchers.IO) {
            runCatching {
                AppleMusicCanvas.albumInfo(album.title, credit)
                    ?: primaryArtist(credit)?.let { AppleMusicCanvas.albumInfo(album.title, it) }
            }.getOrNull()
        } ?: info
    }
    return info
}

/** Apple's cover colour as the palette's key colour, the way Apple Music tints the page. */
internal fun AppleMusicCanvas.AlbumInfo.keyColors(): ArtworkKeyColors? =
    bgColor?.let { Color(0xFF000000.toInt() or it) }?.let { ArtworkKeyColors(background = it, accent = it) }


/**
 * The page's colours for the chosen [CollectionBackground]: Apple's cover colour,
 * the artwork-estimated single tint (blurred pages, whose foot it approximates),
 * or BitChord's classic palette. Shared with MainActivity's bars so both agree.
 */
@Composable
internal fun rememberPagePalette(
    page: DetailPage?,
    imageUrl: String?,
    artistKeys: ArtworkKeyColors?,
    albumInfo: AppleMusicCanvas.AlbumInfo?,
): ArtworkPalette {
    val style by AppSettings.collectionBackground.collectAsStateWithLifecycle()
    val release = page != null && page.type != BrowseType.ARTIST
    return rememberArtworkPalette(
        imageUrl = imageUrl,
        keyColors = artistKeys ?: albumInfo?.keyColors()?.takeIf { style == CollectionBackground.APPLE },
        collection = release && style != CollectionBackground.CLASSIC,
    )
}

/** Up to ten songs from YouTube Music radios of three of [songs], minus what is already there. */
private suspend fun playlistRecommendations(songs: List<Song>): List<Song> = withContext(Dispatchers.IO) {
    val have = songs.mapTo(HashSet()) { it.videoId }
    coroutineScope {
        songs.shuffled().take(3).map { seed ->
            async { YtMusicRepository.radio(seed.videoId).getOrNull().orEmpty() }
        }.awaitAll()
    }.flatten().filter { it.videoId !in have }.distinctBy { it.videoId }.take(10)
}

/**
 * Spotify's countdown on an upcoming album: days, hours, minutes and seconds to
 * release day, ticking. The time of day is never announced, so it counts to local
 * midnight, and says so — as Glaze does.
 */
@Composable
private fun ReleaseCountdown(release: com.music.bitchord.data.sources.GlazeCompanion.Release, palette: ArtworkPalette) {
    val target = remember(release.releaseDate) {
        runCatching {
            java.time.LocalDate.parse(release.releaseDate.take(10))
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
    }
    var now by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(target) {
        while (true) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000)
        }
    }
    val left = ((target ?: 0L) - now).coerceAtLeast(0L) / 1000
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 12.dp, bottom = 26.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(palette.onBackground.copy(alpha = 0.08f))
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (target == null || left == 0L) {
            Text(
                stringResource(if (target == null) R.string.release_coming_soon else R.string.release_out_today),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = palette.onBackground,
            )
        } else {
            // Units split by hairlines the height of the digits and their labels.
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(
                    left / 86_400 to R.string.countdown_days,
                    left % 86_400 / 3_600 to R.string.countdown_hours,
                    left % 3_600 / 60 to R.string.countdown_minutes,
                    left % 60 to R.string.countdown_seconds,
                ).forEachIndexed { index, (value, label) ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .padding(vertical = 4.dp)
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(palette.onBackground.copy(alpha = 0.22f)),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "%02d".format(value),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.onBackground,
                        )
                        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = palette.onBackgroundVariant)
                    }
                }
            }
        }
        Text(
            stringResource(if (release.saved) R.string.release_presaved_note else R.string.release_presave_hint),
            style = MaterialTheme.typography.bodySmall,
            color = palette.onBackgroundVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
        )
    }
}

/**
 * Which disc each of [songs] is on, by video id, from Apple's track list [apple]
 * (name, disc): by position when the two lists are the same length, else by title.
 * Empty for a single-disc album.
 */
internal fun discNumbers(songs: List<Song>, apple: List<Pair<String, Int>>): Map<String, Int> {
    if (apple.none { it.second > 1 } || songs.isEmpty()) return emptyMap()
    if (apple.size == songs.size) return songs.withIndex().associate { (i, song) -> song.videoId to apple[i].second }
    fun norm(s: String) = s.lowercase(Locale.ROOT).replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "").trim()
    val byName = apple.groupBy({ norm(it.first) }, { it.second })
    var last = 1
    return songs.associate { song ->
        last = byName[norm(song.title)]?.firstOrNull() ?: last
        song.videoId to last
    }
}

/** Glaze's disc divider: a disc glyph and "Disc 2". */
@Composable
private fun DiscHeading(disc: Int, palette: ArtworkPalette) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = PAGE_GUTTER, end = PAGE_GUTTER, top = 18.dp, bottom = 6.dp),
    ) {
        Icon(Icons.Rounded.Album, contentDescription = null, tint = palette.onBackgroundVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.disc_number, disc),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = palette.onBackgroundVariant,
        )
    }
}

/**
 * The best quality any active source offers for [songs], judged on the first two tracks the way a
 * source row is labelled: Hi-Res / Lossless / High-Quality, or null when no source has them.
 */
private suspend fun albumSourceQuality(songs: List<Song>): SourceQuality? {
    val sample = songs.filterNot { it.videoId.startsWith(com.music.bitchord.data.model.SPOTIFY_MISSING_PREFIX) }.take(2)
    if (sample.isEmpty()) return null
    val rank = mapOf(SourceQuality.HIGH_QUALITY to 1, SourceQuality.LOSSLESS to 2, SourceQuality.HI_RES_LOSSLESS to 3)
    var best: SourceQuality? = null
    for (source in SourceRegistry.activeForPlayback()) {
        for (song in sample) {
            val target = com.music.bitchord.data.sources.TrackMatcher.targetOf(song)
            val found = runCatching {
                // Plain title and lead artist: a server's search wants every word, and "(feat. …)" or a
                // second credit rarely matches its tags word for word.
                val title = song.title.replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "").trim().ifEmpty { song.title }
                source.search("$title ${song.artist.split(",", " & ").first().trim()}", 5, false, null, target)
            }.getOrNull().orEmpty()
            val quality = com.music.bitchord.data.sources.TrackMatcher.best(found, target)?.sourceQualityBadge() ?: continue
            if ((rank[quality] ?: 0) > (rank[best] ?: 0)) best = quality
        }
    }
    return best
}
