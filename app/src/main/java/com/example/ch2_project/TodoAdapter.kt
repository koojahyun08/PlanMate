package com.example.ch2_project

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class TaskItem(
    val taskId: Int,
    val content: String,
    var isCompleted: Boolean
)

data class ScheduleItem(
    val scheduleId: Int,
    val targetDate: String,
    val title: String,
    val tasks: List<TaskItem>
)

class TodoAdapter(
    private var scheduleList: MutableList<ScheduleItem>,
    private val onCheckChanged: (Int, Boolean) -> Unit,
    private val onEditSchedule: (ScheduleItem) -> Unit,
    private val onDeleteSchedule: (Int) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootLayout: LinearLayout = itemView.findViewById(R.id.todoRootLayout)
        val colorBar: View = itemView.findViewById(R.id.todoColorBar)
        val titleText: TextView = itemView.findViewById(R.id.scheduleTitleText)
        val dateText: TextView = itemView.findViewById(R.id.scheduleDateText)
        val tasksContainer: LinearLayout = itemView.findViewById(R.id.tasksContainer)
        val editScheduleButton: TextView = itemView.findViewById(R.id.scheduleEditButton)
        val deleteScheduleButton: ImageView = itemView.findViewById(R.id.scheduleDeleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val schedule = scheduleList[position]

        when (position % 4) {
            0 -> { holder.rootLayout.setBackgroundResource(R.drawable.todo_pink); holder.colorBar.setBackgroundColor(Color.parseColor("#FF9999")) }
            1 -> { holder.rootLayout.setBackgroundResource(R.drawable.todo_blue); holder.colorBar.setBackgroundColor(Color.parseColor("#6699FF")) }
            2 -> { holder.rootLayout.setBackgroundResource(R.drawable.todo_yellow); holder.colorBar.setBackgroundColor(Color.parseColor("#FFCC66")) }
            3 -> { holder.rootLayout.setBackgroundResource(R.drawable.todo_green); holder.colorBar.setBackgroundColor(Color.parseColor("#66CC66")) }
        }

        holder.titleText.text = schedule.title
        holder.dateText.text = schedule.targetDate.replace("-", ".")

        holder.tasksContainer.removeAllViews()
        val inflater = LayoutInflater.from(holder.itemView.context)

        for (task in schedule.tasks) {
            val taskView = inflater.inflate(R.layout.item_task_row, holder.tasksContainer, false)
            val checkBox = taskView.findViewById<CheckBox>(R.id.taskCheckBox)
            val contentText = taskView.findViewById<TextView>(R.id.taskContentText)

            checkBox.setOnCheckedChangeListener(null)

            contentText.text = task.content
            checkBox.isChecked = task.isCompleted

            if (task.isCompleted) {
                contentText.paintFlags = contentText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                contentText.setTextColor(0xFF999999.toInt())
            } else {
                contentText.paintFlags = contentText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                contentText.setTextColor(0xFF000000.toInt())
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                task.isCompleted = isChecked
                onCheckChanged(task.taskId, isChecked)

                if (isChecked) {
                    contentText.paintFlags = contentText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    contentText.setTextColor(0xFF999999.toInt())
                } else {
                    contentText.paintFlags = contentText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    contentText.setTextColor(0xFF000000.toInt())
                }
            }

            holder.tasksContainer.addView(taskView)
        }

        holder.editScheduleButton.setOnClickListener {
            onEditSchedule(schedule)
        }

        holder.deleteScheduleButton.setOnClickListener {
            onDeleteSchedule(schedule.scheduleId)
        }
    }

    override fun getItemCount(): Int = scheduleList.size

    fun updateData(newSchedules: List<ScheduleItem>) {
        scheduleList.clear()
        scheduleList.addAll(newSchedules)
        notifyDataSetChanged()
    }

    fun getScheduleIds(): List<Int> {
        return scheduleList.map { it.scheduleId }
    }
}