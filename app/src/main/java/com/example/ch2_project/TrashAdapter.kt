package com.example.ch2_project

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class TrashScheduleItem(
    val scheduleId: Int,
    val title: String,
    val deletedAt: String,
    val tasks: List<TaskItem>
)

class TrashAdapter(
    private var trashList: MutableList<TrashScheduleItem>,
    private val onRestoreClick: (Int) -> Unit,
    private val onCheckedChanged: () -> Unit
) : RecyclerView.Adapter<TrashAdapter.TrashViewHolder>() {

    private val checkedSet = mutableSetOf<Int>()

    fun getCheckedIds(): List<Int> {
        return checkedSet.toList()
    }

    fun getCheckedCount(): Int {
        return checkedSet.size
    }

    fun getTotalCount(): Int {
        return trashList.size
    }

    fun checkAll() {
        checkedSet.clear()

        for (item in trashList) {
            checkedSet.add(item.scheduleId)
        }

        notifyDataSetChanged()
        onCheckedChanged()
    }

    fun uncheckAll() {
        checkedSet.clear()
        notifyDataSetChanged()
        onCheckedChanged()
    }

    inner class TrashViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootLayout: LinearLayout = itemView.findViewById(R.id.todoRootLayout)
        val titleText: TextView = itemView.findViewById(R.id.scheduleTitleText)
        val dateText: TextView = itemView.findViewById(R.id.scheduleDateText)
        val tasksContainer: LinearLayout = itemView.findViewById(R.id.tasksContainer)
        val restoreButton: TextView = itemView.findViewById(R.id.scheduleRestoreButton)
        val itemCheckBox: CheckBox = itemView.findViewById(R.id.itemCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trash_todo, parent, false)

        return TrashViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrashViewHolder, position: Int) {
        val schedule = trashList[position]

        when (position % 4) {
            0 -> {
                holder.rootLayout.setBackgroundResource(R.drawable.todo_pink)
            }
            1 -> {
                holder.rootLayout.setBackgroundResource(R.drawable.todo_blue)
            }
            2 -> {
                holder.rootLayout.setBackgroundResource(R.drawable.todo_yellow)
            }
            else -> {
                holder.rootLayout.setBackgroundResource(R.drawable.todo_green)
            }
        }

        holder.titleText.text = schedule.title
        holder.dateText.text = "삭제일: ${formatDeletedDate(schedule.deletedAt)}"

        holder.itemCheckBox.setOnCheckedChangeListener(null)
        holder.itemCheckBox.isChecked = checkedSet.contains(schedule.scheduleId)

        holder.itemCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkedSet.add(schedule.scheduleId)
            } else {
                checkedSet.remove(schedule.scheduleId)
            }

            onCheckedChanged()
        }

        holder.tasksContainer.removeAllViews()

        val inflater = LayoutInflater.from(holder.itemView.context)

        for (task in schedule.tasks) {
            val taskView = inflater.inflate(R.layout.item_task_row, holder.tasksContainer, false)

            val checkBox = taskView.findViewById<CheckBox>(R.id.taskCheckBox)
            val contentText = taskView.findViewById<TextView>(R.id.taskContentText)

            contentText.text = task.content

            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = task.isCompleted
            checkBox.isEnabled = false

            if (task.isCompleted) {
                contentText.paintFlags = contentText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                contentText.setTextColor(0xFF999999.toInt())
            } else {
                contentText.paintFlags = contentText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                contentText.setTextColor(0xFF000000.toInt())
            }

            holder.tasksContainer.addView(taskView)
        }

        holder.restoreButton.setOnClickListener {
            onRestoreClick(schedule.scheduleId)
        }
    }

    override fun getItemCount(): Int {
        return trashList.size
    }

    fun updateData(newList: List<TrashScheduleItem>) {
        trashList.clear()
        trashList.addAll(newList)

        checkedSet.clear()

        notifyDataSetChanged()
        onCheckedChanged()
    }

    private fun formatDeletedDate(deletedAt: String): String {
        if (deletedAt.isBlank()) return "-"

        return deletedAt.split(" ")[0]
    }
}