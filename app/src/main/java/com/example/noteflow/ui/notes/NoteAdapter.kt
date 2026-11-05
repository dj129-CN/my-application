package com.example.noteflow.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.noteflow.R
import com.example.noteflow.data.local.NoteEntity
import com.example.noteflow.databinding.ItemNoteBinding
import com.example.noteflow.util.DateFormats

class NoteAdapter(
    private val onNoteClick: (NoteEntity) -> Unit,
) : ListAdapter<NoteEntity, NoteAdapter.NoteViewHolder>(NoteDiffCallback) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position), onNoteClick)
    }

    class NoteViewHolder(
        private val binding: ItemNoteBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: NoteEntity, onNoteClick: (NoteEntity) -> Unit) {
            binding.noteTitle.text = note.title
            binding.notePreview.text = note.content.ifBlank {
                itemView.context.getString(R.string.note_no_content)
            }
            binding.noteTime.text = DateFormats.listLabel(note.updatedAt)
            binding.root.setOnClickListener { onNoteClick(note) }
        }
    }

    private object NoteDiffCallback : DiffUtil.ItemCallback<NoteEntity>() {
        override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity): Boolean =
            oldItem == newItem
    }
}
