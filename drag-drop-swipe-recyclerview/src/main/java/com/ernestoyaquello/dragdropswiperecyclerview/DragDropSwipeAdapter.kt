package com.ernestoyaquello.dragdropswiperecyclerview

import android.graphics.Canvas
import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.*
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemDragListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.ernestoyaquello.dragdropswiperecyclerview.util.DragDropSwipeTouchHelper
import com.ernestoyaquello.dragdropswiperecyclerview.util.drawHorizontalDividers
import com.ernestoyaquello.dragdropswiperecyclerview.util.drawVerticalDividers

/**
 * Needs to be implemented by any adapter to be used within a DragDropSwipeRecyclerView.
 *
 * @param T The type of the item.
 * @param U The type of the view holder.
 * @property dataSet The data set.
 */
abstract class DragDropSwipeAdapter<T, U : DragDropSwipeAdapter.ViewHolder>(
        dataSet: List<T> = emptyList()
): RecyclerView.Adapter<U>() {

    private var recyclerView: DragDropSwipeRecyclerView? = null

    private var mutableDataSet: MutableList<T> = dataSet.toMutableList()
    var dataSet: List<T>
        get() = mutableDataSet
        set(value) {
            mutableDataSet = value.toMutableList()
            notifyDataSetChanged()
        }

    private val orientation: DragDropSwipeRecyclerView.ListOrientation
        get() {
            val currentOrientation = recyclerView?.orientation
            if (currentOrientation != null)
                return currentOrientation
            else throw NullPointerException("The orientation of the DragDropSwipeRecyclerView is not defined.")
        }

    /**
     * The view holder abstract type whose implementation should be used in the implementation
     * of DragDropSwipeAdapter.
     *
     * @param layout The layout of the corresponding list item.
     */
    abstract class ViewHolder(layout: View) : RecyclerView.ViewHolder(layout) {
        internal var canBeDragged = true
        internal var canBeDroppedOver = true
        internal var canBeSwiped = true
        internal var isBeingDragged = false
        internal var isBeingSwiped = false
    }

    /**
     * Called automatically to get an instance of the adapter's view holder.
     *
     * @param itemView The item layout to be used to create the view holder.
     */
    protected abstract fun getViewHolder(itemView: View): U

    /**
     * Called automatically to get the data to display at the specified position. This method
     * should update the contents of the view holder's views to reflect the item at the given
     * position.
     *
     * @param item The item to bind, as read from the corresponding position of the data set.
     * @param viewHolder The view holder which should be updated to represent the contents of the
     *        item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    protected abstract fun onBindViewHolder(item: T, viewHolder: U, position: Int)

    /**
     * Called automatically to get the item view on which the user has to touch to drag the item.
     * If it returns null, the main view of the item will be used for dragging.
     *
     * @param item The item as read from the corresponding position of the data set.
     * @param viewHolder The corresponding view holder.
     * @param position The position of the item within the adapter's data set.
     * @return The item view on which the user has to touch to drag the item, or null if the view
     *         of the item that will be used for dragging is the main one.
     */
    protected abstract fun getViewToTouchToStartDraggingItem(item: T, viewHolder: U, position: Int): View?

    /**
     * Called automatically to know if the specified item can be dragged.
     *
     * @param item The item as read from the corresponding position of the data set.
     * @param viewHolder The corresponding view holder.
     * @param position The position of the item within the adapter's data set.
     * @return True if the item can be dragged; false otherwise.
     */
    protected open fun canBeDragged(item: T, viewHolder: U, position: Int) = true

    /**
     * Called automatically to know if the specified item accepts being exchanged by another one
     * being dropped over it.
     *
     * @param item The item as read from the corresponding position of the data set.
     * @param viewHolder The corresponding view holder.
     * @param position The position of the item within the adapter's data set.
     * @return True if the item accepts to be exchanged with another one being dragged over it;
     *         false otherwise.
     */
    protected open fun canBeDroppedOver(item: T, viewHolder: U, position: Int) = true

    /**
     * Called automatically to know if the specified item can be swiped.
     *
     * @param item The item as read from the corresponding position of the data set.
     * @param viewHolder The corresponding view holder.
     * @param position The position of the item within the adapter's data set.
     * @return True if the item can be swiped; false otherwise.
     */
    protected open fun canBeSwiped(item: T, viewHolder: U, position: Int) = true

    /**
     * Called when the dragging starts.
     *
     * @param item The item as read from the corresponding position of the data set.
     * @param viewHolder The view holder for which the dragging action has started.
     */
    protected open fun onDragStarted(item: T, viewHolder: U) {
    }

    /**
     * Called when the swiping starts.
     *
     * @param item The item as read from the corresponding position of the data set.
     * @param viewHolder The view holder for which the swiping action has started.
     */
    protected open fun onSwipeStarted(item: T, viewHolder: U) {
    }

    /**
     * Called when the dragging action (or animation) is occurring.
     *
     * @param item The item as read from the corresponding position of the data set.
     * @param viewHolder The view holder for which the dragging action is occurring.
     * @param offsetX The offset in the X axis caused by the horizontal movement of the item.
     *        This offset is the distance measured from the current position of the item, which
     *        may be a different position than the one the item initially had when the dragging
     *        action started (the position of an item can change while the item is being dragged).
     * @param offsetY The offset in the Y axis caused by the vertical movement of the item.
     *        This offset is the distance measured from the current position of the item, which
     *        may be a different position than the one the item initially had when the dragging
     *        action started (the position of an item can change while the item is being dragged).
     * @param canvasUnder A canvas positioned just in front of the recycler view and behind all its items.
     * @param canvasOver A canvas positioned just in front of the recycler view and all its items.
     * @param isUserControlled True if the item is still being controlled manually by the user;
     *        false if it is just being animated automatically by the system after the user has
     *        stopped touching it.
     */
    protected open fun onIsDragging(
            item: T,
            viewHolder: U,
            offsetX: Int,
            offsetY: Int,
            canvasUnder: Canvas?,
            canvasOver: Canvas?,
            isUserControlled: Boolean) {
    }

    /**
     * Called when the swiping action (or animation) is occurring.
     *
     * @param item The item as read from the corresponding position of the data set.
     *        It may be null if this method is being called because the item layout is still being
     *        animated by the system but the item itself has already been removed from the data set.
     * @param viewHolder The view holder for which the swiping action is occurring.
     * @param offsetX The offset in the X axis caused by the horizontal movement of the item.
     * @param offsetY The offset in the Y axis caused by the vertical movement of the item.
     * @param canvasUnder A canvas positioned just in front of the recycler view and behind all its items.
     * @param canvasOver A canvas positioned just in front of the recycler view and all its items.
     * @param isUserControlled True if the item is still being controlled manually by the user;
     *        false if it is just being animated automatically by the system (which is usually the
     *        case when the system is finishing the swiping animation in order to move the item to
     *        its final position right after the user has already stopped touching it).
     */
    protected open fun onIsSwiping(
            item: T?,
            viewHolder: U,
            offsetX: Int,
            offsetY: Int,
            canvasUnder: Canvas?,
            canvasOver: Canvas?,
            isUserControlled: Boolean) {
    }

    /**
     * Called when the dragging finishes (i.e., when the item is dropped).
     *
     * @param item The item as read from the corresponding position of the data set.
     * @param viewHolder The view holder for which the dragging action has finished.
     */
    protected open fun onDragFinished(item: T, viewHolder: U) {
    }

    /**
     * Called when the swiping animation executed by the system to complete the swiping has finished.
     * At the time this method gets called, the item has already been removed from the data set.
     *
     * @param viewHolder The view holder for which the swiping animation has finished.
     */
    protected open fun onSwipeAnimationFinished(viewHolder: U) {
    }

    private var itemTouchHelper : ItemTouchHelper

    private var dragListener: OnItemDragListener<T>? = null
    private var swipeListener: OnItemSwipeListener<T>? = null
    internal val swipeAndDragHelper: DragDropSwipeTouchHelper

    @Suppress("UNCHECKED_CAST")
    internal fun setInternalDragListener(listener: OnItemDragListener<*>?) {
        if (listener != null)
            dragListener = listener as? OnItemDragListener<T>
    }

    @Suppress("UNCHECKED_CAST")
    internal fun setInternalSwipeListener(listener: OnItemSwipeListener<*>?) {
        if (listener != null)
            swipeListener = listener as? OnItemSwipeListener<T>
    }

    private val itemDragListener = object : DragDropSwipeTouchHelper.OnItemDragListener {
        override fun onItemDragged(previousPosition: Int, newPosition: Int) {
            val item = mutableDataSet[previousPosition]
            onListItemDragged(previousPosition, newPosition)

            dragListener?.onItemDragged(previousPosition, newPosition, item)
        }

        override fun onItemDropped(initialPosition: Int, finalPosition: Int) {
            val item = mutableDataSet[finalPosition]

            dragListener?.onItemDropped(initialPosition, finalPosition, item)
        }
    }

    private val itemSwipeListener = object : DragDropSwipeTouchHelper.OnItemSwipeListener {
        override fun onItemSwiped(position: Int, direction: OnItemSwipeListener.SwipeDirection) {
            val item = mutableDataSet[position]
            onListItemSwiped(position)

            swipeListener?.onItemSwiped(position, direction, item)
        }
    }

    private val stateChangeListener = object : DragDropSwipeTouchHelper.OnItemStateChangeListener {
        @Suppress("UNCHECKED_CAST")
        override fun onStateChanged(
                newState: DragDropSwipeTouchHelper.OnItemStateChangeListener.StateChangeType,
                viewHolder: RecyclerView.ViewHolder) {

            val dragDropSwipeViewHolder = viewHolder as U
            when (newState) {
                DragDropSwipeTouchHelper.OnItemStateChangeListener.StateChangeType.DRAG_STARTED ->
                    onDragStartedImpl(dragDropSwipeViewHolder)
                DragDropSwipeTouchHelper.OnItemStateChangeListener.StateChangeType.DRAG_FINISHED ->
                    onDragFinishedImpl(dragDropSwipeViewHolder)
                DragDropSwipeTouchHelper.OnItemStateChangeListener.StateChangeType.SWIPE_STARTED ->
                    onSwipeStartedImpl(dragDropSwipeViewHolder)
                DragDropSwipeTouchHelper.OnItemStateChangeListener.StateChangeType.SWIPE_FINISHED ->
                    onSwipeFinishedImpl(dragDropSwipeViewHolder)
            }
        }
    }

    private val itemLayoutPositionListener = object : DragDropSwipeTouchHelper.OnItemLayoutPositionChangeListener {
        @Suppress("UNCHECKED_CAST")
        override fun onPositionChanged(
                action: DragDropSwipeTouchHelper.OnItemLayoutPositionChangeListener.Action,
                viewHolder: RecyclerView.ViewHolder,
                offsetX: Int,
                offsetY: Int,
                canvasUnder: Canvas?,
                canvasOver: Canvas?,
                isUserControlled: Boolean) {

            val dragDropSwipeViewHolder = viewHolder as U
            when (action) {
                DragDropSwipeTouchHelper.OnItemLayoutPositionChangeListener.Action.SWIPING ->
                    onIsSwipingImpl(dragDropSwipeViewHolder, offsetX, offsetY, canvasUnder, canvasOver, isUserControlled)
                DragDropSwipeTouchHelper.OnItemLayoutPositionChangeListener.Action.DRAGGING ->
                    onIsDraggingImpl(dragDropSwipeViewHolder, offsetX, offsetY, canvasUnder, canvasOver, isUserControlled)
            }
        }
    }

    init {
        swipeAndDragHelper = DragDropSwipeTouchHelper(
                itemDragListener,
                itemSwipeListener,
                stateChangeListener,
                itemLayoutPositionListener,
                recyclerView)
        itemTouchHelper = ItemTouchHelper(swipeAndDragHelper)
    }

    override fun getItemCount() = mutableDataSet.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): U {
        val itemLayoutId = recyclerView?.itemLayoutId ?: 0
        if (itemLayoutId != 0) {
            val itemLayout = LayoutInflater.from(parent.context)
                    .inflate(itemLayoutId, parent, false) as View
            return getViewHolder(itemLayout)
        } else throw NoSuchFieldException("Unless your adapter implements onCreateViewHolder(), " +
                "the attribute item_layout must be provided for the DragDropSwipeRecyclerView.")
    }

    override fun onBindViewHolder(holder: U, position: Int) {
        val item = mutableDataSet[position]

        onBindViewHolder(item, holder, position)

        holder.canBeDragged = canBeDragged(item, holder, position)
        holder.canBeDroppedOver = canBeDroppedOver(item, holder, position)
        holder.canBeSwiped = canBeSwiped(item, holder, position)

        if (holder.canBeDragged) {
            val viewToDrag = getViewToTouchToStartDraggingItem(item, holder, position) ?: holder.itemView
            setItemDragAndDrop(viewToDrag, holder)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        if (recyclerView is DragDropSwipeRecyclerView) {
            this.recyclerView = recyclerView
            itemTouchHelper.attachToRecyclerView(recyclerView)
            swipeAndDragHelper.recyclerView = recyclerView
        }
        else throw TypeCastException("The recycler view must be an extension of DragDropSwipeRecyclerView.")
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        if (recyclerView is DragDropSwipeRecyclerView) {
            this.recyclerView = null
            swipeAndDragHelper.recyclerView = null
        }
        else throw TypeCastException("The recycler view must be an extension of DragDropSwipeRecyclerView.")
    }

    fun addItem(item: T) {
        mutableDataSet.add(item)
        val position = mutableDataSet.indexOf(item)

        notifyItemInserted(position)
    }

    fun insertItem(position: Int, item: T) {
        mutableDataSet.add(position, item)

        notifyItemInserted(position)
    }

    fun removeItem(position: Int) {
        mutableDataSet.removeAt(position)

        notifyItemRemoved(position)
    }

    fun moveItem(previousPosition: Int, newPosition: Int) {
        val item = mutableDataSet[previousPosition]
        mutableDataSet.removeAt(previousPosition)
        mutableDataSet.add(newPosition, item)

        notifyItemMoved(previousPosition, newPosition)
    }

    fun moveItem(newPosition: Int, item: T) {
        val previousPosition = mutableDataSet.indexOf(item)
        if (previousPosition != -1)
            moveItem(previousPosition, newPosition)
        else
            insertItem(newPosition, item)
    }

    private fun onListItemDragged(previousPosition: Int, newPosition: Int) {
        moveItem(previousPosition, newPosition)
    }

    private fun onListItemSwiped(position: Int) {
        removeItem(position)
    }

    private fun onDragStartedImpl(viewHolder: U) {
        val item = dataSet[viewHolder.adapterPosition]

        viewHolder.isBeingDragged = true
        onDragStarted(item, viewHolder)
    }

    private fun onSwipeStartedImpl(viewHolder: U) {
        val item = dataSet[viewHolder.adapterPosition]

        viewHolder.isBeingSwiped = true
        onSwipeStarted(item, viewHolder)
    }

    private fun onDragFinishedImpl(viewHolder: U) {
        val item = dataSet[viewHolder.adapterPosition]

        viewHolder.isBeingDragged = false
        onDragFinished(item, viewHolder)
    }

    private fun onSwipeFinishedImpl(viewHolder: U) {
        viewHolder.isBeingSwiped = false
        onSwipeAnimationFinished(viewHolder)
    }

    private fun onIsSwipingImpl(
            viewHolder: U,
            offsetX: Int,
            offsetY: Int,
            canvasUnder: Canvas?,
            canvasOver: Canvas?,
            isUserControlled: Boolean) {

        val item = if (viewHolder.adapterPosition != -1) dataSet[viewHolder.adapterPosition] else null

        drawOnSwiping(offsetX, offsetY, viewHolder, canvasUnder, canvasOver)
        onIsSwiping(item, viewHolder, offsetX, offsetY, canvasUnder, canvasOver, isUserControlled)
    }

    private fun onIsDraggingImpl(
            viewHolder: U,
            offsetX: Int,
            offsetY: Int,
            canvasUnder: Canvas?,
            canvasOver: Canvas?,
            isUserControlled: Boolean) {

        val item = dataSet[viewHolder.adapterPosition]

        drawOnDragging(canvasOver, viewHolder)
        onIsDragging(item, viewHolder, offsetX, offsetY, canvasUnder, canvasOver, isUserControlled)
    }

    private fun drawOnSwiping(
            offsetX: Int,
            offsetY: Int,
            viewHolder: U,
            canvasUnder: Canvas?,
            canvasOver: Canvas?) {
        val list = recyclerView
        if (list != null) {

            // TODO Make this check in a less ugly way (Kotlin probably has something for this kind of stuff)
            val isSwipingHorizontally = (orientation.swipeFlags and ItemTouchHelper.RIGHT == ItemTouchHelper.RIGHT)
                    || (orientation.swipeFlags and ItemTouchHelper.LEFT == ItemTouchHelper.LEFT)
            val isSecondaryDirection = (isSwipingHorizontally && offsetX > 0) || (!isSwipingHorizontally && offsetY < 0)

            // The current "coordinates" of the layout with the swipe translation applied to it
            val currentLayoutAreaLeft = viewHolder.itemView.left + viewHolder.itemView.translationX.toInt()
            val currentLayoutAreaTop = viewHolder.itemView.top + viewHolder.itemView.translationY.toInt()
            val currentLayoutAreaRight = viewHolder.itemView.right + viewHolder.itemView.translationX.toInt()
            val currentLayoutAreaBottom = viewHolder.itemView.bottom + viewHolder.itemView.translationY.toInt()

            // The original "coordinates" of the layout from which it is being moved away
            val originalLayoutAreaLeft = if (isSwipingHorizontally) viewHolder.itemView.left else currentLayoutAreaLeft
            val originalLayoutAreaTop = if (!isSwipingHorizontally) viewHolder.itemView.top else currentLayoutAreaTop
            val originalLayoutAreaRight = if (isSwipingHorizontally) viewHolder.itemView.right else currentLayoutAreaRight
            val originalLayoutAreaBottom = if (!isSwipingHorizontally) viewHolder.itemView.bottom else currentLayoutAreaBottom

            // If requested, make the item less opaque as it moves away from it original position
            var newItemAlpha = 1f
            if (list.reduceItemAlphaOnSwiping) {
                val offsetToItemSizeRatio =
                        if (isSwipingHorizontally)
                            Math.abs(offsetX).toFloat() / (originalLayoutAreaRight - originalLayoutAreaLeft).toFloat()
                        else
                            Math.abs(offsetY).toFloat() / (originalLayoutAreaBottom - originalLayoutAreaTop).toFloat()
                newItemAlpha = 1.1f - offsetToItemSizeRatio
                newItemAlpha = if (newItemAlpha < 0.1f) 0.1f else newItemAlpha
                newItemAlpha = if (newItemAlpha > 1f) 1f else newItemAlpha
                viewHolder.itemView.alpha = newItemAlpha
            }

            if (canvasUnder != null)
                drawLayoutBehindOnSwiping(
                        list,
                        canvasUnder,
                        originalLayoutAreaLeft,
                        originalLayoutAreaTop,
                        originalLayoutAreaRight,
                        originalLayoutAreaBottom,
                        isSwipingHorizontally,
                        isSecondaryDirection)

            else if (canvasOver != null) {
                drawDividersOnSwiping(
                        list,
                        canvasOver,
                        viewHolder,
                        currentLayoutAreaLeft,
                        currentLayoutAreaTop,
                        currentLayoutAreaRight,
                        currentLayoutAreaBottom,
                        newItemAlpha,
                        originalLayoutAreaLeft,
                        originalLayoutAreaTop,
                        originalLayoutAreaRight,
                        originalLayoutAreaBottom)
            }
        }
    }

    private fun drawOnDragging(canvasOver: Canvas?, viewHolder: U) {
        val list = recyclerView
        if (canvasOver != null && list != null)
            drawDividers(list, canvasOver, viewHolder)
    }

    private fun drawLayoutBehindOnSwiping(
            list: DragDropSwipeRecyclerView,
            canvasUnder: Canvas,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            isSwipingHorizontally: Boolean,
            isSecondarySwipeDirection: Boolean) {

        canvasUnder.save()

        // Apply a clip rectangle with the calculated area to draw inside it
        canvasUnder.clipRect(left, top, right, bottom)

        // Get the custom layout to draw behind the swiped item, if any
        val behindLayout =
                if (isSecondarySwipeDirection && list.behindSwipedItemSecondaryLayout != null)
                    list.behindSwipedItemSecondaryLayout
                else
                    list.behindSwipedItemLayout
        if (behindLayout != null) {

            val behindLayoutWidth = right - left
            val behindLayoutHeight = bottom - top

            // Draw the custom layout behind the item
            if (behindLayout.measuredWidth != behindLayoutWidth || behindLayout.measuredHeight != behindLayoutHeight)
                behindLayout.measure(
                        View.MeasureSpec.makeMeasureSpec(behindLayoutWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(behindLayoutHeight, View.MeasureSpec.EXACTLY))
            behindLayout.layout(left, top, right, bottom)
            canvasUnder.save()
            canvasUnder.translate(left.toFloat(), top.toFloat())
            behindLayout.draw(canvasUnder)

        } else {

            // Since there was no custom layout, look for the background color and draw it if necessary
            val backgroundColor =
                    if (isSecondarySwipeDirection
                            && list.behindSwipedItemBackgroundSecondaryColor != null
                            && list.behindSwipedItemBackgroundSecondaryColor != Color.TRANSPARENT)
                        list.behindSwipedItemBackgroundSecondaryColor
                    else
                        list.behindSwipedItemBackgroundColor
            if (backgroundColor != null && backgroundColor != Color.TRANSPARENT)
                canvasUnder.drawColor(backgroundColor)

            // Draw icon if necessary
            val iconBehindSwipedItem =
                    if (isSecondarySwipeDirection && list.behindSwipedItemIconSecondaryDrawable != null)
                        list.behindSwipedItemIconSecondaryDrawable
                    else
                        list.behindSwipedItemIconDrawable
            if (iconBehindSwipedItem != null) {

                // Calculate the icon position to be centered
                val iconWidth = iconBehindSwipedItem.intrinsicWidth
                val iconHeight = iconBehindSwipedItem.intrinsicHeight
                var iconCenterX = left + ((right - left) / 2)
                var iconCenterY = top + ((bottom - top) / 2)
                val halfIconWidth = iconWidth / 2
                val halfIconHeight = iconHeight / 2
                if (!list.behindSwipedItemCenterIcon) {

                    // Update the icon position to be near the side from which the swiping started
                    val margin = list.behindSwipedItemIconMargin.toInt()

                    if (isSwipingHorizontally && isSecondarySwipeDirection)
                        iconCenterX = left + margin + halfIconWidth
                    else if (isSwipingHorizontally && !isSecondarySwipeDirection)
                        iconCenterX = right - margin - halfIconWidth
                    else if (!isSwipingHorizontally && isSecondarySwipeDirection)
                        iconCenterY = bottom - margin - halfIconHeight
                    else if (!isSwipingHorizontally && !isSecondarySwipeDirection)
                        iconCenterY = top + margin + halfIconHeight
                }
                val iconLeft = iconCenterX - halfIconWidth
                val iconRight = iconLeft + iconWidth
                val iconTop = iconCenterY - halfIconHeight
                val iconBottom = iconTop + iconHeight
                iconBehindSwipedItem.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                // Finally, we draw the icon in the canvas
                iconBehindSwipedItem.draw(canvasUnder)
            }
        }

        canvasUnder.restore()
    }

    private fun drawDividersOnSwiping(
            list: DragDropSwipeRecyclerView,
            canvasOver: Canvas,
            viewHolder: U,
            currentLayoutAreaLeft: Int,
            currentLayoutAreaTop: Int,
            currentLayoutAreaRight: Int,
            currentLayoutAreaBottom: Int,
            newItemAlpha: Float,
            originalLayoutAreaLeft: Int,
            originalLayoutAreaTop: Int,
            originalLayoutAreaRight: Int,
            originalLayoutAreaBottom: Int) {

        // Draw dividers around the swiped item layout
        drawDividers(
                list,
                canvasOver,
                viewHolder,
                currentLayoutAreaLeft,
                currentLayoutAreaTop,
                currentLayoutAreaRight,
                currentLayoutAreaBottom,
                newItemAlpha)

        // Draw dividers around the area behind - except with grids (it looks odd with grids)
        if (orientation != DragDropSwipeRecyclerView.ListOrientation.GRID_LIST_WITH_HORIZONTAL_SWIPING
            && orientation != DragDropSwipeRecyclerView.ListOrientation.GRID_LIST_WITH_VERTICAL_SWIPING)
            drawDividers(
                    list,
                    canvasOver,
                    viewHolder,
                    originalLayoutAreaLeft,
                    originalLayoutAreaTop,
                    originalLayoutAreaRight,
                    originalLayoutAreaBottom)
    }

    private fun drawDividers(
            list: DragDropSwipeRecyclerView,
            canvasOver: Canvas,
            viewHolder: U,
            left: Int? = null,
            top: Int? = null,
            right: Int? = null,
            bottom: Int? = null,
            alpha: Float? = null) {

        val dividerDrawable = list.dividerDrawable
        if (dividerDrawable != null) {
            when (orientation) {
                DragDropSwipeRecyclerView.ListOrientation.VERTICAL_LIST_WITH_VERTICAL_DRAGGING,
                DragDropSwipeRecyclerView.ListOrientation.VERTICAL_LIST_WITH_UNCONSTRAINED_DRAGGING ->
                    drawHorizontalDividers(viewHolder.itemView, list, canvasOver, dividerDrawable, left, right, alpha = alpha)

                DragDropSwipeRecyclerView.ListOrientation.HORIZONTAL_LIST_WITH_UNCONSTRAINED_DRAGGING,
                DragDropSwipeRecyclerView.ListOrientation.HORIZONTAL_LIST_WITH_HORIZONTAL_DRAGGING ->
                    drawVerticalDividers(viewHolder.itemView, list, canvasOver, dividerDrawable, top, bottom, alpha = alpha)

                DragDropSwipeRecyclerView.ListOrientation.GRID_LIST_WITH_HORIZONTAL_SWIPING,
                DragDropSwipeRecyclerView.ListOrientation.GRID_LIST_WITH_VERTICAL_SWIPING -> {
                    drawHorizontalDividers(viewHolder.itemView, list, canvasOver, dividerDrawable, left, right, alpha = alpha)
                    drawVerticalDividers(viewHolder.itemView, list, canvasOver, dividerDrawable, top, bottom, alpha = alpha)
                }
            }
        }
    }

    private fun setItemDragAndDrop(view: View, holder: U) =
            view.setOnTouchListener(getDragAndDropTouchListener(holder))

    private fun getDragAndDropTouchListener(holder: U) = View.OnTouchListener {
        _,
        event ->
        if (holder.canBeDragged && event?.actionMasked == MotionEvent.ACTION_DOWN)
            itemTouchHelper.startDrag(holder)

        false
    }
}