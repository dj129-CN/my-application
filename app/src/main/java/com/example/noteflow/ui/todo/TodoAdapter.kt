package com.example.noteflow.ui.todo

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.noteflow.R
import com.example.noteflow.databinding.ItemTodoBinding

class TodoAdapter(
    private val onItemClick: (TodoDemoItem) -> Unit,
    private val onCheckedChange: (TodoDemoItem, Boolean) -> Unit,
) : ListAdapter<TodoDemoItem, TodoAdapter.TodoViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TodoViewHolder(
        private val binding: ItemTodoBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TodoDemoItem) {
            binding.todoTitle.text = item.title
            binding.todoContent.text = item.content.ifBlank {
                binding.root.context.getString(R.string.todo_item_no_content)
            }

            val strikeThrough = Paint.STRIKE_THRU_TEXT_FLAG
            binding.todoTitle.paintFlags = if (item.isCompleted) {
                binding.todoTitle.paintFlags or strikeThrough
            } else {
                binding.todoTitle.paintFlags and strikeThrough.inv()
            }
            binding.todoTitle.alpha = if (item.isCompleted) 0.55f else 1f
            binding.todoContent.alpha = if (item.isCompleted) 0.55f else 1f

            binding.todoCheckbox.setOnCheckedChangeListener(null)
            binding.todoCheckbox.isChecked = item.isCompleted
            binding.todoCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(item, isChecked)
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TodoDemoItem>() {
            override fun areItemsTheSame(oldItem: TodoDemoItem, newItem: TodoDemoItem): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TodoDemoItem, newItem: TodoDemoItem): Boolean =
                oldItem == newItem
        }
    }
}
