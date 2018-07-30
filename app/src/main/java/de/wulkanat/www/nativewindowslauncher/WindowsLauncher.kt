package de.wulkanat.www.nativewindowslauncher

import java.nio.ByteBuffer
import java.nio.ByteOrder

class WindowsLauncher(val parent: GLRenderer) {
    companion object {
        val enterDuration = 0.5
        val enterRowZoomDifference = 0.075f
        val initalZoom = 0.6f
        val fadeInEnd = 0.05f
        val fadeInStart = 0.15f

        val fadeInNum1 = 0.0
        val fadeInNum2 = 0.6
        val fadeInNum3 = 7.5
        val fadeInLoc = doubleArrayOf(0.8, 0.2)


        val overscrollNum1 = 0.0
        val overscrollNum2 = 1.0
        val overscrollNum3 = 0.0
        val overscrollLoc = doubleArrayOf(0.71, 0.25)

        val exitDuration = 0.36
        val targetZoom = 3.0f
        val fadeOutDist = 0.5f
        val fadeOutStart = 0.15f

        val appTileOffset = 2.0f
        val fadeOutNum1 = 0.0
        val fadeOutNum2 = 1.1
        val fadeOutNum3 = 1.5
        val fadeOutLoc = doubleArrayOf(0.43, 0.29)
    }

    var fadeInInterpolator = AccExpInterpolator(fadeInNum1, fadeInNum2, fadeInNum3, fadeInLoc)
    var fadeOutInterpolator = AccExpInterpolator(fadeOutNum1, fadeOutNum2, fadeOutNum3, fadeOutLoc)
    var overscrollInterpolator = AccExpInterpolator(overscrollNum1, overscrollNum2, overscrollNum3, overscrollLoc)

    var tiles = ArrayList<Tile>()

    var gridWidth: Int = 6
    var leftSideMargin: Float = 0.0104167f * 2//0.01041 6 Period
    var rightSideMargin: Float = 0.0118056f * 2//0.01180 5 Period
    var tilesMargin: Float = 0.0097222f * 2//0.0097 2 Period
    var topMargin: Float = tilesMargin
    var statusBarHeightPercentage: Float = 0.0f
    var statusBarHeight: Float = 0.0f
    set (value) {
        field = value
        overscrollDist = 0.2090278f * 2 - topMargin - value
    }
    var navBarHeightPercentage: Float = 0.0f
    var navBarHeight: Float = 0.0f
    var overscrollDist: Float = 0.2090278f * 2 - topMargin //0,20902 / Period
    val overscrollDuration = 0.25
    var todoOverscrollDist = 0.0f
    var overscrollElapsed = 0.0

    var gridOverscrollHeight = 0.0f
    var gridHeight = 0.0f
    set (value) {
        field = value
        gridOverscrollHeight = (value - (glGrid[3] - glGrid[2] - statusBarHeight - navBarHeight)) * -1
    }
    var gridHeightSpan = 0

    var scrollDist = 0.0f

    var tileSizeCache = 0.0f
    var tileXPosChache = FloatArray(gridWidth)
    var tileAndMarginCache = 0.0f

    var glGrid: FloatArray = FloatArray(4)

    val inds = shortArrayOf(0, 1, 2, 0, 2, 3)

    init {
        addTile(Tile(0, 0, 1, 1))
        addTile(Tile(1, 0, 1, 1))
        addTile(Tile(0, 1, 1, 1))
        addTile(Tile(1, 1, 1, 1))
        addTile(Tile(2, 0, 2, 2))
        addTile(Tile(4, 0, 2, 2))
        addTile(Tile(0, 2, 2, 2))
        addTile(Tile(2, 2, 4, 2))
        addTile(Tile(0, 4, 2, 2))
        addTile(Tile(2, 4, 1, 1))
        addTile(Tile(3, 4, 1, 1))
        addTile(Tile(2, 5, 1, 1))
        addTile(Tile(3, 5, 1, 1))
        addTile(Tile(4, 4, 2, 2))
        addTile(Tile(0, 6, 4, 2))
        addTile(Tile(4, 6, 2, 2))
        addTile(Tile(0, 8, 2, 2))
        addTile(Tile(2, 8, 2, 2))
        addTile(Tile(4, 8, 1, 1))
        addTile(Tile(5, 8, 1, 1))
        addTile(Tile(4, 9, 1, 1))
        addTile(Tile(5, 9, 1, 1))
        addTile(Tile(0, 10, 2, 2))
        addTile(Tile(2, 10, 4, 2))
        addTile(Tile(0, 12, 1, 1))
        addTile(Tile(1, 12, 1, 1))
        addTile(Tile(2, 12, 2, 2))
        addTile(Tile(4, 12, 2, 2))
        addTile(Tile(0, 13, 2, 2))
        addTile(Tile(0, 15, 1, 1))
        addTile(Tile(1, 15, 1, 1))
        addTile(Tile(2, 14, 2, 2))
        addTile(Tile(4, 14, 1, 1))
        addTile(Tile(5, 14, 1, 1))
        addTile(Tile(4, 15, 1, 1))
        addTile(Tile(5, 15, 1, 1))
        addTile(Tile(0, 16, 2, 2))
        addTile(Tile(2, 16, 4, 2))
    }

    fun addTile(tile: Tile) {
        if (tile.posY + tile.spanY > gridHeightSpan) {
            gridHeightSpan = tile.posY + tile.spanY
            gridHeight = gridHeightSpan.toFloat() * tileAndMarginCache
        }
        tiles.add(tile)
    }

    fun performEnterAnimation(progress: Double) {
        TODO("Not implemented")
    }

