/*
MIT License

Copyright (c) 2017 Mahdi Habib AlKhalaf

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.mahdialkhalaf.android.recyclerviewactionmodehelper

import android.support.annotation.MenuRes
import android.support.v7.widget.RecyclerView
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import java.util.*
import kotlin.collections.HashSet

/**
 * A helper class for easily creating and handling a contextual action mode when using a RecyclerView.
 * Make sure to call the onClick() and onLongClick() methods in this class in your own onClick() and
 * onLongClick() callbacks for item views in your RecyclerView. Also make sure to define a state list
 * drawable for changing the background of the selected item views.
 *
 * @param recyclerView the RecyclerView that holds the items that will be selected for actions.
 * @param actionMenu the menu that will be shown during action mode.
 */
abstract class RecyclerViewActionModeHelper(private val recyclerView: RecyclerView, @MenuRes actionMenu: Int) {
    private var actionMode: ActionMode? = null
    private val actionCallback: ActionMode.Callback
    private val selectedItems = mutableSetOf<Int>()

    init {
        actionCallback = object : ActionMode.Callback {
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                if (mode == null) throw AssertionError()
                val result = onActionItemClicked(item, Collections.unmodifiableSet(selectedItems))
                if (result == true) {
                    selectedItems.clear()
                    mode.finish()
                }
                return result
            }

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                if (mode == null) throw AssertionError()
                actionMode = mode
                mode.menuInflater?.inflate(actionMenu, mode.menu)

                onStartActionMode()

                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                if (mode == null) throw AssertionError()

                mode.title = onUpdateActionModeTitle(Collections.unmodifiableSet(selectedItems))

                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                if (mode == null) throw AssertionError()
                for (position in HashSet(selectedItems)) { // making a copy of selectedItems is necessary to prevent a ConcurrentModificationException
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) ?: throw AssertionError()
                    removeFromSelected(viewHolder.itemView)
                }
                assert(selectedItems.size == 0)

                onEndActionMode()

                actionMode = null
            }
        }
    }

    /**
     * Called when a user clicks an item in the action menu.
     *
     * @param item the menu item that was clicked.
     * @param selectedPositions the selected item positions in the RecyclerView
     * @return true if this callback handled the event, false if the standard MenuItem invocation should continue.
     */
    abstract fun onActionItemClicked(item: MenuItem?, selectedPositions: Set<Int>): Boolean

    /**
     * Called when the action mode is created
     */
    open fun onStartActionMode() {

    }

    /**
     * Called when the action mode is destroyed
     */
    open fun onEndActionMode() {

    }

    /**
     * Called when the action mode is updated. Can be used to set a custom title on the action bar
     * of the action mode. The default implementation sets the title to the number of items selected.
     *
     * @param selectedPositions the currently selected item positions in the RecyclerView.
     * @return the new title.
     */
    open fun onUpdateActionModeTitle(selectedPositions: Set<Int>): String {
        return "${selectedPositions.size} items selected"
    }

    /**
     * The click callback that should be delegated to for each itemView in the RecyclerView in
     * order to handle item selections when the action mode is activated. TL;DR call this method in your
     * own onClick() callback.
     *
     * @param view the itemView in the RecyclerView that was clicked
     * @see View.OnClickListener
     */
    fun onClick(view: View?) {
        if (actionMode != null) {
            if (!isItemViewSelected(view!!)) {
                addToSelected(view)
            } else {
                removeFromSelected(view)
            }
        }
    }

    /**
     * The long click callback that should be delegated to for each itemView in the RecyclerView in
     * order to start the action mode and handle item selections. TL;DR call this method in your
     * own onLongClick() callback.
     *
     * @param view the itemView in the RecyclerView that was clicked
     * @return always returns true to signal that the event was consumed.
     * @see View.OnLongClickListener
     */
    fun onLongClick(view: View?): Boolean {
        if (actionMode != null) {
            return true
        }

        view?.startActionMode(actionCallback)
        if (!isItemViewSelected(view!!)) {
            addToSelected(view)
        } else {
            removeFromSelected(view)
        }
        return true
    }

    private fun addToSelected(view: View) {
        view.isSelected = true
        val position = recyclerView.getChildAdapterPosition(view)
        selectedItems.add(position)
        actionMode?.invalidate()
    }

    private fun removeFromSelected(view: View) {
        view.isSelected = false
        val position = recyclerView.getChildAdapterPosition(view)
        selectedItems.remove(position)
        actionMode?.invalidate()
    }

    private fun isItemViewSelected(view: View): Boolean {
        val position = recyclerView.getChildAdapterPosition(view)
        return selectedItems.contains(position)
    }
}