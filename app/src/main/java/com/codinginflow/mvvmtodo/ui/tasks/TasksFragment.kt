package com.codinginflow.mvvmtodo.ui.tasks

import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codinginflow.mvvmtodo.R
import com.codinginflow.mvvmtodo.data.SortOrder
import com.codinginflow.mvvmtodo.data.Task
import com.codinginflow.mvvmtodo.databinding.FragmentTasksBinding
import com.codinginflow.mvvmtodo.util.exhaustive
import com.codinginflow.mvvmtodo.util.onQueryTextChanged
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TasksFragment : Fragment(R.layout.fragment_tasks), TasksAdapter.onItemClickListener{

    private val viewModel: TasksViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentTasksBinding.bind(view)

        val taskAdapter = TasksAdapter(this)

        binding.apply {
            recyclerViewTasks.apply {
                adapter = taskAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
            }

            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {

                val icon : Drawable? = context?.let { ContextCompat.getDrawable(it, R.drawable.ic_delete) }
                val background = ColorDrawable(Color.RED)

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean  = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val task = taskAdapter.currentList[viewHolder.adapterPosition]
                    viewModel.onTaskSwiped(task)
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )

                    val itemView = viewHolder.itemView
                    val backgroundCornerOffset = 20

                    val iconMargin = (itemView.height - icon!!.intrinsicHeight!!)/2
                    val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight)/2
                    val iconBottom = iconTop + icon.intrinsicHeight

                    when {
                        dX > 0 -> {
                            val iconLeft = itemView.left + iconMargin
                            val iconRight = itemView.left + iconMargin + icon.intrinsicWidth

                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                            background.setBounds(itemView.left, itemView.top,
                                itemView.left + (dX.toInt() + backgroundCornerOffset), itemView.bottom)


                        }
                        dX < 0 -> {
                            val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                            val iconRight = itemView.right - iconMargin
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

                            background.setBounds(itemView.right + dX.toInt() - backgroundCornerOffset,
                                itemView.top, itemView.right, itemView.bottom
                            )


                        }
                        else -> {
                            background.setBounds(0,0,0,0)
                        }
                    }

                    background.draw(c)
                    icon.draw(c)

                }

                override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float =
                    0.7f

            }).attachToRecyclerView(recyclerViewTasks)


            fabAddTask.setOnClickListener {
                viewModel.onAddNewTaskClick()
            }
        }


        setFragmentResultListener("add_edit_request"){
            _, bundle ->
            val result = bundle.getInt("add_edit_result")
            viewModel.onAddEditResult(result)
        }

        viewModel.tasks.observe(viewLifecycleOwner) {
            taskAdapter.submitList(it)
        }

        //why use launchWhenCreated? : As soon as we put the Fragment into background, we don't need to listen to any event
        //because we don't want to show a Snackbar while our Fragment is invisible, instead the channel can suspend(wait)
        //when we put the Fragment into the foreground, the Coroutine will start collecting flow
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.tasksEvent.collect {
                event -> when(event){
                    is TasksViewModel.TasksEvent.ShowUndoDeleteTaskMessage -> {
                        Snackbar.make(requireView(), "删除任务", Snackbar.LENGTH_LONG)
                            .setAction("撤回"){
                                viewModel.onUndoDeleteClick(event.task)
                            }.show()
                    }
                    is TasksViewModel.TasksEvent.NavigateToEditTaskScreen -> {
                    val action = TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment(event.task, "编辑事项")
                    findNavController().navigate(action)
                }
                    is TasksViewModel.TasksEvent.NavigateToAddTaskScreen -> {
                    val action = TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment(null, "添加新事项")
                    findNavController().navigate(action)
                }

                    is TasksViewModel.TasksEvent.ShowTaskSavedConfirmationMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
            }.exhaustive
            }
        }

        setHasOptionsMenu(true)
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_tasks, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.onQueryTextChanged {
            viewModel.searchQuery.value = it
        }

        //set the hideCompleted state from flow when the menu is first created
        //scenario: save the hideCompleted state when we exit the app then go back again, it shows the same state from the last exit
        viewLifecycleOwner.lifecycleScope.launch {
            menu.findItem(R.id.action_hide_completed_tasks).isChecked =
                viewModel.preferenceFlow.first().hideCompleted
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_sort_by_name -> {
                viewModel.onSortOrderSelected(SortOrder.BY_NAME)
                true
            }
            R.id.action_sort_by_date_created -> {
                viewModel.onSortOrderSelected(SortOrder.BY_DATE)
                true
            }
            R.id.action_hide_completed_tasks -> {
                item.isChecked = !item.isChecked
                viewModel.onHideCompletedClicked(item.isChecked)
                true
            }

            R.id.action_delete_all_completed_tasks -> {

                true
            }
            else -> super.onOptionsItemSelected(item)

        }
    }

    override fun onItemClick(task: Task) {
        viewModel.onTaskSelected(task)

    }

    override fun onCheckBoxClick(task: Task, isChecked: Boolean) {
        viewModel.onTaskCheckedChanged(task, isChecked)
    }




}