    fun update(elapsed: Double) {
        handleTouch(elapsed)

        for (tile in tiles) {
            //TODO: calc in seperate Thread
            calculateTile(tile, 1.0f)
        }

        //TODO: multithreading, check for GPU finished

        for (tile in tiles) {
            //Swap buffers in tiles
            tile.renderDrawListBuffer = tile.drawListBuffer
            tile.renderVertBuffer = tile.vertBuffer
            tile.renderColorBuffer = tile.colorBuffer
        }
    }

    fun handleTouch(elapsed: Double) {
        if ((scrollDist >= overscrollDist) || (scrollDist <= gridOverscrollHeight - overscrollDist)) {
            parent.yVelocityTouch = 0.0f
            parent.dyTouch = 0.0f
        } else if (scrollDist > 0) {
            scroll(elapsed, 6.0f)
            if (parent.fingerDown) {
                todoOverscrollDist = scrollDist
                overscrollElapsed = overscrollDuration
            }
        } else if (scrollDist < gridOverscrollHeight) {
            scroll(elapsed, 6.0f)
            if (parent.fingerDown) {
                todoOverscrollDist = scrollDist - gridOverscrollHeight
                overscrollElapsed = overscrollDuration
            }
        } else {
            scroll(elapsed, 1.0f)
        }

        if (!parent.fingerDown) {
            if (scrollDist > 0.0f && parent.yVelocityTouch > -1.0) {
                overscrollEffect(true, elapsed)
            } else if (scrollDist < gridOverscrollHeight && parent.yVelocityTouch < 1.0) {
                overscrollEffect(false, elapsed)
            } else {
                if (scrollDist > 0 || scrollDist < gridOverscrollHeight)
                    parent.yVelocityTouch -= elapsed.toFloat() * (parent.yVelocityTouch / 0.04f)
                else
                    parent.yVelocityTouch -= elapsed.toFloat() * (parent.yVelocityTouch / 0.4f)

                if (scrollDist > gridOverscrollHeight / 2)
                    todoOverscrollDist = scrollDist
                else
                    todoOverscrollDist = scrollDist - gridOverscrollHeight
                overscrollElapsed = overscrollDuration
            }
        }
    }

    fun scroll(elapsed: Double, divideBy: Float) {
        if (parent.fingerDown) {
            scrollDist -= parent.dyTouch / divideBy
        } else {
            scrollDist -= (parent.yVelocityTouch / divideBy) * elapsed.toFloat()
        }
    }

    fun overscrollEffect(top: Boolean, elapsed: Double) {
        if (top)
            scrollDist = todoOverscrollDist * overscrollInterpolator.getMulti(overscrollElapsed, overscrollDuration).toFloat()
        else
            scrollDist = gridOverscrollHeight + (todoOverscrollDist * overscrollInterpolator.getMulti(overscrollElapsed, overscrollDuration).toFloat())

        if (overscrollElapsed <= 0) {
            todoOverscrollDist = 0.0f
            if (top)
                scrollDist = 0.0f
            else
                scrollDist = gridOverscrollHeight
            parent.yVelocityTouch = 0.0f
        } else {
            overscrollElapsed -= elapsed
        }
    }

    fun calculateTile(tile: Tile, zoom: Float) {
        val yPos = (glGrid[2] + topMargin + tile.posY.toFloat() * tileAndMarginCache + scrollDist + statusBarHeight) * zoom
        val xPos = tileXPosChache[tile.posX] * zoom
        val xSize = (tileSizeCache + (tile.spanX - 1).toFloat() * tileAndMarginCache) * zoom
        val ySize = (tileSizeCache + (tile.spanY - 1).toFloat() * tileAndMarginCache) * zoom

        val zPos = 1 - 1 / zoom
        if (zoom < 0)
            zPos -(1 - zoom)

        val verts = floatArrayOf(
                xPos        , yPos        , zPos,
                xPos + xSize, yPos        , zPos,
                xPos + xSize, yPos + ySize, zPos,
                xPos        , yPos + ySize, zPos
        )

        tile.colorBuffer = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)

        val bb = ByteBuffer.allocateDirect(verts.size * 4)
        bb.order(ByteOrder.nativeOrder())
        tile.vertBuffer = bb.asFloatBuffer()
        tile.vertBuffer!!.put(verts)
        tile.vertBuffer!!.position(0)

        val dlb = ByteBuffer.allocateDirect(inds.size * 2)
        dlb.order(ByteOrder.nativeOrder())
        tile.drawListBuffer = dlb.asShortBuffer()
        tile.drawListBuffer!!.put(inds)
        tile.drawListBuffer!!.position(0)

        /*val col = ByteBuffer.allocateDirect(color.size * 4)
        bb.order(ByteOrder.nativeOrder())
        tile.colorBuffer = col.asFloatBuffer()
        tile.colorBuffer!!.put(color)
        tile.colorBuffer!!.position(0)*/
    }

    fun cacheTileValues() {
        tileSizeCache = ((glGrid[1] - glGrid[0]) - leftSideMargin - rightSideMargin - (gridWidth - 1) * tilesMargin) / gridWidth

        tileAndMarginCache = tileSizeCache + tilesMargin

        tileXPosChache = FloatArray(gridWidth)

        var xPos = glGrid[0] + rightSideMargin
        for (i in 0..(gridWidth - 1)) {
            tileXPosChache[i] = xPos

            xPos += tileAndMarginCache
        }

        statusBarHeight = (glGrid[3] - glGrid[2]) * statusBarHeightPercentage
        navBarHeight = (glGrid[3] - glGrid[2]) * navBarHeightPercentage

        gridHeight = gridHeightSpan.toFloat() * tileAndMarginCache
    }
}